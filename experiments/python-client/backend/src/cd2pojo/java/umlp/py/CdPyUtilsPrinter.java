/* (c) https://github.com/MontiCore/monticore */
package umlp.py;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import de.monticore.cd.codegen.CdUtilsPrinter;
import de.monticore.cd4code._prettyprint.CD4CodeFullPrettyPrinter;
import de.monticore.cd4codebasis._ast.ASTCDParameter;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.TemplateController;
import de.monticore.literals.mccommonliterals._ast.ASTBooleanLiteral;
import de.monticore.literals.mccommonliterals._ast.ASTNullLiteral;
import de.monticore.literals.mccommonliterals._prettyprint.MCCommonLiteralsPrettyPrinter;
import de.monticore.prettyprint.CommentPrettyPrinter;
import de.monticore.prettyprint.IndentPrinter;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import de.monticore.types.mcbasictypes._ast.ASTMCPrimitiveType;
import de.monticore.types.mcbasictypes._ast.ASTMCQualifiedName;
import de.monticore.types.mcbasictypes._ast.ASTMCReturnType;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types.mcbasictypes._ast.ASTMCVoidType;
import de.monticore.types.mcbasictypes._prettyprint.MCBasicTypesPrettyPrinter;
import de.monticore.umlmodifier._ast.ASTModifier;
import umlp.py.printer.PythonMYSimpleGenericTypesPrinter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * AST specific helper to print AST nodes.
 */
public class CdPyUtilsPrinter extends CdUtilsPrinter {

  public static final String NotificationScope = "montigem_runtime.gembase.NotificationScope";

  public static final String EMPTY_STRING = "";

  // Printer with
  protected CD4CodeFullPrettyPrinter fullPrettyPrinter;

  public CdPyUtilsPrinter() {
    fullPrettyPrinter = new CD4CodeFullPrettyPrinter(new IndentPrinter());

    fullPrettyPrinter.getTraverser().setMCSimpleGenericTypesHandler(new PythonMYSimpleGenericTypesPrinter(fullPrettyPrinter.getPrinter(), true));
    fullPrettyPrinter.getTraverser().setMCBasicTypesHandler(new MCBasicTypesPrettyPrinter(fullPrettyPrinter.getPrinter(), true) {
      @Override
      public void handle(ASTMCQualifiedName node) {
        if (this.isPrintComments()) {
          CommentPrettyPrinter.printPreComments(node, this.getPrinter());
        }
        String p = node.toString();
        if (p.equals("String") || p.equals("java.lang.String"))
          p = "str";
        getPrinter().print(p);
        getPrinter().print(" ");
        if (this.isPrintComments()) {
          CommentPrettyPrinter.printPostComments(node, this.getPrinter());
        }
      }

      @Override
      public void handle(ASTMCPrimitiveType node) {
        if (this.isPrintComments()) {
          CommentPrettyPrinter.printPreComments(node, this.getPrinter());
        }

        if (node.getPrimitive() == 1) {
          this.getPrinter().print("bool ");
        } else if (node.getPrimitive() == 2) {
          this.getPrinter().print("byte ");
        } else if (node.getPrimitive() == 8) {
          this.getPrinter().print("short ");
        } else if (node.getPrimitive() == 6) {
          this.getPrinter().print("int ");
        } else if (node.getPrimitive() == 7) {
          this.getPrinter().print("int "); // longs were unified in python 3
        } else if (node.getPrimitive() == 3) {
          this.getPrinter().print("str ");
        } else if (node.getPrimitive() == 5) {
          this.getPrinter().print("float ");
        } else if (node.getPrimitive() == 4) {
          this.getPrinter().print("float ");
        }

        if (this.isPrintComments()) {
          CommentPrettyPrinter.printPostComments(node, this.getPrinter());
        }

      }
    });

    fullPrettyPrinter.getTraverser().setMCCommonLiteralsHandler(new MCCommonLiteralsPrettyPrinter(fullPrettyPrinter.getPrinter(), true) {
      @Override
      public void handle(ASTBooleanLiteral node) {
        if (node.getValue())
          getPrinter().print("True ");
        else
          getPrinter().print("True ");
      }

      @Override
      public void handle(ASTNullLiteral node) {
        getPrinter().print("None ");
      }
    });

  }


  /**
   * Converts a list of import statements to a string list.
   *
   * @param importStatements the list of import statements
   * @return a string list of all import statements
   */
  public String printImportList(Collection<ASTMCImportStatement> importStatements) {
    StringBuilder sb = new StringBuilder();
    // Some built-in imports have to be removed (e.g. import str)
    importStatements.stream().map(i -> fullPrettyPrinter.prettyprint(i))
        .filter(importLine -> !builtIns.contains(importLine))
        .forEach(importLine -> {
          sb.append(importLine).append("\n");
        });
    return "#printImportList\n" + sb.toString().replace("import de.se_rwth.commons.logging.Log", "#import de.se_rwth.commons.logging.Log") + "\n#end printImportList";
  }

  protected List<String> builtIns = Arrays.asList("import str;\n", "import java.util.List;\n", "import java.util.Optional;\n");

  /**
   * Prints an ASTType
   *
   * @param type an ASTType
   * @return String representation of the ASTType
   */
  public String printType(ASTMCType type) {
    return fullPrettyPrinter.prettyprint(type);
  }

  public String printType(ASTMCReturnType type) {
    return fullPrettyPrinter.prettyprint(type);
  }

  public String printExpression(ASTExpression expr) {
    return fullPrettyPrinter.prettyprint(expr);
  }

  public String unwrapIndentation(String input) {
    StringBuilder sb = new StringBuilder();

    int lvl = 0;
    for (String line : input.split("\n")) {
      if (line.strip().equals("[[[indent]]]")) {
        lvl++;
      } else if (line.strip().equals("[[[deindent]]]")) {
        lvl--;
      } else {
        sb.append(Strings.repeat(" ", lvl));
        sb.append(line.strip()).append("\n");
      }
    }

    return sb.toString();
  }

  @Override
  public String printCDParametersDecl(List<ASTCDParameter> parameterList) {
    throw new IllegalStateException("Missing tc arg!");
  }

  public String printCDParametersDecl(List<ASTCDParameter> parameterList, TemplateController tc, GlobalExtensionManagement glex) {
    checkNotNull(parameterList);
    return Joiner.on(",")
        .join(
            Collections2.transform(
                parameterList,
                cdParam ->
                    cdParam.getAnnotation().map(a -> a + " ").orElse("")
                        + cdParam.getName()
                        + ": "
                        + printType(cdParam.getMCType()) + " " + glex.defineHookPoint(tc, "PyParam::Initial", cdParam)
            ));
  }

  public boolean isVoidType(ASTMCType type) {
    return type instanceof ASTMCVoidType;
  }

  public boolean isVoidType(ASTMCReturnType type) {
    return type.isPresentMCVoidType();
  }

  public boolean hasStereotype(ASTModifier modifier, String name) {
    if (!modifier.isPresentStereotype()) return false;
    return modifier.getStereotype().contains(name);
  }

  public boolean isAsync(ASTModifier modifier) {
    return hasStereotype(modifier, "py__async");
  }
}
