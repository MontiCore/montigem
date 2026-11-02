package aas;

import de.monticore.ast.ASTNode;
import de.monticore.cd._symboltable.BuiltInTypes;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4code._symboltable.CD4CodeSymbolTableCompleter;
import de.monticore.cd4code._symboltable.ICD4CodeArtifactScope;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._visitor.CDAssociationVisitor2;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;
import de.monticore.symboltable.ImportStatement;
import de.se_rwth.commons.logging.Log;
import de.se_rwth.commons.logging.LogStub;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.junit.jupiter.api.BeforeEach;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

public class AbstractTest {

  @BeforeEach
  public void setup() {
    Log.init();
    Log.clearFindings();
    Log.enableFailQuick(true);
    CD4CodeMill.reset();
    CD4CodeMill.init();
    BuiltInTypes.addBuiltInTypes(CD4CodeMill.globalScope(), false);
    _setup();
  }

  public void _setup() {
    //override this method if needed
  }

  protected ASTCDCompilationUnit parse(String fileName) {
    try {
      Optional<ASTCDCompilationUnit> res = CD4CodeMill.parser().parse(fileName);
      if (res.isPresent()) {
        return res.get();
      } else {
        fail("No compilation unit found");

      }
      return null;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ICD4CodeArtifactScope createSymTab(ASTCDCompilationUnit ast) {
    ICD4CodeArtifactScope as = CD4CodeMill.scopesGenitorDelegator().createFromAST(ast);
    as.addAllImports(ast.getMCImportStatementList().stream()
        .map(i -> new ImportStatement(i.getQName(), i.isStar()))
        .collect(Collectors.toList()));
    as.addImports(new ImportStatement("java.lang", true));
    CD4CodeSymbolTableCompleter c = new CD4CodeSymbolTableCompleter(ast);
    ast.accept(c.getTraverser());
    return as;
  }

  public static Reference semanticId(String identifier) {
    return new DefaultReference.Builder()
        .type(ReferenceTypes.EXTERNAL_REFERENCE)
        .keys(
            new DefaultKey.Builder()
                .type(KeyTypes.GLOBAL_REFERENCE)
                .value(identifier)
                .build()
        )
        .build();
  }
}

