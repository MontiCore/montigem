/* (c) https://github.com/MontiCore/monticore */
package expressionswrapper;

import de.monticore.expressions.assignmentexpressions.types3.AssignmentExpressionsCTTIVisitor;
import de.monticore.expressions.commonexpressions.types3.CommonExpressionsCTTIVisitor;
import de.monticore.expressions.commonexpressions.types3.util.CommonExpressionsLValueRelations;
import de.monticore.expressions.expressionsbasis.types3.ExpressionBasisCTTIVisitor;
import de.monticore.expressions.lambdaexpressions.types3.LambdaExpressionsTypeVisitor;
import de.monticore.literals.mccommonliterals.types3.MCCommonLiteralsTypeVisitor;
import de.monticore.ocl.setexpressions.types3.SetExpressionsCTTIVisitor;
import de.monticore.types.mcarraytypes.types3.MCArrayTypesTypeVisitor;
import de.monticore.types.mcbasictypes.types3.MCBasicTypesTypeVisitor;
import de.monticore.types.mccollectiontypes.types3.MCCollectionSymTypeRelations;
import de.monticore.types.mccollectiontypes.types3.MCCollectionTypesTypeVisitor;
import de.monticore.types.mcfullgenerictypes.types3.MCFullGenericTypesTypeVisitor;
import de.monticore.types.mcfunctiontypes.types3.MCFunctionTypesTypeVisitor;
import de.monticore.types.mcsimplegenerictypes.types3.MCSimpleGenericTypesTypeVisitor;
import de.monticore.types3.SymTypeRelations;
import de.monticore.types3.Type4Ast;
import de.monticore.types3.TypeCheck3;
import de.monticore.types3.generics.TypeParameterRelations;
import de.monticore.types3.generics.context.InferenceContext4Ast;
import de.monticore.types3.util.FunctionRelations;
import de.monticore.types3.util.MapBasedTypeCheck3;
import de.monticore.types3.util.OOWithinScopeBasicSymbolsResolver;
import de.monticore.types3.util.OOWithinTypeBasicSymbolsResolver;
import de.monticore.types3.util.TypeContextCalculator;
import de.monticore.types3.util.TypeVisitorOperatorCalculator;
import de.monticore.visitor.ITraverser;
import de.se_rwth.commons.logging.Log;
import expressionwrapper.ExpressionWrapperMill;
import expressionwrapper._visitor.ExpressionWrapperTraverser;

public class TypeCheck extends MapBasedTypeCheck3 {

  public static void init() {
    Log.trace(
        "init " + TypeCheck.class.getSimpleName(),
        "TypeCheck setup"
    );

    SymTypeRelations.init();
    MCCollectionSymTypeRelations.init();
    OOWithinTypeBasicSymbolsResolver.init();
    OOWithinScopeBasicSymbolsResolver.init();
    TypeContextCalculator.init();
    TypeVisitorOperatorCalculator.init();
    CommonExpressionsLValueRelations.init();
    TypeParameterRelations.init();
    FunctionRelations.init();

    ExpressionWrapperTraverser traverser =
        ExpressionWrapperMill.inheritanceTraverser();
    Type4Ast type4Ast = new Type4Ast();
    InferenceContext4Ast ctx4Ast = new InferenceContext4Ast();

    // Literals

    MCCommonLiteralsTypeVisitor visMCCommonLiterals = new MCCommonLiteralsTypeVisitor();
    visMCCommonLiterals.setType4Ast(type4Ast);
    traverser.add4MCCommonLiterals(visMCCommonLiterals);

    // Expressions

    AssignmentExpressionsCTTIVisitor visAssignmentExpressions = new AssignmentExpressionsCTTIVisitor();
    visAssignmentExpressions.setType4Ast(type4Ast);
    visAssignmentExpressions.setContext4Ast(ctx4Ast);
    traverser.add4AssignmentExpressions(visAssignmentExpressions);
    traverser.setAssignmentExpressionsHandler(visAssignmentExpressions);

    CommonExpressionsCTTIVisitor visCommonExpressions =
        new CommonExpressionsCTTIVisitor();
    visCommonExpressions.setType4Ast(type4Ast);
    visCommonExpressions.setContext4Ast(ctx4Ast);
    traverser.add4CommonExpressions(visCommonExpressions);
    traverser.setCommonExpressionsHandler(visCommonExpressions);

    ExpressionBasisCTTIVisitor visExpressionBasis =
        new ExpressionBasisCTTIVisitor();
    visExpressionBasis.setType4Ast(type4Ast);
    visExpressionBasis.setContext4Ast(ctx4Ast);
    traverser.add4ExpressionsBasis(visExpressionBasis);
    traverser.setExpressionsBasisHandler(visExpressionBasis);

    SetExpressionsCTTIVisitor visSetExpressions = new SetExpressionsCTTIVisitor();
    visSetExpressions.setType4Ast(type4Ast);
    visSetExpressions.setContext4Ast(ctx4Ast);
    traverser.add4SetExpressions(visSetExpressions);
    traverser.setSetExpressionsHandler(visSetExpressions);

    LambdaExpressionsTypeVisitor visLambdaExpressions = new LambdaExpressionsTypeVisitor();
    visLambdaExpressions.setType4Ast(type4Ast);
    traverser.add4LambdaExpressions(visLambdaExpressions);

    // MCTypes

    MCFunctionTypesTypeVisitor visMCFunctionTypes = new MCFunctionTypesTypeVisitor();
    visMCFunctionTypes.setType4Ast(type4Ast);
    traverser.add4MCFunctionTypes(visMCFunctionTypes);

    MCBasicTypesTypeVisitor visMCBasicTypes = new MCBasicTypesTypeVisitor();
    visMCBasicTypes.setType4Ast(type4Ast);
    traverser.add4MCBasicTypes(visMCBasicTypes);

    MCCollectionTypesTypeVisitor visMCCollectionTypes = new MCCollectionTypesTypeVisitor();
    visMCCollectionTypes.setType4Ast(type4Ast);
    traverser.add4MCCollectionTypes(visMCCollectionTypes);

    MCSimpleGenericTypesTypeVisitor visMCSimpleGenericTypes = new MCSimpleGenericTypesTypeVisitor();
    visMCSimpleGenericTypes.setType4Ast(type4Ast);
    traverser.add4MCSimpleGenericTypes(visMCSimpleGenericTypes);

    MCFullGenericTypesTypeVisitor visMCFullGenericTypes = new MCFullGenericTypesTypeVisitor();
    visMCFullGenericTypes.setType4Ast(type4Ast);
    traverser.add4MCFullGenericTypes(visMCFullGenericTypes);

    MCArrayTypesTypeVisitor array = new MCArrayTypesTypeVisitor();
    array.setType4Ast(type4Ast);
    traverser.add4MCArrayTypes(array);

    // create delegate
    TypeCheck oclTC3 = new TypeCheck(traverser, type4Ast, ctx4Ast);
    oclTC3.setThisAsDelegate();
  }

  public static void reset() {
    TypeCheck3.resetDelegate();
    SymTypeRelations.reset();
    MCCollectionSymTypeRelations.reset();
    OOWithinTypeBasicSymbolsResolver.reset();
    OOWithinScopeBasicSymbolsResolver.reset();
    TypeContextCalculator.reset();
    TypeVisitorOperatorCalculator.reset();
    CommonExpressionsLValueRelations.reset();
    TypeParameterRelations.reset();
    FunctionRelations.reset();
  }

  protected TypeCheck(
      ITraverser typeTraverser, Type4Ast type4Ast, InferenceContext4Ast ctx4Ast) {
    super(typeTraverser, type4Ast, ctx4Ast);
  }

}
