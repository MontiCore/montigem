package umlp.generator.ocl.javagen;

import de.monticore.codegen.CodeGenVisitorState;
import de.monticore.codegen.javagen.JavaGenSymTypeExpressionConverter;
import de.monticore.codegen.javagen.JavaGenVisitorState;
import de.monticore.expressions.bitexpressions.codegen.javagen.BitExpressionsJavaGenVisitor;
import de.monticore.expressions.commonexpressions.codegen.javagen.CommonExpressionsJavaGenVisitor;
import de.monticore.expressions.expressionsbasis.codegen.javagen.ExpressionsBasisJavaGenVisitor;
import de.monticore.literals.mccommonliterals.codegen.javagen.MCCommonLiteralsJavaGenVisitor;
import de.monticore.ocl.oclexpressions.codegen.javagen.OCLExpressionsJavaGenVisitor;
import de.monticore.ocl.optionaloperators.codegen.javagen.OptionalOperatorsJavaGenVisitor;
import de.monticore.ocl.setexpressions.codegen.javagen.SetExpressionsJavaGenVisitor;
import de.monticore.prettyprint.IndentPrinter;
import de.monticore.visitor.ITraverser;
import oclwitherrormessages.OCLWithErrorMessagesMill;
import oclwitherrormessages._visitor.OCLWithErrorMessagesTraverser;

public class OCL2UMLPJavaCodeGenerator extends CodeGenVisitorState {
  OCLWithErrorMessagesTraverser traverser;
  protected String fullPackage;

  public OCL2UMLPJavaCodeGenerator(IndentPrinter printer) {
    super(printer);
    init();
  }

  public OCL2UMLPJavaCodeGenerator(IndentPrinter printer, String fullPackage) {
    super(printer);
    this.fullPackage = fullPackage;
    init();
  }

  public void init() {
    traverser = OCLWithErrorMessagesMill.traverser();
    OCLTypeNarrowingContext typeNarrowingContext = new OCLTypeNarrowingContext();

    JavaGenSymTypeExpressionConverter.init();
    SymTypeExpression2UMLPJavaConverter.init();
    OCL2JavaOperationPrinter.init();

    // Literals

    JavaGenVisitorState state = new JavaGenVisitorState(printer);
    MCCommonLiteralsJavaGenVisitor visMCCommonLiterals = new MCCommonLiteralsJavaGenVisitor(state);
    traverser.setMCCommonLiteralsHandler(visMCCommonLiterals);

    // Expressions

    BitExpressionsJavaGenVisitor visBitExpressions = new BitExpressionsJavaGenVisitor(state);
    traverser.setBitExpressionsHandler(visBitExpressions);

    CommonExpressionsJavaGenVisitor visCommonExpressions = new CommonExpressionsOCLJavaGenVisitor(state);
    traverser.setCommonExpressionsHandler(visCommonExpressions);

    ExpressionsBasisJavaGenVisitor visExpressionBasis = new OCLTypeNarrowingNameExpressionJavaGenVisitor(state, typeNarrowingContext);
    traverser.setExpressionsBasisHandler(visExpressionBasis);

    OCLExpressionsJavaGenVisitor visOCLExpressions = new OCLExpressionsUMLPJavaGenVisitor(state, typeNarrowingContext);
    traverser.setOCLExpressionsHandler(visOCLExpressions);

    OptionalOperatorsJavaGenVisitor visOptionalOperators = new OptionalOperatorsJavaGenVisitor(state);
    traverser.setOptionalOperatorsHandler(visOptionalOperators);

    SetExpressionsJavaGenVisitor visSetExpressions = new SetExpressionsJavaGenVisitor(state);
    traverser.setSetExpressionsHandler(visSetExpressions);

    // OCL
    OCLJavaGenVisitor oclJavaGenVisitor = new OCLJavaGenVisitor(getPrinter(), fullPackage);
    traverser.setOCLHandler(oclJavaGenVisitor);

    // OCLWithErrorMessages
    OCLWithErrorMessagesJavaGenVisitor oclWithErrorMessagesJavaGenVisitor = new OCLWithErrorMessagesJavaGenVisitor(state.getPrinter());
    traverser.setOCLWithErrorMessagesHandler(oclWithErrorMessagesJavaGenVisitor);
  }

  public ITraverser getTraverser() {
    return traverser;
  }
}
