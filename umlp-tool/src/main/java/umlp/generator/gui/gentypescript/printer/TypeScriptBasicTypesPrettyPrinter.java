package umlp.generator.gui.gentypescript.printer;

import de.monticore.prettyprint.IndentPrinter;
import de.monticore.types.mcbasictypes._ast.ASTConstantsMCBasicTypes;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import de.monticore.types.mcbasictypes._ast.ASTMCPrimitiveType;
import de.monticore.types.mcbasictypes._ast.ASTMCQualifiedName;
import de.monticore.types.mcbasictypes._prettyprint.MCBasicTypesPrettyPrinter;
import umlp.generator.gui.GUIGeneratorUtil;

import java.util.List;
import java.util.Optional;

public class TypeScriptBasicTypesPrettyPrinter extends MCBasicTypesPrettyPrinter {

  public TypeScriptBasicTypesPrettyPrinter(IndentPrinter printer) {
    super(printer, false);
  }

  @Override
  public void handle(ASTMCQualifiedName node) {
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPreComments(node, getPrinter());
    }
    // print only last, except for a TypeScript type in a namespace (Temporal.PlainDate)
    if (!node.getPartsList().isEmpty() && TypeScriptTypes.isNamespacedTSType(node.getQName())) {
      getPrinter().print(node.getQName());
    } else if (!node.getPartsList().isEmpty()) {
      getPrinter().print(TypeScriptTypes.toTSType(node.getBaseName()));
      if (TypeScriptTypes.TEMPLATE_REF.getMCType().equals(node.getQName())) {
        // GUIViewElement is TemplateRef<any>
        getPrinter().print("<any>");
      }
    }
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPostComments(node, getPrinter());
    }
  }

  @Override
  public void handle(ASTMCImportStatement node) {
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPreComments(node, getPrinter());
    }
    Optional<String> tsImportPath = TypeScriptTypes.getTsImportPath(node.getQName());
    // Optional doesn't exist in TypeScript
    if (node.getQName().equals("java.util.Optional")) {
      return;
    }
    if (tsImportPath.isPresent()) {
      getPrinter().print("import { " + TypeScriptTypes.toTSType(node.getQName())
          + " } from '" + tsImportPath.get() + "';");
    }
    else if (!TypeScriptTypes.isDefault(node.getQName())) {
      getPrinter().print("import { ");
      node.getMCQualifiedName().accept(getTraverser());
      getPrinter().print(" }");
      getPrinter().stripTrailing();
      getPrinter().print(" from '");

      // generated path doesn't match package for domain classes
      // hardy.Flensburg.Human -> hardy/flensburg/Human
      List<String> parts = node.getMCQualifiedName().getPartsList();
      int index = parts.size() - 2;
      if (index >= 0) {
        parts.set(index, parts.get(index).toLowerCase());
      }
      String from = GUIGeneratorUtil.getInstance().qName2path(String.join(".", parts));
      tsImportPath = TSImportMap.INSTANCE.getTSPackage(node.getQName());
      if (tsImportPath.isPresent()) {
        from = tsImportPath.get();
      }
      getPrinter().print(from);

      getPrinter().println("';");
    }
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPostComments(node, getPrinter());
    }
  }

  @Override
  public void handle(ASTMCPrimitiveType node) {
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPreComments(node, getPrinter());
    }
    if (node.getPrimitive() == ASTConstantsMCBasicTypes.BOOLEAN) {
      getPrinter().print("boolean ");
    }
    else if (node.getPrimitive() == ASTConstantsMCBasicTypes.BYTE
        || node.getPrimitive() == ASTConstantsMCBasicTypes.SHORT
        || node.getPrimitive() == ASTConstantsMCBasicTypes.INT
        || node.getPrimitive() == ASTConstantsMCBasicTypes.LONG
        || node.getPrimitive() == ASTConstantsMCBasicTypes.FLOAT
        || node.getPrimitive() == ASTConstantsMCBasicTypes.DOUBLE
    ) {
      getPrinter().print("number ");
    }
    else if (node.getPrimitive() == ASTConstantsMCBasicTypes.CHAR) {
      getPrinter().print("string ");
    }
    if (this.isPrintComments()) {
      de.monticore.prettyprint.CommentPrettyPrinter.printPostComments(node, getPrinter());
    }
  }
}
