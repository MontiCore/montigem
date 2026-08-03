package umlp.generator.ocl.javagen;

import de.monticore.codegen.javagen.SymTypeExpression2JavaConverter;
import de.monticore.ocl.ocl._ast.*;
import de.monticore.ocl.ocl._visitor.OCLHandler;
import de.monticore.ocl.ocl._visitor.OCLTraverser;
import de.monticore.prettyprint.IndentPrinter;
import de.monticore.types3.TypeCheck3;
import de.se_rwth.commons.logging.Log;

import java.util.List;

public class OCLJavaGenVisitor implements OCLHandler {
  protected OCLTraverser traverser;
  protected IndentPrinter printer;
  protected String fullPackage;

  public OCLJavaGenVisitor(IndentPrinter printer) {
    this.printer = printer;
  }

  public OCLJavaGenVisitor(IndentPrinter printer, String fullPackage) {
    this.printer = printer;
    this.fullPackage = fullPackage;
  }

  @Override
  public OCLTraverser getTraverser() {
    return traverser;
  }

  @Override
  public void setTraverser(OCLTraverser traverser) {
    this.traverser = traverser;
  }

  @Override
  public void traverse(ASTOCLCompilationUnit node) {
    if (fullPackage != null && !fullPackage.isEmpty()) {
      printer.println("package " + fullPackage + ";");
      printer.println();
    } else if (node.isPresentPackage()) {
      printer.println("package " + node.getPackage() + ";");
      printer.println();
    }

    for (String importedType : OCLJavaImportCollector.collect(node)) {
      printer.println("import " + importedType + ";");
    }

    // Default imports
    printer.println("import java.util.List;");
    printer.println("import java.util.Set;");
    printer.println("import java.util.HashSet;");
    printer.println("import java.util.Objects;");
    printer.println("import java.util.Optional;");
    printer.println("import java.util.function.Function;");
    printer.println("import mc.fenix.input.geminputbasetypes.ValidationError;");
    printer.println();

    if (null != node.getOCLArtifact()) {
      node.getOCLArtifact().accept(getTraverser());
    }
  }

  @Override
  public void handle(ASTOCLArtifact node) {
    printer.print("public class ");
    printer.print(node.getName());
    printer.println("{");
    printer.indent();

    OCLHandler.super.handle(node);

    printer.unindent();
    printer.println();
    printer.println("}");
  }

  @Override
  public void handle(ASTOCLParamDeclaration node) {
    printer.print(SymTypeExpression2JavaConverter.getJavaTypePrint(TypeCheck3.symTypeFromAST(node.getMCType())));
    printer.print(" ");
    printer.print(node.getName());

    if(node.isPresentExpression()){
      // TODO
      Log.warn("Expression for OCLParamDeclaration is not yet supported",
          node.getExpression().get_SourcePositionStart(), node.getExpression().get_SourcePositionEnd());
    }
  }
}
