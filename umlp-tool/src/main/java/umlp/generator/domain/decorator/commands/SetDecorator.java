/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;/* (c) https://github.com/MontiCore/monticore */

import com.google.common.collect.Lists;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd.codegen.methods.AccessorDecorator;
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
import de.se_rwth.commons.Names;
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

public class SetDecorator extends AbstractDeSerDecorator {

  public static final String SUFFIX = "_set";

  private Set<String> supportedDeSers;

  public SetDecorator(GlobalExtensionManagement glex, Set<String> supportedDeSers) {
    super(glex);
    this.supportedDeSers = supportedDeSers;
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    addedDeSers.clear();
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, COMMAND_PACKAGE);
    getConcreteDomainClasses(origCD).forEach(c -> {
      CDSymbolTables.getAttributesInHierarchy(c)
          .stream()
          .filter(a -> !a.getModifier().isDerived())
          .filter(a -> !GenService.isList(a))
          .forEach(a -> {
            astcdPackage.addCDElement(withOrigin(createClass(origCD,
                a.getName(),
                a.getSymbol().getType(),
                c,
                getCorrectManager(a),
                false,
                astcdPackage.getName())));
            astcdPackage.addCDElement(withOrigin(createDeSer(origCD,
                a.getName(),
                a.getSymbol().getType(),
                c,
                false)));
          });
      CDSymbolTables.getAssociationsInHierarchy(c)
          .stream()
          .filter(Predicate.not(GenService::isSymbolDerived))
          .filter(a -> !GenService.isBidirectional(a))
          .filter(a -> !GenService.isMultiValued(a))
          .forEach(a -> {
            astcdPackage.addCDElement(withOrigin(createClass(origCD,
                a.getName(),
                a.getSymbol().getType(),
                c,
                getCorrectManagerOfTargetType(a),
                true,
                astcdPackage.getName())));
            astcdPackage.addCDElement(withOrigin(createDeSer(origCD, a.getName(), getSymType("long"), c, true)));
          });
    });

    getNonDerivedDomainInterfaces(origCD).forEach(c -> {
      CDSymbolTables.getAttributesInHierarchy(c)
          .stream()
          .filter(a -> !a.getModifier().isDerived())
          .filter(a -> !GenService.isList(a))
          .forEach(a -> {
            astcdPackage.addCDElement(withOrigin(createClass(origCD, a.getName(), a.getSymbol().getType(), c, getCorrectManager(a), false, astcdPackage.getName())));
            astcdPackage.addCDElement(withOrigin(createDeSer(origCD, a.getName(), a.getSymbol().getType(), c, false)));
          });
      CDSymbolTables.getAssociations(c)
          .stream()
          .filter(Predicate.not(GenService::isSymbolDerived))
          .filter(a -> !GenService.isBidirectional(a))
          .filter(a -> !GenService.isMultiValued(a))
          .forEach(a -> {
            astcdPackage.addCDElement(withOrigin(createClass(origCD, a.getName(), a.getSymbol().getType(), c, getCorrectManagerOfTargetType(a), true, astcdPackage.getName())));
            astcdPackage.addCDElement(withOrigin(createDeSer(origCD, a.getName(), getSymType("long"), c, true)));
          });
    });
  }


  protected ASTCDClass createClass(ASTCDCompilationUnit origCD, String name, SymTypeExpression type, ASTCDType domainType,
      String targetTypeManager, boolean isGem, String packageName) {
    String classname = domainType.getName() + SUFFIX
        + StringTransformations.capitalize(name);
    String completeName = isGem ? name + "GemId" : name;
    String domainPackage = getPackageName(origCD, DEFAULT_PACKAGE);


    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setName(classname)
        .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(extendedClass()))
        .build();

    ASTCDAttribute gemAttr = getCD4C().addAttribute(ast, "protected long gemId");


    ASTCDAttribute attr = isGem ? getCD4C().addAttribute(ast, "protected long " + completeName)
        : getCD4C().addAttribute(ast, "protected " + GenService.createType(type).printType() + " " + completeName);

    addConstructor(ast, classname, getPackageName(origCD, COMMAND_PACKAGE),
        completeName, isGem ? "long" : GenService.createType(type).printType());

    getCD4C().addMethod(ast, getDoRunTemplate(), name, domainType.getName(), getCorrectManager(domainType), isGem, type.print(), targetTypeManager, domainPackage, hasOCL);
    if (isGem) {
      String otherType = GenService.createType(type).printType();

      getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(), Lists.newArrayList("gemId", name + "GemId"));
      getCD4C().addMethod(ast, getAssocsCheckAccessPolicyTemplate(), domainType.getName(), otherType, "Update");
      addImportAccessPolicy(ast, type, otherType, domainType);
    }
    else {
      getCD4C().addMethod(ast, getGeneralCheckAccessPolicyTemplate(), domainType.getName(), "Update");
      getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(), Lists.newArrayList("getGemId()"));
    }
    AccessorDecorator accessor = getAccessorDecorator();
    ast.addAllCDMembers(accessor.decorate(gemAttr));
    ast.addAllCDMembers(accessor.decorate(attr));

    addImports(ast, origCD, domainType, type, isGem, attr);

    return ast;
  }

  protected void addImportAccessPolicy(ASTCDClass ast, SymTypeExpression type, String otherType, ASTCDType domainType) {
    ASTCDCompilationUnit cd = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());
    getCD4C().addImport(ast, getPackageName(cd, DEFAULT_PACKAGE) + "." + otherType + "AccessPolicy");
  }

  protected String getAssocsCheckAccessPolicyTemplate() {
    return "umlp.commands.assocs.AssocsCheckAccessPolicy";
  }

  protected String getGeneralCheckAccessPolicyTemplate() {
    return "umlp.commands.GeneralCheckAccessPolicy";
  }

  protected String extendedClass() {
    return "Command<Void>";
  }

  protected void addConstructor(ASTCDClass ast, String classname, String packageName,
      String attrName, String type) {
    getCD4C().addConstructor(ast, getConstructorTemplate(), classname, packageName,
        attrName, type);
  }

  protected AccessorDecorator getAccessorDecorator() {
    return new AccessorDecorator(glex);
  }

  protected String getConstructorTemplate() {
    return "umlp.commands.set.Constructor";
  }

  protected String getGetEffectedGemIdsTemplate() {
    return "umlp.commands.GetEffectedGemIds";
  }

  protected String getDoRunTemplate() {
    return "umlp.commands.set.DoRun";
  }

  protected void addImports(ASTCDClass ast, ASTCDCompilationUnit origCD, ASTCDType domainType,
      SymTypeExpression type, boolean isGem, ASTCDAttribute attr) {
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("ValidationResult"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("StatusResult"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("Command"));
    getCD4C().addImport(ast, PathUtil.getBackendRTEUtilPath("LogUtil"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("GeneralErrorCollector"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + domainType.getName());
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + getCorrectManager(domainType));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + domainType.getName() + "AccessPolicy");
    getCD4C().addImport(ast, "com.google.common.collect.Sets");
    addCorrectMangerImportForTargetType(origCD.getCDDefinition(), ast, type);
    if (GenService.isOptional(type)) {
      if (GenService.isInnerCDEnum(type)) {
        ASTCDCompilationUnit compilationUnit = UMLPSymbolTables
            .getDiagramFor(type.asGenericType().getArgument(0).getTypeInfo().getAstNode());
        getCD4C().addImport(ast, getPackageName(compilationUnit, DEFAULT_PACKAGE) + "." + ((SymTypeOfGenerics) type).getArgument(0).print());
      }
      else if (GenService.isNotADomainType(((SymTypeOfGenerics) type).getArgument(0))) {
        getCD4C().addImport(ast, ((SymTypeOfGenerics) type).getArgument(0).printFullName());
      }
    }
    else {
      if (GenService.isCDEnum(type)) {
        ASTCDCompilationUnit compilationUnit = UMLPSymbolTables.getDiagramFor(type.getTypeInfo().getAstNode());
        getCD4C().addImport(ast,getPackageName(compilationUnit, DEFAULT_PACKAGE)+"."+ type.print());
      }
      else if (GenService.isNotADomainType(type)) {
        getCD4C().addImport(ast, type.printFullName());
      }
      else if (isGem) {
        ASTCDCompilationUnit typeCD = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());
        getCD4C().addImport(ast, getPackageName(typeCD, DEFAULT_PACKAGE) + "." + type.print());
      }
    }
  }

  protected ASTCDClass createDeSer(ASTCDCompilationUnit origCD, String name, SymTypeExpression type, ASTCDType domainType, boolean isGem) {
    String cmdName = domainType.getName() + SUFFIX
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
    String jsonStr;
    String typeName;
    String packageName;
    boolean isOptional = false;
    boolean isCDEnum;
    if (GenService.isOptional(type)) {
      jsonStr = GenService.calculateJSon(((SymTypeOfGenerics) type).getArgument(0));
      typeName = ((SymTypeOfGenerics) type).getArgument(0).printFullName();
      packageName = ((SymTypeOfGenerics) type).getArgument(0).getTypeInfo().getPackageName().toLowerCase();
      isOptional = true;
      isCDEnum = GenService.isInnerCDEnum(type);
    }
    else {
      jsonStr = GenService.calculateJSon(type);
      typeName = type.printFullName();
      isCDEnum = GenService.isCDEnum(type);
      packageName = type.getTypeInfo().getPackageName().toLowerCase();
      if (GenService.isNotADomainType(type)) {
        addDeSerJavaImport(ast, typeName);
      }
    }
    String baseClassDecorator = origCD.getCDDefinition().getName() + BaseClassObjectMapperDecorator.SUFFIX;
    boolean useDeSer = supportedDeSers.contains(typeName);
    boolean makeAbstract = jsonStr.equals("") && !useDeSer;
    getCD4C().addMethod(ast, getSet_FromJsonObjectTemplate(), cmdName, name, typeName, isOptional, isCDEnum, jsonStr, useDeSer, baseClassDecorator, makeAbstract);
    getCD4C().addMethod(ast, getSet_BuildJsonTemplate(), cmdName, name, typeName, isCDEnum, GenService.getGetterName(name, type), useDeSer, baseClassDecorator, makeAbstract, isOptional);

    addDeSerImport(ast, origCD, useDeSer, isCDEnum, baseClassDecorator, typeName, packageName);

    if (makeAbstract) {
      ast.setModifier(PUBLIC_ABSTRACT.build());
    }

    addedDeSers.add(ast);
    return ast;
  }

  protected void addDeSerImport(ASTCDClass ast, ASTCDCompilationUnit origCD, boolean useDeSer,
      boolean isCDEnum, String baseClassDecorator, String typeName, String packageName) {
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
    if (isCDEnum) {
      getCD4C().addImport(ast, packageName + "." + Names.getSimpleName(typeName));
    }
    if (useDeSer) {
      getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + baseClassDecorator);
    }
  }

  protected void addDeSerJavaImport(ASTCDClass ast, String typeName) {
    getCD4C().addImport(ast, typeName);
  }

  protected String getSet_BuildJsonTemplate() {
    return "umlp.commands.deser.Set_BuildJson";
  }

  protected String getSet_FromJsonObjectTemplate() {
    return "umlp.commands.deser.Set_FromJsonObject";
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
