package umlp.backendrte.ocl;

import de.monticore.expressions.commonexpressions._prettyprint.CommonExpressionsPrettyPrinter;
import de.monticore.literals.mccommonliterals._prettyprint.MCCommonLiteralsPrettyPrinter;
import de.monticore.ocl.ocl._prettyprint.OCLFullPrettyPrinter;
import de.monticore.prettyprint.IndentPrinter;

public class CommonExpressions2SQLFullPrinter extends OCLFullPrettyPrinter {

  public CommonExpressions2SQLFullPrinter(IndentPrinter printer, boolean printComments) {
    super(printer, printComments);
  }

  @Override
  protected void initializeTraverser(IndentPrinter printer, boolean printComments) {
    super.initializeTraverser(printer, printComments);

    // replace pretty printer via custom one
    CommonExpressionsPrettyPrinter commonExpressions = new CommonExpressions2SQLPrinter(printer, printComments);
    getTraverser().setCommonExpressionsHandler(commonExpressions);
    getTraverser().getCommonExpressionsVisitorList().clear();
    getTraverser().add4CommonExpressions(commonExpressions);

    MCCommonLiteralsPrettyPrinter mCCommonLiterals = new CommonLiterals2SQLPrinter(printer, printComments);
    getTraverser().setMCCommonLiteralsHandler(mCCommonLiterals);
    getTraverser().getMCCommonLiteralsVisitorList().clear();
    getTraverser().add4MCCommonLiterals(mCCommonLiterals);
  }

}
