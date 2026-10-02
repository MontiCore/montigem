/* (c) https://github.com/MontiCore/monticore */
package transpilationtests;

import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.SourceFile;
import org.jsweet.transpiler.util.ConsoleTranspilationHandler;
import org.jsweet.transpiler.util.ErrorCountTranspilationHandler;
import org.junit.jupiter.api.Test;
import util.TranspilerSetupUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jsweet.transpiler.SourceFile.getSourceFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A method with observer registration parameters and the methods it overrides or that override it must have the
 * same parameters, else the TypeScript signatures in the class hierarchy differ.
 */
public class ObserverParamsHierarchyTest {

  @Test
  public void overriddenAndSiblingMethodsGetTheObserverParameters() throws Exception {
    File input = Paths.get("src/test/resources/java/observer/ObserverParamsHierarchy.java").toFile();
    JSweetTranspiler transpiler = TranspilerSetupUtil.getGUITranspiler();
    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(new ConsoleTranspilationHandler());
    SourceFile[] sourceFiles = getSourceFiles(input);
    transpiler.transpile(handler, sourceFiles);
    assertEquals(0, handler.getErrorCount());

    String ts = Files.readString(sourceFiles[0].getTsFile().toPath());
    // Sub declares it, Base gets it from Sub, Sibling gets it from Base
    assertEquals(3, count(ts, "label\\(\\s*__regObsToDomainObj"));
    assertEquals(0, count(ts, "other\\(\\s*__regObs"));
    assertEquals(0, count(ts, "toString\\(\\s*__regObs"));
  }

  private static int count(String text, String regex) {
    Matcher m = Pattern.compile(regex).matcher(text);
    int n = 0;
    while (m.find()) {
      n++;
    }
    return n;
  }
}
