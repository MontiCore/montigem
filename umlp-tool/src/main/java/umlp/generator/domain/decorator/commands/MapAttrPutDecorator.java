/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;

import com.google.common.collect.Lists;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.types.check.SymTypeExpression;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.domain.decorator.BaseClassObjectMapperDecorator;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.util.PathUtil;
import umlp.generator.util.UMLPSymbolTables;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.domain.decorator.commands.CommandDecorator.DE_SER;

/**
 * Generates the command {@code <Class>_put<Attr>} of a map attribute with simple values
 * ({@code Map<String, Integer> scores}, see GenService#isMap), the counterpart of {@code _add<Attr>} of list
 * attributes: puts a value under a key. The entry is addressed by its key, like a list element by its index.
 */
public class MapAttrPutDecorator extends AbstractDeSerDecorator {

  public static final String PUT_SUFFIX = "_put";

  protected final Set<String> supportedDeSers;

  public MapAttrPutDecorator(GlobalExtensionManagement glex, Set<String> supportedDeSers) {
    super(glex);
    this.supportedDeSers = supportedDeSers;
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    addedDeSers.clear();
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, COMMAND_PACKAGE);
    getNonDerivedDomainClasses(origCD).forEach(c -> decorateMapAttributes(origCD, astcdPackage, c));
    getNonDerivedDomainInterfaces(origCD).forEach(i -> decorateMapAttributes(origCD, astcdPackage, i));
  }

  protected void decorateMapAttributes(ASTCDCompilationUnit origCD, ASTCDPackage astcdPackage, ASTCDType domainType) {
    CDSymbolTables.getAttributesInHierarchy(domainType).stream()
        .filter(a -> !a.getModifier().isDerived())
        .filter(GenService::isMap)
        .forEach(a -> {
          astcdPackage.addCDElement(withOrigin(createCommand(origCD, a, domainType)));
          astcdPackage.addCDElement(withOrigin(createCommandDeSer(origCD, a, domainType)));
        });
  }

  protected String getSuffix() {
    return PUT_SUFFIX;
  }

  /**
   * @return whether the command carries a value besides the key
   */
  protected boolean hasValue() {
    return true;
  }

  /**
   * @return the permission the command needs, like the add of list attributes
   */
  protected String getPermission() {
    return "Create";
  }

  protected String getCommandName(ASTCDType domainType, ASTCDAttribute a) {
    return domainType.getName() + getSuffix() + StringTransformations.capitalize(a.getName());
  }

  protected ASTCDClass createCommand(ASTCDCompilationUnit origCD, ASTCDAttribute a, ASTCDType domainType) {
    String classname = getCommandName(domainType, a);
    String keyType = GenService.getMapAttributeType(a, 0);
    String valueType = GenService.getMapAttributeType(a, 1);
    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setName(classname)
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("Command<Void>"))
        .build();

    AccessorDecorator accessor = new AccessorDecorator(glex);
    List<ASTCDAttribute> fields = Lists.newArrayList(
        getCD4C().addAttribute(ast, "protected long gemId"),
        getCD4C().addAttribute(ast, "protected " + keyType + " key"));
    if (hasValue()) {
      fields.add(getCD4C().addAttribute(ast, "protected " + valueType + " value"));
    }
    fields.forEach(f -> ast.addAllCDMembers(accessor.decorate(f)));

    getCD4C().addConstructor(ast, "umlp.commands.mapattrs.Constructor", classname,
        getPackageName(origCD, COMMAND_PACKAGE), keyType, valueType, hasValue());
    getCD4C().addMethod(ast, "umlp.commands.mapattrs.DoRun", domainType.getName(), getCorrectManager(domainType),
        a.getName(), hasValue());
    // preconditions: no null key or value, a long key within what a browser represents (a put that fails them is
    // neither executed nor sent on)
    getCD4C().addMethod(ast, "umlp.commands.mapattrs.CheckContract", hasValue(),
        hasValue() && GenService.isLongType(keyType));
    getCD4C().addMethod(ast, "umlp.commands.GeneralCheckAccessPolicy", domainType.getName(), getPermission());
    getCD4C().addMethod(ast, "umlp.commands.GetEffectedGemIds", Lists.newArrayList("getGemId()"));

    addImports(origCD, domainType, ast);
    getValueEnum(a).ifPresent(v -> getCD4C().addImport(ast, getEnumQName(v)));
    return ast;
  }

  protected ASTCDClass createCommandDeSer(ASTCDCompilationUnit origCD, ASTCDAttribute a, ASTCDType domainType) {
    String cmdName = getCommandName(domainType, a);
    SymTypeExpression type = a.getSymbol().getType();
    SymTypeExpression keyType = GenService.getMapTypeArgument(type, 0).get();
    SymTypeExpression valueType = GenService.getMapTypeArgument(type, 1).get();
    Optional<SymTypeExpression> valueEnum = getValueEnum(a);
    // keys and values with a DeSer of the object mapper (date/time types), like the elements of list attributes
    String keyTypeName = keyType.printFullName();
    boolean keyUseDeSer = supportedDeSers.contains(keyTypeName);
    String valueTypeName = valueType.printFullName();
    boolean useDeSer = supportedDeSers.contains(valueTypeName);
    String objectMapperName = getPackageName(origCD, DEFAULT_PACKAGE) + "." + origCD.getCDDefinition().getName()
        + BaseClassObjectMapperDecorator.SUFFIX;
    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("CommandDeSer<" + cmdName + ">"))
        .setName(cmdName + DE_SER)
        .build();
    getCD4C().addMethod(ast, "umlp.commands.deser.MapAttrCommand_FromJsonObject", cmdName,
        GenService.calculateJSon(keyType), keyUseDeSer, keyTypeName, hasValue(), GenService.calculateJSon(valueType),
        valueEnum.map(SymTypeExpression::print).orElse(""), useDeSer, valueTypeName, objectMapperName);
    getCD4C().addMethod(ast, "umlp.commands.deser.MapAttrCommand_BuildJson", cmdName, keyUseDeSer, keyTypeName,
        hasValue(), GenService.getGetterName("value", valueType), valueEnum.isPresent(), useDeSer, valueTypeName,
        objectMapperName);
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + domainType.getName());
    valueEnum.ifPresent(v -> getCD4C().addImport(ast, getEnumQName(v)));
    addedDeSers.add(ast);
    return ast;
  }

  /**
   * @return the value type if it is an enum (its values are sent by name)
   */
  protected Optional<SymTypeExpression> getValueEnum(ASTCDAttribute a) {
    return GenService.getMapTypeArgument(a.getSymbol().getType(), 1).filter(GenService::isCDEnum);
  }

  protected String getEnumQName(SymTypeExpression enumType) {
    ASTCDType enumAST = (ASTCDType) enumType.getTypeInfo().getAstNode();
    return getPackageName(UMLPSymbolTables.getDiagramFor(enumAST), DEFAULT_PACKAGE) + "." + enumType.print();
  }

  protected void addImports(ASTCDCompilationUnit origCD, ASTCDType domainType, ASTCDClass ast) {
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("StatusResult"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("Command"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("GeneralErrorCollector"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + domainType.getName());
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + getCorrectManager(domainType));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + domainType.getName() + "AccessPolicy");
    getCD4C().addImport(ast, "com.google.common.collect.Sets");
    getCD4C().addImport(ast, "de.se_rwth.commons.logging.Log");
  }
}
