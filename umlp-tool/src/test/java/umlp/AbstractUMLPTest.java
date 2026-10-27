/* (c) https://github.com/MontiCore/monticore */

package umlp;

import de.monticore.cd._parser.CDAfterParseHelper;
import de.monticore.cd._symboltable.BuiltInTypes;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4code._symboltable.CD4CodeSymbolTableCompleter;
import de.monticore.cd4code.trafo.CD4CodeAfterParseTrafo;
import de.monticore.cd4code.types3.CD4CodeTypeCheck3;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdmerge.util.CDMergeAfterParseTrafo;
import de.monticore.generating.GeneratorSetup;
import de.monticore.io.paths.MCPath;
import de.monticore.ocl.ocl.OCLMill;
import de.monticore.ocl.ocl._ast.ASTOCLCompilationUnit;
import de.monticore.ocl.util.SymbolTableUtil;
import de.se_rwth.commons.logging.Finding;
import de.se_rwth.commons.logging.Log;
import de.se_rwth.commons.logging.LogStub;
import org.junit.jupiter.api.BeforeEach;
import umlp._symboltable.IUMLPArtifactScope;
import umlp._visitor.UMLPTraverser;
import umlp.generator.domain.UMLAssocToAttrTrafo;
import umlp.generator.domain.UMLAttrToAssocTrafo;
import umlp.generator.domain.UMLImportTrafo;
import umlp.generator.domain.UMLSetDefaultsTrafo;
import umlp.generator.util.UMLPSymbolTables;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractUMLPTest {

  public static final String BUILD_FOLDER = "build";

  @BeforeEach
  public void setup() {
    LogStub.init();
    Log.enableFailQuick(false);
    UMLPMill.reset();
    UMLPMill.init();
    BuiltInTypes.addBuiltInTypes(UMLPMill.globalScope(), false);
    _setup();
  }

  public void _setup() {
    //override this method if needed
  }

  public ASTCDCompilationUnit parse(String model) {
    return parse("src/test/resources", model);
  }

  public ASTOCLCompilationUnit parseOCL(String model, ASTCDCompilationUnit cdAst) {
    return parseOCL("src/test/resources", model, cdAst);
  }

  public ASTCDCompilationUnit parse(String testPath, String model) {
    Path m = Paths.get(testPath, model);
    assertTrue(m.toFile().exists());
    try {
      Optional<ASTCDCompilationUnit> ast = UMLPMill.parser().parse(m.toString());
      assertTrue(ast.isPresent());
      return ast.get();
    }
    catch (IOException e) {
      e.printStackTrace();
      fail();
    }
    return null;
  }

  public ASTOCLCompilationUnit parseOCL(String testPath, String model, ASTCDCompilationUnit cdAst) {
    Path m = Paths.get(testPath, model);
    assertTrue(m.toFile().exists());
    OCLMill.init();
    try {
      Optional<ASTOCLCompilationUnit> ast = OCLMill.parser().parse(m.toString());
      assertTrue(ast.isPresent());
      SymbolTableUtil.prepareMill();
      UMLPSymbolTables.addCd4cSymbols();
      SymbolTableUtil.runSymTabGenitor(ast.get());
      SymbolTableUtil.runSymTabCompleter(ast.get());
      return ast.get();
    }
    catch (IOException e) {
      e.printStackTrace();
      fail();
    }
    return null;
  }

  public IUMLPArtifactScope createSymTab(ASTCDCompilationUnit ast) {
    IUMLPArtifactScope as = UMLPMill.scopesGenitorDelegator().createFromAST(ast);
    CD4CodeTypeCheck3.init();
    CD4CodeSymbolTableCompleter c = new CD4CodeSymbolTableCompleter(ast);
    ast.accept(c.getTraverser());
    return as;
  }

  protected void runAfterParseTrafo(ASTCDCompilationUnit node){
    UMLPTraverser trav = UMLPMill.inheritanceTraverser();
    CD4CodeAfterParseTrafo.init(new CDAfterParseHelper(), trav);
    node.accept(trav);

    UMLImportTrafo importTrafo = new UMLImportTrafo(CD4CodeMill.inheritanceTraverser());
    importTrafo.transform(node);
  }

  protected void runAfterParseTrafoForMerge(ASTCDCompilationUnit node){
    UMLPTraverser trav = UMLPMill.inheritanceTraverser();
    CDMergeAfterParseTrafo.init(new CDAfterParseHelper(), trav);
    node.accept(trav);
  }

  public static void assertNoErrors() {
    if (Log.getErrorCount() > 0) {
      System.err.println("Expected no errors, but got: ");
      for (Finding f : Log.getFindings()) {
        if (f.isError()) {
          System.err.println(f.toString());
        }
      }
      fail("Unexpected errors: " + Log.getFindings().stream().map(Finding::toString).collect(Collectors.joining("\n")));
    }
  }

  public static void assertErrors(String... errorCodes) {
    assertEquals(errorCodes.length, Log.getErrorCount(),
        "Expected and actual number of errors differ!");

    for (String expectedErrorCode : errorCodes) {
      boolean foundError = false;
      for (Finding f : Log.getFindings()) {
        if (f.isError() && f.getMsg().startsWith(expectedErrorCode)) {
          foundError = true;
          break;
        }
      }
      if (!foundError) {
        fail("Expected to find an error with the error code '" +
            expectedErrorCode + "', but the error did not occur!");
      }
    }
  }

  public static GeneratorSetup getGenSetup(){
    GeneratorSetup genSetup = new GeneratorSetup();

    // set output directory
    File outDir = new File(BUILD_FOLDER + "/generated-test-sources");
    genSetup.setOutputDirectory(outDir);

    // set hwc directories
    MCPath hwcPath = new MCPath("src/test/java/");
    genSetup.setHandcodedPath(hwcPath);
    return genSetup;
  }

  public void runDefaultTrafos(ASTCDCompilationUnit node) {
    UMLAttrToAssocTrafo assocTrafo = new UMLAttrToAssocTrafo(CD4CodeMill.inheritanceTraverser());
    assocTrafo.transform(node);

    UMLSetDefaultsTrafo assocNameTrafo = new UMLSetDefaultsTrafo(CD4CodeMill.inheritanceTraverser());
    assocNameTrafo.transform(node);

    UMLAssocToAttrTrafo assoc2Trafo = new UMLAssocToAttrTrafo(CD4CodeMill.inheritanceTraverser());
    assoc2Trafo.transform(node);
  }

  public IUMLPArtifactScope recreateSymbolTable(ASTCDCompilationUnit node) {
    // rebuild symbol table
    CD4CodeMill.globalScope().removeSubScope(node.getEnclosingScope());
    IUMLPArtifactScope as = UMLPMill.scopesGenitorDelegator().createFromAST(node);
    CD4CodeTypeCheck3.init();
    node.accept(new CD4CodeSymbolTableCompleter(node).getTraverser());
    return as;
  }
}
