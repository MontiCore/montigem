package util;

import de.monticore.io.paths.MCPath;
import de.monticore.ocl.ocl.OCLMill;
import de.monticore.ocl.ocl._ast.ASTOCLCompilationUnit;
import de.monticore.ocl.ocl._symboltable.IOCLArtifactScope;
import de.monticore.ocl.ocl._symboltable.IOCLGlobalScope;
import de.monticore.ocl.util.SymbolTableUtil;
import de.monticore.symboltable.ImportStatement;
import de.se_rwth.commons.logging.Log;
import umlp._symboltable.CDRoleSymbol2VariableSymbolAdapterDeSer;
import umlp.backendrte.common.GemIdentifiable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

public interface IOCL2XTest<T extends GemIdentifiable> {
  String TEST_RESOURCE_PATH = "../models/ocl";

  default void languageSetup() {
    SymbolTableUtil.prepareMill();
    NoRegexOCLTypeCheck3.init();

    IOCLGlobalScope gs = OCLMill.globalScope();

    gs.putTypeSymbolDeSer("de.monticore.cdbasis._symboltable.CDTypeSymbol");
    gs.putFunctionSymbolDeSer("de.monticore.cd4codebasis._symboltable.CDMethodSignatureSymbol");
    gs.putVariableSymbolDeSer("de.monticore.symbols.oosymbols._symboltable.FieldSymbol");
    gs.putSymbolDeSer("de.monticore.cdassociation._symboltable.CDRoleSymbol",
        new CDRoleSymbol2VariableSymbolAdapterDeSer());

    Path symbolPath = Paths.get("build/umlp2sym");
    assertTrue(symbolPath.toFile().exists());
    gs.setSymbolPath(new MCPath(symbolPath));
    gs.loadDiagram("Mydomain");

    Log.enableFailQuick(false);
  }

  default void languageCleanup() {
    OCLMill.reset();
  }

  default void createSymtab(ASTOCLCompilationUnit ocl) {
    SymbolTableUtil.runSymTabGenitor(ocl);
    ((IOCLArtifactScope) ocl.getEnclosingScope()).addImports(new ImportStatement("Mydomain", true));
    SymbolTableUtil.runSymTabCompleter(ocl);
  }

  default String getOclFromFile(String path, String file) {
    Path filepath = Paths.get(TEST_RESOURCE_PATH + "/" + path + "/" + file + ".ocl");
    try {
      return Files.readString(filepath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  default List<Long> unpackGemIds(Optional<ResultSet> res, String columnLabel) throws SQLException {
    assertTrue(res.isPresent());
    return unpackGemIds(res.get(), columnLabel);
  }

  default List<Long> unpackGemIds(ResultSet res, String columnLabel) throws SQLException {
    List<Long> violatingObjectGemIds = new ArrayList<>();
    while (res.next()) {
      violatingObjectGemIds.add(res.getLong(columnLabel));
    }
    return violatingObjectGemIds;
  }

  default List<Long> translateAndExecQuery(String constraint) throws IOException, SQLException {
    ASTOCLCompilationUnit ocl = OCLMill.parser().parse_String(constraint).get();
    createSymtab(ocl);
    return _translateAndExecQuery(ocl);
  }

  List<Long> translateAndExecQueryFromPath(String path) throws SQLException, IOException;

  default Map<String, Predicate<T>> getConstraintToChecker() {
    return Map.of();
  }

  default boolean isCypher(){
    return false;
  }

  List<Long> _translateAndExecQuery(ASTOCLCompilationUnit ocl);

  String getResourceFolder();
}
