package umlp._cocos;

import de.monticore.cdassociation._cocos.CDAssociationASTCDAssociationCoCo;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._cocos.CDBasisASTCDClassCoCo;
import de.monticore.cdinterfaceandenum._cocos.CDInterfaceAndEnumASTCDInterfaceCoCo;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import umlp.AbstractUMLPTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UMLPCDNameLenghtTest extends AbstractUMLPTest {
  @Test
  public void testInvalid() {
    UMLPCoCoChecker checker = new UMLPCoCoChecker();
/*  checker.addCoCo((CDInterfaceAndEnumASTCDEnumCoCo) new PostgresTableNameLengthCoCo());
    checker.addCoCo((CDBasisASTCDAttributeCoCo) new PostgresTableNameLengthCoCo());*/

    checker.addCoCo((CDBasisASTCDClassCoCo) new PostgresTableNameLengthCoCo());
    checker.addCoCo((CDInterfaceAndEnumASTCDInterfaceCoCo) new PostgresTableNameLengthCoCo());
    checker.addCoCo((CDAssociationASTCDAssociationCoCo) new PostgresTableNameLengthCoCo());

    ASTCDCompilationUnit ast = parse("cocoinvalid/CDNameLengthTooLongCase.umlp");
    runAfterParseTrafo(ast);
    createSymTab(ast);
    runDefaultTrafos(ast);
    recreateSymbolTable(ast);

    checker.checkAll(ast);

    assertTrue(Log.getFindings().getFirst().getMsg().startsWith(PostgresTableNameLengthCoCo.ERROR_CODE));
  }
}
