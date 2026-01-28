package expressionwrapper;

import de.monticore.class2mc.OOClass2MCResolver;
import de.monticore.expressions.assignmentexpressions._cocos.AssignmentExpressionsASTAssignmentExpressionCoCo;
import de.monticore.expressions.assignmentexpressions.cocos.AssignmentExpressionsOnlyAssignToLValuesCoCo;
import de.monticore.expressions.assignmentexpressions.interpreter.AssignmentExpressionsInterpreter;
import de.monticore.expressions.cocos.ExpressionValid;
import de.monticore.expressions.commonexpressions.interpreter.CommonExpressionsInterpreter;
import de.monticore.expressions.expressionsbasis.interpreter.ExpressionsBasisInterpreter;
import de.monticore.expressions.lambdaexpressions._symboltable.LambdaExpressionsSTCompleteTypes2;
import de.monticore.expressions.lambdaexpressions.interpreter.LambdaExpressionsInterpreter;
import de.monticore.interpreter.calculations.MICalculation;
import de.monticore.interpreter.signals.MCSignalReturn;
import de.monticore.interpreter.util.InterpreterDataForBasicSymbols;
import de.monticore.literals.mccommonliterals.interpreter.MCCommonLiteralsInterpreter;
import de.monticore.ocl.setexpressions.cocos.SetComprehensionHasGenerator;
import de.monticore.ocl.setexpressions.interpreter.SetExpressionsInterpreter;
import de.monticore.ocl.setexpressions.symboltable.SetExpressionsSymbolTableCompleter;
import de.monticore.statements.mccommonstatements._symboltable.MCCommonStatementsSymTabCompletion;
import de.monticore.statements.mccommonstatements.cocos.DoWhileConditionHasBooleanType;
import de.monticore.statements.mccommonstatements.cocos.ExpressionStatementIsValid;
import de.monticore.statements.mccommonstatements.cocos.ForConditionHasBooleanType;
import de.monticore.statements.mccommonstatements.cocos.ForEachIsValid;
import de.monticore.statements.mccommonstatements.cocos.IfConditionHasBooleanType;
import de.monticore.statements.mccommonstatements.cocos.SwitchStatementValid;
import de.monticore.statements.mccommonstatements.cocos.WhileConditionHasBooleanType;
import de.monticore.statements.mccommonstatements.interpreter.MCCommonStatementsInterpreter;
import de.monticore.statements.mcreturnstatements.interpreter.MCReturnStatementsInterpreter;
import de.monticore.statements.mcvardeclarationstatements._cocos.VarDeclarationInitializationHasCorrectType;
import de.monticore.statements.mcvardeclarationstatements._cocos.VarDeclarationNameAlreadyDefinedInScope;
import de.monticore.statements.mcvardeclarationstatements._symboltable.MCVarDeclarationStatementsSymTabCompletion;
import de.monticore.statements.mcvardeclarationstatements.interpreter.MCVarDeclarationStatementsInterpreter;
import de.monticore.symbols.basicsymbols.BasicSymbolsMill;
import de.monticore.symbols.oosymbols.OOSymbolsMill;
import de.monticore.symbols.oosymbols._symboltable.IOOSymbolsGlobalScope;
import de.monticore.symboltable.ImportStatement;
import de.monticore.types.mcbasictypes.cocos.QualifiedTypeHasNoTypeParameters;
import de.monticore.values.MCValue;
import de.se_rwth.commons.logging.Log;
import expressionwrapper._ast.ASTInterpreterCompilationUnit;
import expressionwrapper._cocos.ExpressionWrapperCoCoChecker;
import expressionwrapper._symboltable.IExpressionWrapperArtifactScope;
import expressionwrapper._visitor.ExpressionWrapperTraverser;
import expressionwrapper.interpreter.ExpressionsWrapperInterpreter;
import mc.fenix.charts.gempiecharttypes.GemPieChartData;
import mc.fenix.charts.gempiecharttypes.GemPieChartDataBuilder;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntry;
import mc.fenix.charts.gempiecharttypes.GemPieChartEntryBuilder;
import mc.fenix.charts.gemlinecharttypes.GemLineChartData;
import mc.fenix.charts.gemlinecharttypes.GemLineChartDataBuilder;
import mc.fenix.charts.gemlinecharttypes.GemLineChartEntry;
import mc.fenix.charts.gemlinecharttypes.GemLineChartEntryBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotAxis;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotAxisBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotData;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotDataBuilder;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotPoint;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotSet;
import mc.fenix.charts.gemscatterplottypes.GemScatterPlotSetBuilder;
import mc.fenix.networkgraph.networkgraphtypes.Graph;
import mc.fenix.networkgraph.networkgraphtypes.GraphBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdge;
import mc.fenix.networkgraph.networkgraphtypes.GraphEdgeBuilder;
import mc.fenix.networkgraph.networkgraphtypes.GraphNode;
import mc.fenix.networkgraph.networkgraphtypes.GraphNodeBuilder;
import mc.fenix.charts.gembarcharttypes.GemBarChartData;
import mc.fenix.charts.gembarcharttypes.GemBarChartDataBuilder;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntry;
import mc.fenix.charts.gembarcharttypes.GemBarChartEntryBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotBox;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotBoxBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotData;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotDataBuilder;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotEntry;
import mc.fenix.charts.gemboxplottypes.GemBoxPlotEntryBuilder;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartData;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartDataBuilder;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartEntry;
import mc.fenix.charts.gemgaugecharttypes.GemGaugeChartEntryBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Interpreter {

  protected List<ImportStatement> defaultImportStatements = new ArrayList<>();
  protected IExpressionWrapperArtifactScope artifactScope;
  protected AnotherInterpreter interpreter;
  public Interpreter() {}

  public MCValue interpret(String modelStr) throws InterpreterFailedException {
    try {
      ASTInterpreterCompilationUnit ast = getPreparedAST(modelStr);
      // explicitly get the calculation to check if there are errors

      MICalculation calculation = getCalculation(ast); // TODO this is probably not necessary
      calculation.isCalculationVoid();
      MCValue value = interpreter.interpretNode(ast);
      if(value.asNativeObject() instanceof MCSignalReturn signalReturn) {
        if(signalReturn.getValue().isPresent()) {
          return signalReturn.getValue().get();
        }
      }
      return value;
    } catch (Exception e) {
      // Logging has to be captured somewhere else
      throw new InterpreterFailedException(e);
    }
  }


  ASTInterpreterCompilationUnit getPreparedAST(String modelStr) {
    ASTInterpreterCompilationUnit ast = this.parse(modelStr);
    runSymTabGenitor(ast);
    runSymTabCompleter(ast);
    runContextConditionChecks(ast);
    return ast;
  }

  MICalculation getCalculation(ASTInterpreterCompilationUnit ast) {
    MICalculation calculation = interpreter.getCalculation(ast);
    return calculation;
  }

  public void setup() {
    Log.enableFailQuick(false);
    // setup, done once (simplified)
    InterpreterDataForBasicSymbols iData = new InterpreterDataForBasicSymbols();
    ExpressionWrapperMill.init();
    ExpressionWrapperMill.globalScope().clear();
    BasicSymbolsMill.initializePrimitives();

    artifactScope = ExpressionWrapperMill.artifactScope();
    artifactScope.setName("");
    ExpressionWrapperMill.globalScope().addSubScope(artifactScope);

    // TypeCheck
    expressionswrapper.TypeCheck.init();
    // Class2MC
    IOOSymbolsGlobalScope globalScope = OOSymbolsMill.globalScope();
    OOClass2MCResolver resolver = new OOClass2MCResolver();
    globalScope.addAdaptedOOTypeSymbolResolver(resolver);
    globalScope.addAdaptedTypeSymbolResolver(resolver);
    addAvailableClassesToGlobalScope();

    ExpressionWrapperTraverser traverser = ExpressionWrapperMill.inheritanceTraverser();
    traverser.setExpressionsBasisHandler(new ExpressionsBasisInterpreter(iData));
    traverser.setCommonExpressionsHandler(new CommonExpressionsInterpreter(iData));
    traverser.setAssignmentExpressionsHandler(new AssignmentExpressionsInterpreter(iData));
    traverser.setLambdaExpressionsHandler(new LambdaExpressionsInterpreter(iData));
    traverser.setMCCommonLiteralsHandler(new MCCommonLiteralsInterpreter(iData));
    traverser.setSetExpressionsHandler(new SetExpressionsInterpreter(iData));
    traverser.setMCCommonStatementsHandler(new MCCommonStatementsInterpreter(iData));
    traverser.setMCReturnStatementsHandler(new MCReturnStatementsInterpreter(iData));
    traverser.setMCVarDeclarationStatementsHandler(new MCVarDeclarationStatementsInterpreter(iData));
    traverser.setExpressionWrapperHandler(new ExpressionsWrapperInterpreter(iData));

    interpreter = new AnotherInterpreter(traverser, iData);
  }

  protected void addAvailableClassesToGlobalScope() {
    addClassPathEntry(GemPieChartData.class);
    addClassPathEntry(GemPieChartDataBuilder.class);
    addClassPathEntry(GemPieChartEntry.class);
    addClassPathEntry(GemPieChartEntryBuilder.class);
    addClassPathEntry(GemLineChartData.class);
    addClassPathEntry(GemLineChartDataBuilder.class);
    addClassPathEntry(GemLineChartEntry.class);
    addClassPathEntry(GemLineChartEntryBuilder.class);
    addClassPathEntry(Graph.class);
    addClassPathEntry(GraphBuilder.class);
    addClassPathEntry(GraphEdge.class);
    addClassPathEntry(GraphEdgeBuilder.class);
    addClassPathEntry(GraphNode.class);
    addClassPathEntry(GraphNodeBuilder.class);
    addClassPathEntry(GemBarChartData.class);
    addClassPathEntry(GemBarChartDataBuilder.class);
    addClassPathEntry(GemBarChartEntry.class);
    addClassPathEntry(GemBarChartEntryBuilder.class);
    addClassPathEntry(GemBoxPlotBox.class);
    addClassPathEntry(GemBoxPlotBoxBuilder.class);
    addClassPathEntry(GemBoxPlotData.class);
    addClassPathEntry(GemBoxPlotDataBuilder.class);
    addClassPathEntry(GemBoxPlotEntry.class);
    addClassPathEntry(GemBoxPlotEntryBuilder.class);
    addClassPathEntry(GemGaugeChartData.class);
    addClassPathEntry(GemGaugeChartDataBuilder.class);
    addClassPathEntry(GemGaugeChartEntry.class);
    addClassPathEntry(GemGaugeChartEntryBuilder.class);
    addClassPathEntry(GemScatterPlotAxis.class);
    addClassPathEntry(GemScatterPlotAxisBuilder.class);
    addClassPathEntry(GemScatterPlotData.class);
    addClassPathEntry(GemScatterPlotDataBuilder.class);
    addClassPathEntry(GemScatterPlotPoint.class);
    addClassPathEntry(GemScatterPlotSet.class);
    addClassPathEntry(GemScatterPlotSetBuilder.class);
  }

  public void addClassPathEntry(Class<?> clazz) {
    try {
      CodeSource codeSource = clazz
          .getProtectionDomain()
          .getCodeSource();
      if (codeSource == null) {
        return;
      }
      Path classPath = Paths.get(codeSource.getLocation().toURI());
      BasicSymbolsMill.globalScope().getSymbolPath().addEntry(classPath);
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public ASTInterpreterCompilationUnit parse(String input) {
    Optional<ASTInterpreterCompilationUnit> astOpt = Optional.empty();
    try {
      astOpt = ExpressionWrapperMill.parser().parse_String(input);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return astOpt.get();
  }

  protected IExpressionWrapperArtifactScope runSymTabGenitor(ASTInterpreterCompilationUnit ast) {
    IExpressionWrapperArtifactScope scope =
        ExpressionWrapperMill.scopesGenitorDelegator().createFromAST(ast);
    // default import
    scope.addImports(new ImportStatement("java.lang", true));
    scope.addImports(new ImportStatement("java.util", true));
    scope.addImports(new ImportStatement("mc.fenix.charts.gempiecharttypes", true));
    scope.addImports(new ImportStatement("mc.fenix.charts.gemlinecharttypes", true));
    scope.addImports(new ImportStatement("mc.fenix.networkgraph.networkgraphtypes", true));
    scope.addImports(new ImportStatement("mc.fenix.charts.gemscatterplottypes", true));
    scope.addImports(new ImportStatement("mc.fenix.charts.gemboxplottypes", true));
    scope.addImports(new ImportStatement("mc.fenix.charts.gemgaugecharttypes", true));
    scope.addImports(new ImportStatement("mc.fenix.charts.gembarcharttypes", true));

    defaultImportStatements.forEach(s -> scope.addImports(s));
    return scope;
  }

  public void addDefaultImportStatement(ImportStatement importStatement) {
    this.defaultImportStatements.add(importStatement);
  }

  protected void runSymTabCompleter(ASTInterpreterCompilationUnit ast) {
    ExpressionWrapperTraverser symTabCompleter = ExpressionWrapperMill.inheritanceTraverser();

    // Expressions

    SetExpressionsSymbolTableCompleter setExprCompleter =
        new SetExpressionsSymbolTableCompleter();
    symTabCompleter.setSetExpressionsHandler(setExprCompleter);
    symTabCompleter.add4BasicSymbols(setExprCompleter);
    symTabCompleter.add4SetExpressions(setExprCompleter);

    LambdaExpressionsSTCompleteTypes2 lambdaExpressionsSTCompleteTypes2 =
        new LambdaExpressionsSTCompleteTypes2();
    symTabCompleter.add4LambdaExpressions(lambdaExpressionsSTCompleteTypes2);

    // Statements

    MCCommonStatementsSymTabCompletion commonStatementsCompleter =
        new MCCommonStatementsSymTabCompletion();
    symTabCompleter.add4MCCommonStatements(commonStatementsCompleter);

    MCVarDeclarationStatementsSymTabCompletion mcVarDeclarationStatementsCompleter =
        new MCVarDeclarationStatementsSymTabCompletion();
    symTabCompleter.add4MCVarDeclarationStatements(mcVarDeclarationStatementsCompleter);

    ast.accept(symTabCompleter);
  }

  protected void runContextConditionChecks(ASTInterpreterCompilationUnit ast) {
    ExpressionWrapperCoCoChecker checker = new ExpressionWrapperCoCoChecker();
    checker.addCoCo(new DoWhileConditionHasBooleanType());
    checker.addCoCo(new ExpressionStatementIsValid());
    checker.addCoCo(new ForConditionHasBooleanType());
    checker.addCoCo(new ForEachIsValid());
    checker.addCoCo(new IfConditionHasBooleanType());
    checker.addCoCo(new SwitchStatementValid());
    checker.addCoCo(new WhileConditionHasBooleanType());
    checker.addCoCo(new ExpressionValid());
    checker.addCoCo(new VarDeclarationInitializationHasCorrectType());
    checker.addCoCo(new VarDeclarationNameAlreadyDefinedInScope());
    checker.addCoCo((AssignmentExpressionsASTAssignmentExpressionCoCo) new AssignmentExpressionsOnlyAssignToLValuesCoCo());
    checker.addCoCo(new SetComprehensionHasGenerator());
    checker.addCoCo(new QualifiedTypeHasNoTypeParameters());
    checker.checkAll(ast);
  }


}
