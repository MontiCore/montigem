package umlp.generator.gui.gentypescript.printer;

import de.monticore.cd4code._prettyprint.CD4CodeFullPrettyPrinter;
import de.monticore.cd4code._prettyprint.CD4CodePrettyPrinter;

public class TypeScriptTypesFullPrettyPrinter extends CD4CodeFullPrettyPrinter {

  boolean asyncList;

  public TypeScriptTypesFullPrettyPrinter(de.monticore.prettyprint.IndentPrinter printer, boolean asyncList) {
    super(printer, false);
    this.asyncList = asyncList;

    // Otherwise asyncList is always false!
    this.initializeTraverser(printer, false);
  }

  @Override
  protected void initializeTraverser(de.monticore.prettyprint.IndentPrinter printer, boolean printComments) {
    CD4CodePrettyPrinter cD4Code = new CD4CodePrettyPrinter(getPrinter(), printComments);
    getTraverser().setCD4CodeHandler(cD4Code);
    getTraverser().add4CD4Code(cD4Code);

    de.monticore.cd4analysis._prettyprint.CD4AnalysisPrettyPrinter cD4Analysis = new de.monticore.cd4analysis._prettyprint.CD4AnalysisPrettyPrinter(getPrinter(), printComments);
    getTraverser().setCD4AnalysisHandler(cD4Analysis);
    getTraverser().add4CD4Analysis(cD4Analysis);
    de.monticore.cd4codebasis._prettyprint.CD4CodeBasisPrettyPrinter cD4CodeBasis = new de.monticore.cd4codebasis._prettyprint.CD4CodeBasisPrettyPrinter(getPrinter(), printComments);
    getTraverser().setCD4CodeBasisHandler(cD4CodeBasis);
    getTraverser().add4CD4CodeBasis(cD4CodeBasis);
    de.monticore.cdinterfaceandenum._prettyprint.CDInterfaceAndEnumPrettyPrinter cDInterfaceAndEnum = new de.monticore.cdinterfaceandenum._prettyprint.CDInterfaceAndEnumPrettyPrinter(getPrinter(), printComments);
    getTraverser().setCDInterfaceAndEnumHandler(cDInterfaceAndEnum);
    getTraverser().add4CDInterfaceAndEnum(cDInterfaceAndEnum);
    de.monticore.cdassociation._prettyprint.CDAssociationPrettyPrinter cDAssociation = new de.monticore.cdassociation._prettyprint.CDAssociationPrettyPrinter(getPrinter(), printComments);
    getTraverser().setCDAssociationHandler(cDAssociation);
    getTraverser().add4CDAssociation(cDAssociation);
    de.monticore.types.mcarraytypes._prettyprint.MCArrayTypesPrettyPrinter mCArrayTypes = new de.monticore.types.mcarraytypes._prettyprint.MCArrayTypesPrettyPrinter(getPrinter(), printComments);
    getTraverser().setMCArrayTypesHandler(mCArrayTypes);
    getTraverser().add4MCArrayTypes(mCArrayTypes);
    de.monticore.literals.mccommonliterals._prettyprint.MCCommonLiteralsPrettyPrinter mCCommonLiterals = new de.monticore.literals.mccommonliterals._prettyprint.MCCommonLiteralsPrettyPrinter(getPrinter(), printComments);
    getTraverser().setMCCommonLiteralsHandler(mCCommonLiterals);
    getTraverser().add4MCCommonLiterals(mCCommonLiterals);
    de.monticore.expressions.bitexpressions._prettyprint.BitExpressionsPrettyPrinter bitExpressions = new de.monticore.expressions.bitexpressions._prettyprint.BitExpressionsPrettyPrinter(getPrinter(), printComments);
    getTraverser().setBitExpressionsHandler(bitExpressions);
    getTraverser().add4BitExpressions(bitExpressions);
    de.monticore.expressions.commonexpressions._prettyprint.CommonExpressionsPrettyPrinter commonExpressions = new de.monticore.expressions.commonexpressions._prettyprint.CommonExpressionsPrettyPrinter(getPrinter(), printComments);
    getTraverser().setCommonExpressionsHandler(commonExpressions);
    getTraverser().add4CommonExpressions(commonExpressions);
    de.monticore.cdbasis._prettyprint.CDBasisPrettyPrinter cDBasis = new de.monticore.cdbasis._prettyprint.CDBasisPrettyPrinter(getPrinter(), printComments);
    getTraverser().setCDBasisHandler(cDBasis);
    getTraverser().add4CDBasis(cDBasis);
    de.monticore.literals.mcliteralsbasis._prettyprint.MCLiteralsBasisPrettyPrinter mCLiteralsBasis = new de.monticore.literals.mcliteralsbasis._prettyprint.MCLiteralsBasisPrettyPrinter(getPrinter(), printComments);
    getTraverser().setMCLiteralsBasisHandler(mCLiteralsBasis);
    getTraverser().add4MCLiteralsBasis(mCLiteralsBasis);
    de.monticore.symbols.oosymbols._prettyprint.OOSymbolsPrettyPrinter oOSymbols = new de.monticore.symbols.oosymbols._prettyprint.OOSymbolsPrettyPrinter(getPrinter(), printComments);
    getTraverser().setOOSymbolsHandler(oOSymbols);
    getTraverser().add4OOSymbols(oOSymbols);
    de.monticore.umlstereotype._prettyprint.UMLStereotypePrettyPrinter uMLStereotype = new de.monticore.umlstereotype._prettyprint.UMLStereotypePrettyPrinter(getPrinter(), printComments);
    getTraverser().setUMLStereotypeHandler(uMLStereotype);
    getTraverser().add4UMLStereotype(uMLStereotype);
    de.monticore.umlmodifier._prettyprint.UMLModifierPrettyPrinter uMLModifier = new de.monticore.umlmodifier._prettyprint.UMLModifierPrettyPrinter(getPrinter(), printComments);
    getTraverser().setUMLModifierHandler(uMLModifier);
    getTraverser().add4UMLModifier(uMLModifier);
    de.monticore.mcbasics._prettyprint.MCBasicsPrettyPrinter mCBasics = new de.monticore.mcbasics._prettyprint.MCBasicsPrettyPrinter(getPrinter(), printComments);
    getTraverser().setMCBasicsHandler(mCBasics);
    getTraverser().add4MCBasics(mCBasics);
    de.monticore.symbols.basicsymbols._prettyprint.BasicSymbolsPrettyPrinter basicSymbols = new de.monticore.symbols.basicsymbols._prettyprint.BasicSymbolsPrettyPrinter(getPrinter(), printComments);
    getTraverser().setBasicSymbolsHandler(basicSymbols);
    getTraverser().add4BasicSymbols(basicSymbols);

    de.monticore.types.mcfullgenerictypes._prettyprint.MCFullGenericTypesPrettyPrinter mCFullGenericTypes =
        new TypeScriptFullGenericTypesPrettyPrinter(getPrinter());
    getTraverser().setMCFullGenericTypesHandler(mCFullGenericTypes);
    getTraverser().add4MCFullGenericTypes(mCFullGenericTypes);
    de.monticore.types.mcsimplegenerictypes._prettyprint.MCSimpleGenericTypesPrettyPrinter mCSimpleGenericTypes =
        new TypeScriptSimpleGenericTypesPrettyPrinter(getPrinter(), asyncList);
    getTraverser().setMCSimpleGenericTypesHandler(mCSimpleGenericTypes);
    getTraverser().add4MCSimpleGenericTypes(mCSimpleGenericTypes);
    de.monticore.types.mccollectiontypes._prettyprint.MCCollectionTypesPrettyPrinter mCCollectionTypes =
        new TypeScriptCollectionTypesPrettyPrinter(getPrinter());
    getTraverser().setMCCollectionTypesHandler(mCCollectionTypes);
    getTraverser().add4MCCollectionTypes(mCCollectionTypes);
    de.monticore.types.mcbasictypes._prettyprint.MCBasicTypesPrettyPrinter mCBasicTypes =
        new TypeScriptBasicTypesPrettyPrinter(getPrinter());
    getTraverser().setMCBasicTypesHandler(mCBasicTypes);
    getTraverser().add4MCBasicTypes(mCBasicTypes);

    de.monticore.expressions.expressionsbasis._prettyprint.ExpressionsBasisPrettyPrinter expressionsBasis =
        new TypeScriptExpressionsBasisPrettyPrinter(getPrinter());
    getTraverser().setExpressionsBasisHandler(expressionsBasis);
    getTraverser().add4ExpressionsBasis(expressionsBasis);
  }

}
