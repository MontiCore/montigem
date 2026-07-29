package transpilationtests;

import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.SourceFile;
import org.jsweet.transpiler.util.ConsoleTranspilationHandler;
import org.jsweet.transpiler.util.ErrorCountTranspilationHandler;
import org.junit.jupiter.api.Test;
import util.TranspilerSetupUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.jsweet.transpiler.SourceFile.getSourceFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class QNameCastTest {

  @Test
  public void testQNameCastTranslatesToImport() throws IOException {
    List<File> inputs = new ArrayList<>();
    inputs.add(Paths.get("src/test/resources/java/commentmodule/Importer.java").toFile());
    inputs.add(Paths.get("src/test/resources/java/commentmodule/Imported1.java").toFile());
    inputs.add(Paths.get("src/test/resources/java/commentmodule/Imported2.java").toFile());
    inputs.add(Paths.get("src/test/resources/java/mc/fenix/input/gemdropdowninputtypes/Option.java").toFile());
    inputs.add(Paths.get("src/test/resources/java/loginmodule/LoginModuleUser.java").toFile());
    inputs.add(Paths.get("src/test/resources/java/loginmodule/LoginModuleUserDeSer.java").toFile());

    checkQNameCast(inputs, TranspilerSetupUtil.transpilerModuleUMLPAdapter());
  }

  public void checkQNameCast(List<File> input, JSweetTranspiler transpiler) throws IOException {
    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());
    SourceFile[] files = getSourceFiles(input);
    transpiler.transpile(handler, files);

    assertEquals(0, handler.getErrorCount());
    assertNotNull(files[0]);

    File transpiledFile = files[0].getTsFile();
    String transpiledContent = Files.readString(Paths.get(transpiledFile.getPath()));

    int occurrences;
    int idx;
    String find;

    occurrences = 0;
    idx = 0;
    // should contain import string with this path
    /*
     Normally translated to @montigem/comment, but since Domain name is
     CommentModule and no srcDirs are specified in jsweetconfig (used for
     relative path resolution), the path from root is taken.
     */
    find = "commentmodule/Imported1";

    while ((idx = transpiledContent.indexOf(find, idx)) != -1) {
      occurrences++;
      idx += find.length(); // move past last match
    }
    assertEquals(1, occurrences);

    occurrences = 0;
    idx = 0;
    find = "commentmodule/Imported2";

    while ((idx = transpiledContent.indexOf(find, idx)) != -1) {
      occurrences++;
      idx += find.length(); // move past last match
    }
    assertEquals(1, occurrences);

    occurrences = 0;
    idx = 0;
    find = "@umlp/input";

    while ((idx = transpiledContent.indexOf(find, idx)) != -1) {
      occurrences++;
      idx += find.length(); // move past last match
    }
    assertEquals(1, occurrences);


    occurrences = 0;
    idx = 0;
    // should contain import string with this path
    find = "mc.fenix.input.gemdropdowninputtypes.Option";

    while ((idx = transpiledContent.indexOf(find, idx)) != -1) {
      occurrences++;
      idx += find.length(); // move past last match
    }
    assertEquals(0, occurrences);
  }

}
