/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;

import com.google.common.collect.Lists;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.types.check.SymTypeExpression;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.domain.decorator.QualifierKey;
import umlp.generator.util.PathUtil;
import umlp.generator.util.UMLPSymbolTables;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.commands.CommandDecorator.DE_SER;

/**
 * Generates the command {@code <Owner>_put<Role>} of the GemMap of an attribute qualified association (instead of
 * {@code _add}, maps have no positions): puts a value under its key. The server derives the key from the value (a
 * key that does not match is replaced, warning 0xA7306/0xA7316), clients store the key the server sent without
 * loading the value.
 */
public class PutDecorator extends AddDecorator {

  public static final String PUT_SUFFIX = "_put";

  public PutDecorator(GlobalExtensionManagement glex, Set<String> supportedDeSers) {
    super(glex, supportedDeSers);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    addedDeSers.clear();
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, COMMAND_PACKAGE);
    getNonDerivedDomainClasses(origCD).forEach(c -> decorateMapRoles(origCD, astcdPackage, c));
    getNonDerivedDomainInterfaces(origCD).forEach(i -> decorateMapRoles(origCD, astcdPackage, i));
  }

  protected void decorateMapRoles(ASTCDCompilationUnit origCD, ASTCDPackage astcdPackage, ASTCDType domainType) {
    CDSymbolTables.getAssociations(domainType).stream()
        .filter(Predicate.not(GenService::isSymbolDerived))
        .filter(GenService::isMap)
        .forEach(a -> {
          ASTCDClass clazz = createCommand(origCD, a, domainType);
          astcdPackage.addCDElement(withOrigin(clazz));
          addManagerImportForAssoc(origCD, clazz, a);
          astcdPackage.addCDElement(withOrigin(createCommandDeSer(origCD, a, domainType)));
        });
  }

  protected String getSuffix() {
    return PUT_SUFFIX;
  }

  /**
   * @return the key fields of the command, all of the stored key type
   */
  protected List<String> getKeyFields() {
    return List.of("key");
  }

  protected String getCommandName(ASTCDType domainType, ASTCDAssocSide a) {
    return domainType.getName() + getSuffix() + StringTransformations.capitalize(a.getName());
  }

  protected ASTCDClass createCommand(ASTCDCompilationUnit origCD, ASTCDAssocSide a, ASTCDType domainType) {
    QualifierKey key = QualifierKey.of(a);
    String classname = getCommandName(domainType, a);
    String valueIdName = a.getName() + "GemId";
    SymTypeExpression targetType = a.getSymbol().getType();
    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setName(classname)
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(extendedClass()))
        .build();

    AccessorDecorator accessor = new AccessorDecorator(glex);
    List<ASTCDAttribute> fields = Lists.newArrayList(
        getCD4C().addAttribute(ast, "protected long gemId"),
        getCD4C().addAttribute(ast, "protected long " + valueIdName));
    for (String keyField : getKeyFields()) {
      fields.add(getCD4C().addAttribute(ast, "protected " + key.getStoredKeyType() + " " + keyField));
    }
    fields.forEach(f -> ast.addAllCDMembers(accessor.decorate(f)));

    getCD4C().addConstructor(ast, "umlp.commands.put.Constructor", classname,
        getPackageName(origCD, COMMAND_PACKAGE), valueIdName, key.getStoredKeyType(), getKeyFields());
    addDoAction(ast, origCD, a, domainType, key);
    addCheckAccessPolicy(ast, domainType);
    // the owner only: maps are unidirectional, only the participants that loaded the owner have the map. The value
    // must not be included: sending the command subscribes the receivers to the effected objects, so they would
    // receive (and load) the value with its next setter command.
    getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(), Lists.newArrayList("gemId"));

    addImports(origCD, domainType, ast);
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandManager"));
    getCD4C().addImport(ast, "de.se_rwth.commons.logging.Log");
    addCorrectMangerImportForTargetType(origCD.getCDDefinition(), ast, targetType);
    addTypeImport(origCD, ast, domainType,
        UMLPSymbolTables.getDiagramFor((ASTCDType) targetType.getTypeInfo().getAstNode()), targetType);
    return ast;
  }

  protected void addDoAction(ASTCDClass ast, ASTCDCompilationUnit origCD, ASTCDAssocSide a, ASTCDType domainType,
                             QualifierKey key) {
    String targetType = a.getSymbol().getType().print();
    getCD4C().addMethod(ast, "umlp.commands.put.DoRun", domainType.getName(), getCorrectManager(domainType),
        a.getName(), targetType, getCorrectManagerOfTargetType(a), key.isEntryKey(), key.getStoredKeyType(),
        key.isEntryKey() ? "0xA7316" : "0xA7306");
  }

  protected void addCheckAccessPolicy(ASTCDClass ast, ASTCDType domainType) {
    getCD4C().addMethod(ast, getGeneralCheckAccessPolicyTemplate(), domainType.getName(), "Create");
  }

  protected ASTCDClass createCommandDeSer(ASTCDCompilationUnit origCD, ASTCDAssocSide a, ASTCDType domainType) {
    String cmdName = getCommandName(domainType, a);
    String valueIdName = a.getName() + "GemId";
    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("CommandDeSer<" + cmdName + ">"))
        .setName(cmdName + DE_SER)
        .build();
    getCD4C().addMethod(ast, "umlp.commands.deser.GemMapCommand_FromJsonObject", cmdName, valueIdName,
        getKeyFields(), QualifierKey.of(a).getStoredKeyJson());
    getCD4C().addMethod(ast, "umlp.commands.deser.GemMapCommand_BuildJson", cmdName, valueIdName, getKeyFields());
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + domainType.getName());
    addedDeSers.add(ast);
    return ast;
  }
}
