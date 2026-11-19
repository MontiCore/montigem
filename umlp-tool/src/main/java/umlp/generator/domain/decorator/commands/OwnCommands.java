package umlp.generator.domain.decorator.commands;

import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.types.MCTypeFacade;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeOfGenerics;
import de.monticore.types.mcbasictypes._ast.ASTMCObjectType;
import de.monticore.types.mcbasictypes._ast.ASTMCReturnType;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types.mcfullgenerictypes.MCFullGenericTypesMill;
import de.monticore.types.mcsimplegenerictypes.MCSimpleGenericTypesMill;
import de.monticore.types.mcsimplegenerictypes._ast.ASTMCBasicGenericTypeBuilder;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.util.PathUtil;

import java.util.ArrayList;
import java.util.Arrays;

import static de.monticore.types.check.SymTypePrimitive.box;

public interface OwnCommands {

  String COMMAND = "command";
  String REQUEST = "request";
  String STATUS_RESULT = "StatusResult";
  String SIMPLE_RESULT = "SimpleResult";
  String LIST_RESULT = "ListResult";

  default ASTMCObjectType buildReturnType(ASTCDMethod command, ASTCDClass ast) {
    ASTMCReturnType retType = command.getMCReturnType();
    if (retType.isPresentMCVoidType()) {
      CD4C.getInstance().addImport(ast, PathUtil.getBackendRTECommandPath(STATUS_RESULT));
      return MCTypeFacade.getInstance().createQualifiedType(STATUS_RESULT);
    }
    else if (retType.isPresentMCType()) {
      ASTMCType mcType = retType.getMCType();

      if (GenService.isList(mcType)) {
        return buildListResult(GenService.getInnerType(mcType), ast);
      }
      if (GenService.isOptional(mcType)) {
        return buildSimpleResult(GenService.getInnerType(mcType), ast);
      }
      return buildSimpleResult(UMLPMill.prettyPrint(mcType, false), ast);
    }
    return buildSimpleResult("Object", ast);
  }

  default ASTMCObjectType buildExtendedCommandType(String typeParam) {
    String type = StringTransformations.capitalize(REQUEST);
    ASTMCType param = MCTypeFacade.getInstance().createQualifiedType(box(typeParam));
    return buildGeneric(type, param);
  }

  default ASTMCObjectType buildSimpleResult(String typeParam, ASTCDClass ast) {
    CD4C.getInstance().addImport(ast, PathUtil.getBackendRTECommandPath(SIMPLE_RESULT));
    String type = StringTransformations.capitalize(SIMPLE_RESULT);
    ASTMCType param = MCTypeFacade.getInstance().createQualifiedType(box(typeParam));
    return buildGeneric(type, param);
  }

  default ASTMCObjectType buildListResult(String typeParam, ASTCDClass ast) {
    CD4C.getInstance().addImport(ast, PathUtil.getBackendRTECommandPath(LIST_RESULT));
    CD4C.getInstance().addImport(ast, "java.util.List");
    String type = StringTransformations.capitalize(LIST_RESULT);
    ASTMCType param = MCTypeFacade.getInstance().createQualifiedType(box(typeParam));
    return buildGeneric(type, param, buildGeneric("List", param));
  }

  default ASTMCObjectType buildGeneric(String type, ASTMCType... params) {
    ASTMCBasicGenericTypeBuilder genTypeBuilder = MCFullGenericTypesMill.mCBasicGenericTypeBuilder()
        .setNamesList(new ArrayList<>(Arrays.asList(type)))
        .setMCTypeArgumentsList(new ArrayList<>());

    for (ASTMCType param : params) {
      genTypeBuilder.addMCTypeArgument(
          MCSimpleGenericTypesMill.mCCustomTypeArgumentBuilder()
              .setMCType(param)
              .build());
    }
    return genTypeBuilder.build();
  }

  default boolean isGem(SymTypeExpression type) {
    return (GenService.isDomainType(type)
               || isOptionalGem(type))
                && !GenService.isCDEnum(type);
  }

  default boolean isOptionalGem(SymTypeExpression type) {
    return GenService.isOptional(type) && GenService.isDomainType(((SymTypeOfGenerics) type).getArgument(0))  && !GenService.isCDOrInnerCDEnum(type);
  }

  default boolean isListOfGemIdentifiable(SymTypeExpression type) {
    return GenService.isList(type) && GenService.isDomainType(((SymTypeOfGenerics) type).getArgument(0))  && !GenService.isCDOrInnerCDEnum(type);
  }

}
