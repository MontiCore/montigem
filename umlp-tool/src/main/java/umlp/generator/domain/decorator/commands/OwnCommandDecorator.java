/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.TopDecorator;
import de.monticore.cd.codegen.methods.AccessorDecorator;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cd4codebasis._ast.ASTCDParameter;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeOfGenerics;
import de.monticore.types.mcbasictypes._ast.ASTMCObjectType;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types.mccollectiontypes._ast.ASTMCCollectionTypesNode;
import de.monticore.types.mccollectiontypes._ast.ASTMCListType;
import de.monticore.types.mccollectiontypes._ast.ASTMCOptionalType;
import de.monticore.types.mcsimplegenerictypes._ast.ASTMCSimpleGenericTypesNode;
import de.monticore.umlstereotype._ast.ASTStereotypeBuilder;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import umlp.UMLPMill;
import umlp.generator.Filter;
import umlp.generator.domain.decorator.AccessPolicyDecorator;
import umlp.generator.domain.decorator.BaseClassObjectMapperDecorator;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.util.DecoratorUtil;
import umlp.generator.util.PathUtil;
import umlp.generator.util.TagLookUp;
import umlp.generator.util.UMLPSymbolTables;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static de.monticore.cd.facade.CDModifier.PUBLIC_ABSTRACT;
import static umlp.generator.domain.decorator.commands.CommandDecorator.DE_SER;

public class OwnCommandDecorator extends AbstractDeSerDecorator implements OwnCommands {

  public static final String SINGLETON = "singleton";

  protected Set<String> supportedDeSers;

  public OwnCommandDecorator(GlobalExtensionManagement glex, Set<String> supportedDeSers) {
    super(glex);
    this.supportedDeSers = supportedDeSers;
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    addedDeSers.clear();
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, COMMAND_PACKAGE);
    Predicate<Filter.Wrapper> filter = Filter.DERIVED.negate().and(Filter.SINGLETON);
    getDomainClasses(origCD, filter)
        .forEach(c -> c.getCDMethodList().stream()
                          //   .filter(m -> GenService.hasStereotype(m.getModifier(), COMMAND))
        //  .filter(m -> TagLookUp.isCommand(m.getSymbol())) // Doesn't work properly anymore
          .forEach(m -> {
             astcdPackage.addCDElement(withOrigin(createCommandClass(origCD, m)));
             astcdPackage.addCDElement(withOrigin(createCommandDeSer(origCD, m)));
          }
        )
    );

    getDomainInterfaces(origCD, filter)
        .forEach(c -> c.getCDMethodList().stream()
        // .filter(m -> GenService.hasStereotype(m.getModifier(), COMMAND))
          .filter(m -> TagLookUp.isCommand(m.getSymbol()))
          .forEach(m -> {
            astcdPackage.addCDElement(withOrigin(createCommandClass(origCD, m)));
            astcdPackage.addCDElement(withOrigin(createCommandDeSer(origCD, m)));
          }
        )
    );
  }

  protected ASTCDClass createCommandClass(ASTCDCompilationUnit origCD, ASTCDMethod meth) {
    String classname = StringTransformations.capitalize(meth.getName());

    ASTCDClass ast = UMLPMill.cDClassBuilder()
                         .setModifier(PUBLIC.build())
                         .setName(classname)
                         .build();
    ast.setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(extendedClass(meth, ast)));
    ast.setModifier(PUBLIC_ABSTRACT.build());

    ASTStereotypeBuilder stereotype = UMLPMill.stereotypeBuilder();
    stereotype.addValues(
        TopDecorator.NEEDS_TOP_STEREO_BUILDER.apply(
            String.format("Custom implementation of command class %%s required - use the TOP mechanism. " +
                "Please implement the %s#doAction() method", classname)));
    ast.getModifier().setStereotype(stereotype.build());

    List<ASTCDAttribute> attrList = Lists.newArrayList();
    meth.getCDParameterList().forEach(p -> attrList.add(getCD4C().addAttribute(ast, getAttributeForMethodParameter(p))));

    getCD4C().addConstructor(ast, getConstructorTemplate(), classname, getPackageName(origCD, COMMAND_PACKAGE), attrList);

    AccessorDecorator accessor = DecoratorUtil.streamlessListAcessor(glex);
    attrList.forEach(a -> ast.addAllCDMembers(accessor.decorate(a)));

    String returnType = buildReturnType(meth, ast).printType();
    getCD4C().addMethod(ast, getDoActionTemplate(), returnType);

    // Add reply methods
    if (meth.getMCReturnType().isPresentMCVoidType()) {
      getCD4C().addMethod(ast, getReplySuccessStatusTemplate());
      getCD4C().addMethod(ast, getReplyFailStatusTemplate());
    } else if (meth.getMCReturnType().isPresentMCType()){
      ASTMCType paramTypeSym = meth.getMCReturnType().getMCType();
      String paramType = paramTypeSym.printType();

      if (GenService.isList(paramTypeSym)) {
        paramType = ((ASTMCListType)paramTypeSym).getMCTypeArgumentList().get(0).printType();
        var symbol = meth.getEnclosingScope().resolveType(paramType);
        if (symbol.isEmpty())
          Log.error("Unable to resolve Optional inner type " + paramType, meth.get_SourcePositionStart());
        String paramFQN = GenService.getCorrectJavaFQN(symbol.get().getFullName());
        // Handle native types
        if (DerivedClassCommandDecorator.nativeTypes.contains(paramFQN) || DerivedClassCommandDecorator.nativeTypes.contains(boxType(paramFQN)))
          paramFQN = "java.lang." + boxType(paramFQN);
        getCD4C().addMethod(ast, getReplySuccessListTemplate(), returnType, paramType, paramFQN);
        getCD4C().addMethod(ast, getReplyFailListTemplate(), returnType, paramFQN);
      } else {
        String paramFQN;
        boolean isOptional = GenService.isOptional(paramTypeSym);
        if (isOptional) {
          paramType = ((ASTMCOptionalType)paramTypeSym).getMCTypeArgumentList().get(0).printType();
          var symbol = meth.getEnclosingScope().resolveType(paramType);
          if (symbol.isEmpty()) {
            Log.error("Unable to resolve Optional inner type " + paramType, meth.get_SourcePositionStart());
          }
          paramFQN = GenService.getCorrectJavaFQN(symbol.get().getFullName());
        } else if (paramTypeSym.getDefiningSymbol().isPresent()) {
          paramFQN = GenService.getCorrectJavaFQN(paramTypeSym.getDefiningSymbol().get().getFullName());
        } else {
          Log.error("Unable to detect type from command ", meth.get_SourcePositionStart());
          paramFQN = "Object";
        }

        // Handle native types
        if (DerivedClassCommandDecorator.nativeTypes.contains(paramFQN) || DerivedClassCommandDecorator.nativeTypes.contains(boxType(paramFQN)))
          paramFQN = "java.lang." + boxType(paramFQN);

        getCD4C().addMethod(ast, getReplySuccessTemplate(), returnType, paramType, paramFQN);
        getCD4C().addMethod(ast, getReplyFailTemplate(), returnType, paramFQN);

        if (isOptional) {
          getCD4C().addMethod(ast, getReplyEmptyTemplate(), returnType);
        }

      }
    }

    getCD4C().addMethod(ast, getGetEffectedGemIdsTemplate(), Lists.newArrayList());

    addImports(ast);

    List<SymTypeExpression> accessPolicyParameters = new ArrayList<>();
    meth.getCDParameterList().forEach(p -> {
      SymTypeExpression type = p.getSymbol().getType();
      if (GenService.isDomainType(type) && !GenService.isCDOrInnerCDEnum(type)) {
        accessPolicyParameters.add(type);
      }
      addImport(type, ast);
    });

    getCD4C().addMethod(ast, getCheckAccessPolicyTemplate(), accessPolicyParameters);

    return ast;
  }

  protected String getAttributeForMethodParameter(ASTCDParameter param) {
    if (GenService.isCDEnum(param.getSymbol().getType())) {
      return "protected " + param.getSymbol().getType().getTypeInfo().getName() + " " + StringTransformations.uncapitalize(param.getName());
    }
    if (GenService.isInnerCDEnum(param.getSymbol().getType())) {
      if (GenService.isList(param.getSymbol().getType())) {
        return "protected List<" + param.getSymbol().getType().asGenericType().getArgument(0).getTypeInfo().getName() + "> " + StringTransformations.uncapitalize(param.getName());
      }
      else if (GenService.isSet(param.getSymbol().getType())) {
        return "protected Set<" + param.getSymbol().getType().asGenericType().getArgument(0).getTypeInfo().getName() + "> " + StringTransformations.uncapitalize(param.getName());
      }
      else if (GenService.isOptional(param.getSymbol().getType())) {
        return "protected Optional<" + param.getSymbol().getType().asGenericType().getArgument(0).getTypeInfo().getName() + "> " + StringTransformations.uncapitalize(param.getName());
      }
      else {
        Log.error("Unhandled inner Enum type " + param.getSymbol().getType(), param.get_SourcePositionStart());
      }
    }
    if (isOptionalGem(param.getSymbol().getType())) {
      return "protected Optional<Long> " + param.getName() + "GemId";
    }
    if (isGem(param.getSymbol().getType())) {
      return "protected long " + param.getName() + "GemId";
    }
    if (isListOfGemIdentifiable(param.getSymbol().getType())) {
      return "protected List<Long> " + param.getName() + "GemId";
    }
    return "protected " + UMLPMill.prettyPrint(param.getMCType(), false) + " " + param.getName();
  }

  protected void addImports(ASTCDClass ast) {
    getCD4C().addImport(ast, PathUtil.getBackendRTERequestPath("Request"));
    getCD4C().addImport(ast, PathUtil.getBackendRTEUtilPath("LogUtil"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("NotificationScope"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("GeneralErrorCollector"));
    getCD4C().addImport(ast, PathUtil.getBackendRTECommonPath("LoadingState"));
    getCD4C().addImport(ast, "com.google.common.collect.Sets");
    getCD4C().addImport(ast, "java.util.ListIterator");
    getCD4C().addImport(ast, "java.util.Spliterator");
  }

  protected void addImport(SymTypeExpression type, ASTCDClass ast) {
    if(type.getTypeInfo().isPresentAstNode()) {
      ASTCDCompilationUnit typeCD = UMLPSymbolTables.getDiagramFor((ASTCDType) type.getTypeInfo().getAstNode());
      if (GenService.isDomainType(type) || GenService.isPojoCDType(type)) {
        getCD4C().addImport(ast, getPackageName(typeCD, DEFAULT_PACKAGE) + "." + type.print());
        if (!GenService.isCDEnum(type)) {
          getCD4C().addImport(ast, getPackageName(typeCD, DEFAULT_PACKAGE) + "." + type.print()
              + AccessPolicyDecorator.SUFFIX);
        }
      }
      else if (GenService.isCDEnum(type)) {
        getCD4C().addImport(ast, getPackageName(typeCD, DEFAULT_PACKAGE) + "." + type.print());
      }
    }
    else if (GenService.isJavaType(type)) {
      addJavaImport(ast, type);
    }
    else {
      // Assume guiLib Import?
      if(GenService.isPojoCDType(type) && type.getTypeInfo().getFullName().startsWith("mc.fenix")) {
        System.out.println(type.getTypeInfo().getFullName());
        getCD4C().addImport(ast, Joiners.DOT.join(type.getTypeInfo().getPackageName().toLowerCase(), type.print()));
      }
    }
  }

  protected ASTMCObjectType extendedClass(ASTCDMethod command, ASTCDClass ast) {
    SymTypeExpression returnTypeSymbol = command.getSymbol().getType();
    if (returnTypeSymbol.isVoidType()) {
      return buildExtendedCommandType("Void");
    }
    if (returnTypeSymbol.isGenericType()) {
      SymTypeExpression innerType = ((SymTypeOfGenerics) returnTypeSymbol).getArgumentList().get(0);
      addImport(innerType, ast);
      if (GenService.isList(returnTypeSymbol) || GenService.isOptional(returnTypeSymbol)) {
        return buildExtendedCommandType(innerType.getTypeInfo().getName());
      }
      addImport(returnTypeSymbol, ast);
      return buildExtendedCommandType(returnTypeSymbol.printFullName());
    }
    addImport(returnTypeSymbol, ast);
    return buildExtendedCommandType(returnTypeSymbol.getTypeInfo().getName());
  }

  protected String getConstructorTemplate() {
    return "umlp.commands.own.Constructor";
  }

  protected String getGetEffectedGemIdsTemplate() {
    return "umlp.commands.GetEffectedGemIds";
  }

  protected String getDoActionTemplate() {
    return "umlp.commands.own.DoAction";
  }

  protected String getReplySuccessTemplate() {
    return "umlp.commands.own.ReplySuccess";
  }

  protected String getReplyFailTemplate() {
    return "umlp.commands.own.ReplyFail";
  }

  protected String getReplyEmptyTemplate() {
    return "umlp.commands.own.ReplyEmpty";
  }

  protected String getReplySuccessListTemplate() {
    return "umlp.commands.own.ReplySuccessList";
  }

  protected String getReplyFailListTemplate() {
    return "umlp.commands.own.ReplyFailList";
  }

  protected String getReplySuccessStatusTemplate() {
    return "umlp.commands.own.ReplySuccessStatus";
  }

  protected String getReplyFailStatusTemplate() {
    return "umlp.commands.own.ReplyFailStatus";
  }

  protected String getCheckAccessPolicyTemplate() {
    return "umlp.commands.own.CheckAccessPolicy";
  }

  protected ASTCDClass createCommandDeSer(ASTCDCompilationUnit origCD, ASTCDMethod meth) {
    String cmdName = StringTransformations.capitalize(meth.getName());

    ASTCDClass ast = UMLPMill.cDClassBuilder()
                         .setModifier(PUBLIC.build())
                         .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage("CommandDeSer<" + cmdName + ">"))
                         .setName(cmdName + DE_SER)
                         .build();

    String baseClassDecorator = origCD.getCDDefinition().getName() + BaseClassObjectMapperDecorator.SUFFIX;

    List<String> paramNameList = new ArrayList<>();
    List<SymTypeExpression> typeList = new ArrayList<>();
    List<Boolean> isCollectionTypes = new ArrayList<>(); // workaround for https://git.rwth-aachen.de/monticore/monticore/-/work_items/5257
    List<Boolean> userDeserList = new ArrayList<>();
    List<Boolean> optionals = new ArrayList<>();

    boolean useDeSer = true;
    boolean jsonEmpty = true;

    addDeSerImports(ast);

    for (ASTCDParameter p : meth.getCDParameterList()) {
      SymTypeExpression type = p.getSymbol().getType();
      String paramName = p.getName();
      /*
        TODO this check may break when we import types from other Class Diagrams that are not
        Gem identifiable
      */
      boolean originalGemList = isListOfGemIdentifiable(type);
      // Workaround for https://git.rwth-aachen.de/monticore/monticore/-/work_items/5257 (CTs behave different than java.util/SimpleGenericTypes)
      isCollectionTypes.add(p.getMCType() instanceof ASTMCCollectionTypesNode && !(p.getMCType() instanceof ASTMCSimpleGenericTypesNode));
      if (GenService.isCDOrInnerCDEnum(type)) {
        optionals.add(false);
      } else if (isOptionalGem(type) ) {
        type = GenService.getSymType("long");
        paramName += "GemId";
        optionals.add(true);
      } else {
        optionals.add(false);
        if (isGem(type)) {
          type = GenService.getSymType("long");
          paramName += "GemId";
        }
        if (originalGemList) {
          paramName += "GemIdList";
        }
      }
      paramNameList.add(paramName);
      typeList.add(type);
      if (GenService.isNotADomainType(type)) {
        addJavaImport(ast, type);
      }

      String typeName = GenService.printTypeFullName(type);
      userDeserList.add(supportedDeSers.contains(typeName));

      jsonEmpty = jsonEmpty && GenService.calculateJsonInclGenerics(type, paramName).isEmpty();
      useDeSer = useDeSer && supportedDeSers.contains(typeName);

      addDeSerImport(ast, origCD, supportedDeSers.contains(typeName), GenService.isCDOrInnerCDEnum(type),
          baseClassDecorator, typeName);

      if (originalGemList) {
        getCD4C().addMethod(ast, getOwnCommand_FromJsonObjectParametersGemListTemplate(), paramName);
        getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonElement");
      } else if (GenService.isList(type)) {
        getCD4C().addMethod(ast, getOwnCommand_FromJsonObjectParametersListTemplate(), p.getName());
        getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonElement");
      }
    }

    boolean makeAbstract = jsonEmpty && !useDeSer;

    if (makeAbstract) {
      ast.setModifier(PUBLIC_ABSTRACT.build());
    }

    ASTCDMethodSignature fromJsonObjectMethod = getCD4C().addMethod(ast, getOwnCommand_FromJsonObjectTemplate(), cmdName, paramNameList, typeList,
        userDeserList, baseClassDecorator, makeAbstract, optionals);
    // JSweet does not support the stream operator
    glex.replaceTemplate("cd2java.Annotations", fromJsonObjectMethod,
        new StringHookPoint("@umlp.jsweet.extension.annotation.BackendOnly(umlp.jsweet.extension.annotation.BackendOnlyModes.THROW_ERROR) "));
    getCD4C().addMethod(ast, getOwnCommand_BuildJsonTemplate(), cmdName, paramNameList, typeList,
        userDeserList, baseClassDecorator, makeAbstract, isCollectionTypes);

    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));

    addedDeSers.add(ast);
    return ast;
  }

  protected void addDeSerImports(ASTCDClass ast) {
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
  }

  protected void addDeSerImport(ASTCDClass ast, ASTCDCompilationUnit origCD, boolean useDeSer,
                                boolean isCDEnum, String baseClassDecorator, String typeName) {
    if (isCDEnum) {
      getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + Names.getSimpleName(typeName));
    }
    if (useDeSer) {
      getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + baseClassDecorator);
    }
  }

  protected String getOwnCommand_BuildJsonTemplate() {
    return "umlp.commands.deser.OwnCommand_BuildJson";
  }

  protected String getOwnCommand_FromJsonObjectTemplate() {
    return "umlp.commands.deser.OwnCommand_FromJsonObject";
  }

  protected String getOwnCommand_FromJsonObjectParametersListTemplate() {
    return "umlp.commands.deser.OwnCommand_FromJsonObjectParametersList";
  }

  protected String getOwnCommand_FromJsonObjectParametersGemListTemplate() {
    return "umlp.commands.deser.OwnCommand_FromJsonObjectParametersGemList";
  }

  protected void addDeSerImport(ASTCDClass ast, ASTCDCompilationUnit origCD,
                                ASTCDType domainClass, SymTypeExpression type, SymTypeExpression innerType, String cmdName) {
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.JsonPrinter");
    getCD4C().addImport(ast, "de.monticore.symboltable.serialization.json.JsonObject");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandDeSer"));
    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + domainClass.getName());
    if (GenService.isCDEnum(innerType)) {
      getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + ((SymTypeOfGenerics) type).getArgument(0).print());
    }
    else if (GenService.isNotADomainType(innerType)) {
      getCD4C().addImport(ast, innerType.printFullName());
    }
  }

  protected String boxType(String type) {
    if (type.equals("int"))
      type = "Integer";
    else if (type.equals("boolean"))
      type = "Boolean";
    else if (type.equals("char"))
      type = "Character";
    else if (type.equals("byte"))
      type = "Byte";
    else if (type.equals("short"))
      type = "Short";
    else if (type.equals("long"))
      type = "Long";
    else if (type.equals("float"))
      type = "Float";
    else if (type.equals("double"))
      type = "Double";
    return type;
  }

}
