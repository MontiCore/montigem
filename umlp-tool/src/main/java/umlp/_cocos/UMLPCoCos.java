/* (c) https://github.com/MontiCore/monticore */

package umlp._cocos;

import de.monticore.cd4analysis.cocos.CD4AnalysisCoCosDelegator;
import de.monticore.cdassociation.cocos.ebnf.RoleAndFieldNamesUnique;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;

/**
 * Collection of all CoCos of the UMLP language
 */
public class UMLPCoCos {

  public static void checkAll(ASTCDCompilationUnit ast) {
    UMLPCoCoChecker checker = new UMLPCoCoChecker();
    addAllUMLPCoCos(checker);
    addAllCDCoCos(checker);
    checker.checkAll(ast);
  }

  public static void addAllUMLPCoCos(UMLPCoCoChecker checker){
    // TODO add cocos, once these are implemented
  }

  public static void addAllCDCoCos(UMLPCoCoChecker checker){
    checker.addChecker(new CD4AnalysisCoCosDelegator().getCheckerForAllCoCos());
    // CoCos for generation
    checker.addCoCo(new RoleAndFieldNamesUnique());
    checker.addCoCo(new UMLPCheckDiagramName());
    checker.addCoCo(new UMLPNoCyclicCDImports());
    checker.addCoCo(new UMLPAssociationQualifierByType());
    checker.addCoCo(new UMLPAssociationQualifierByAttributeName());
    checker.addCoCo(new UMLPAssociationQualifierUnidirectional());
    checker.addCoCo(new UMLPAssociationCardinality());
    checker.addCoCo(new UMLPCheckAttributeModifier());
    checker.addCoCo(new UMLPAttributeTypeIsNotJavaUtilSet());
    checker.addCoCo(new UMLPCheckDerivedAttribute());
    checker.addCoCo(new UMLPCheckOutgoingAssociation());
    checker.addCoCo(new UMLPNotAllCollectionsImplemented());
    checker.addCoCo(new UMLPCheckCDClassModifier());
    checker.addCoCo(new UMLPCheckCDInterfaceModifier());
    checker.addCoCo(new UMLPCheckCDEnumModifier());
    checker.addCoCo(new UMLPCheckAttributeName());
  }
}
