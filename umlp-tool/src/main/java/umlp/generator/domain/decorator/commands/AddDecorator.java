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
import de.monticore.symbols.basicsymbols.BasicSymbolsMill;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.monticore.types.check.SymTypeOfGenerics;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.domain.decorator.BaseClassObjectMapperDecorator;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.util.PathUtil;
import umlp.generator.util.UMLPSymbolTables;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static de.monticore.cd.facade.CDModifier.PUBLIC_ABSTRACT;
import static umlp.generator.domain.decorator.commands.CommandDecorator.DE_SER;

public class AddDecorator extends AbstractDeSerDecorator {

  public static final String SUFFIX = "_add";
  protected Set<String> supportedDeSers;

  public AddDecorator(GlobalExtensionManagement glex, Set<String> supportedDeSers) {
    super(glex);
    this.supportedDeSers = supportedDeSers;
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    addedDeSers.clear();
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, COMMAND_PACKAGE);
    getNonDerivedDomainClasses(origCD).forEach(c -> {
      CDSymbolTables.getAttributesInHierarchy(c)
          .stream()
          .filter(a -> !a.getModifier().isDerived())
          .filter(GenService::isList)
          .forEach(a -> {
            astcdPackage.addCDElement(withOrigin(createClass(origCD,
                a.getName(),
                ((SymTypeOfGenerics) a.getSymbol().getType()).getArgument(0),
                c,
                getCorrectManager(a),
                false)));
            astcdPackage.addCDElement(withOrigin(createDeSer(origCD,
                a.getName(),
                a.getSymbol().getType(),
                c,
                false)));
          });
      CDSymbolTables.getAssociations(c)
          .stream()
          .filter(Predicate.not(GenService::isSymbolDerived))
          .filter(Predicate.not(GenService::isBidirectional))
          .filter(GenService::isMultiValued)
          // maps have no positions: their values are put, see PutDecorator
          .filter(Predicate.not(GenService::isMap))
          .forEach(a -> {
            ASTCDClass clazz = createClass(
                origCD, a.getName(), a.getSymbol().getType(),
                c, getCorrectManagerOfTargetType(a), true
            );
            astcdPackage.addCDElement(withOrigin(clazz));
            addManagerImportForAssoc(origCD, clazz, a);
            astcdPackage.addCDElement(withOrigin(createDeSer(origCD,
                a.getName(),
                getSymType("long"),
                c,
                true)));
          });
    });
    getNonDerivedDomainInterfaces(origCD).forEach(c -> {
      CDSymbolTables.getAttributesInHierarchy(c)
          .stream()
          .filter(a -> !a.getModifier().isDerived())
          .filter(GenService::isList)
          .forEach(a -> {
            astcdPackage.addCDElement(withOrigin(createClass(origCD, a.getName(), ((SymTypeOfGenerics) a.getSymbol().getType()).getArgument(0), c, getCorrectManager(a), false)));
            astcdPackage.addCDElement(withOrigin(createDeSer(origCD, a.getName(), a.getSymbol().getType(), c, false)));
          });
      CDSymbolTables.getAssociations(c)
          .stream()
          .filter(Predicate.not(GenService::isSymbolDerived))
          .filter(Predicate.not(GenService::isBidirectional))
          .filter(GenService::isMultiValued)
          // maps have no positions: their values are put, see PutDecorator
          .filter(Predicate.not(GenService::isMap))
          .forEach(a -> {
            ASTCDClass clazz = createClass(origCD, a.getName(), a.getSymbol().getType(), c, getCorrectManagerOfTargetType(a), true);
            astcdPackage.addCDElement(withOrigin(clazz));
            addManagerImportForAssoc(origCD, clazz, a);
            astcdPackage.addCDElement(withOrigin(createDeSer(origCD, a.getName(), getSymType("long"), c, true)));
          });
    });
  }


  protected ASTCDClass createClass(ASTCDCompilationUnit origCD, String name, SymTypeExpression type, ASTCDType domainClass,
      String targetTypeManager, boolean isGem) {
    String classname = domainClass.getName() + SUFFIX
        + StringTransformations.capitalize(name);
    String attrName = isGem ? name + "GemId" : name;

    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setName(classname)
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(extendedClass()))
        .build();

    ASTCDAttribute gemAttr = getCD4C().addAttribute(ast, "protected long gemId");
    ASTCDAttribute attr = getCD4C().addAttribute(ast, "protected " + (isGem ? "long" : type.print()) + " " + attrName);
    ASTCDAttribute indexAttr = getCD4C().addAttribute(ast, "protected int indexInList");

    addConstructor(ast, classname, getPackageName(origCD, COMMAND_PACKAGE),
        attrName, isGem ? "long" : type.print());

    getCD4C().addMethod(ast, getDoRunTemplate(), name, domainClass.getName(), getCorrectManager(domainClass), isGem, name, type.print(), targetTypeManager, hasOCL);
    getCD4C().addMethod(ast, getGeneralCheckAccessPolicyTemplate(), domainClass.getName(), "Create");
    if (isGem) {
      getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(), Lists.newArrayList("gemId", name + "GemId"));
    }
    else {
      getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(), Lists.newArrayList("getGemId()"));
    }
    AccessorDecorator accessor = new AccessorDecorator(glex);
    ast.addAllCDMembers(accessor.decorate(gemAttr));
    ast.addAllCDMembers(accessor.decorate(attr));
    ast.addAllCDMembers(accessor.decorate(indexAttr));

    addImports(origCD, domainClass, ast);
    addCorrectMangerImportForTargetType(origCD.getCDDefinition(), ast, type);

    if (isGem) {
      ASTCDCompilationUnit typeCD = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());
      addTypeImport(origCD, ast, domainClass, typeCD, type);
    }
    else if (GenService.isCDEnum(type)) {
      ASTCDCompilationUnit typeCD = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());
      addTypeImport(origCD, ast, domainClass, typeCD, type);
    }
    else if (GenService.isNotADomainType(type)) {
      addJavaImport(ast, type);
    }

    return ast;
  }

  protected void addManagerImportForAssoc(ASTCDCompilationUnit origCD, ASTCDClass clazz, ASTCDAssocSide a) {
    getCD4C().addImport(clazz, getManagerOfTargetTypeQName(a));
  }

  protected void addTypeImport(ASTCDCompilationUnit origCD, ASTCDClass ast, ASTCDType domainClass, ASTCDCompilationUnit typeCD, SymTypeExpression type) {
    getCD4C().addImport(ast, getPackageName(typeCD, DEFAULT_PACKAGE) + "." + type.print());
  }

  protected String getGeneralCheckAccessPolicyTemplate() {
    return "umlp.commands.GeneralCheckAccessPolicy";
  }

  protected void addImports(ASTCDCompilationUnit origCD, ASTCDType domainClass, ASTCDClass ast) {
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("ValidationResult"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("StatusResult"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("Command"));
    getCD4C().addImport(ast, PathUtil.getBackendRTEUtilPath("LogUtil"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("GeneralErrorCollector"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + domainClass.getName());
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + getCorrectManager(domainClass));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + domainClass.getName() + "AccessPolicy");
    getCD4C().addImport(ast, "com.google.common.collect.Sets");
  }

  protected void addConstructor(ASTCDClass ast, String classname, String packageName,
      String attrName, String type) {
    getCD4C().addConstructor(ast, getConstructorTemplate(), classname, packageName,
        attrName, type);
  }

  protected String extendedClass() {
    return "Command<Void>";
  }

  protected String getConstructorTemplate() {
    return "umlp.commands.add.Constructor";
  }

  protected String getGetEffectedGemIdsTemplate() {
    return "umlp.commands.GetEffectedGemIds";
  }

  protected String getDoRunTemplate() {
    return "umlp.commands.add.DoRun";
  }

  protected ASTCDClass createDeSer(ASTCDCompilationUnit origCD, String name, SymTypeExpression type, ASTCDType domainClass, boolean isGem) {
    String cmdName = domainClass.getName() + SUFFIX
        + StringTransformations.capitalize(name);

    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("CommandDeSer<" + cmdName + ">"))
        .setName(cmdName + DE_SER)
        .build();

    if (isGem) {
      type = getSymType("long");
      name += "GemId";
    }

    SymTypeExpression innerType = type;
    if (GenService.isList(type)) {
      innerType = ((SymTypeOfGenerics) type).getArgument(0);
    }
    else if (GenService.isOptional(type)) {
      innerType = ((SymTypeOfGenerics) type).getArgument(0);
    }
    String jsonStr = GenService.calculateJSon(innerType);
    String typeName = innerType.printFullName();
    String getter = GenService.getGetterName(name, innerType);

    String baseClassDecorator = getPackageName(origCD, DEFAULT_PACKAGE) + "." + origCD.getCDDefinition().getName() + BaseClassObjectMapperDecorator.SUFFIX;
    boolean useDeSer = supportedDeSers.contains(typeName);
    boolean makeAbstract = jsonStr.isEmpty() && !useDeSer;
    getCD4C().addMethod(ast, getAdd_FromJsonObjectTemplate(), cmdName, name, typeName,
        GenService.isInnerCDEnum(type), jsonStr, useDeSer, baseClassDecorator, makeAbstract);
    getCD4C().addMethod(ast, getAdd_BuildJsonTemplate(), cmdName, name, typeName,
        GenService.isInnerCDEnum(type), getter, useDeSer,
        baseClassDecorator, makeAbstract);

    addDeSerImport(ast, origCD, domainClass, type, innerType, cmdName);
    if (makeAbstract) {
      ast.setModifier(PUBLIC_ABSTRACT.build());
    }
    addedDeSers.add(ast);
    return ast;
  }

  protected void addDeSerImport(ASTCDClass ast, ASTCDCompilationUnit origCD,
      ASTCDType domainClass, SymTypeExpression type, SymTypeExpression innerType, String cmdName) {
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + domainClass.getName());
    if (GenService.isCDEnum(innerType)) {
      ASTCDCompilationUnit compilationUnit = UMLPSymbolTables
          .getDiagramFor(type.asGenericType().getArgument(0).getTypeInfo().getAstNode());
      getCD4C().addImport(ast, getPackageName(compilationUnit, DEFAULT_PACKAGE) + "." + ((SymTypeOfGenerics) type).getArgument(0).print());
    }
    else if (GenService.isNotADomainType(innerType)) {
      getCD4C().addImport(ast, innerType.printFullName());
    }
  }

  protected String getAdd_BuildJsonTemplate() {
    return "umlp.commands.deser.Add_BuildJson";
  }

  protected String getAdd_FromJsonObjectTemplate() {
    return "umlp.commands.deser.Add_FromJsonObject";
  }

  // TODO (MB): Bessere Lösung?
  protected SymTypeExpression getSymType(String type) {
    Optional<TypeSymbol> primitive = BasicSymbolsMill.globalScope().resolveType(type);
    if (primitive.isPresent()) {
      return SymTypeExpressionFactory.createPrimitive(primitive.get());
    }
    else {
      return SymTypeExpressionFactory.createObscureType();
    }
  }
}
