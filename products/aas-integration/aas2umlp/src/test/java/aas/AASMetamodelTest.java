package aas;

import de.monticore.cd4code._cocos.CD4CodeCoCoChecker;
import de.monticore.cdmerge.validation.CDMergeCD4ACoCos;
import de.monticore.cd4code.cocos.CD4CodeCoCos;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AASMetamodelTest extends AbstractTest {

  private String metamodelPath = "aas/decorator/aasMetamodel.cd";

  @Test
  void validateMetaModel() {

    InputStream input = getClass().getClassLoader().getResourceAsStream(metamodelPath);

    assertNotNull(input, "Resource not found on classpath: " + metamodelPath);

    ASTCDCompilationUnit ast = parse("src/main/resources/" + metamodelPath);
    createSymTab(ast);

    CD4CodeCoCoChecker checker = new CDMergeCD4ACoCos().getCheckerForMergedCDs();
    checker.checkAll(ast);

  }
}
