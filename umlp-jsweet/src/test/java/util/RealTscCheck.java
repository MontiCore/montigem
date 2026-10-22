package util;

import org.jsweet.transpiler.JSweetTranspiler;
import org.jsweet.transpiler.SourceFile;
import org.jsweet.transpiler.util.ConsoleTranspilationHandler;
import org.jsweet.transpiler.util.ErrorCountTranspilationHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static org.jsweet.transpiler.SourceFile.getSourceFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Runs a Java fixture through the real UMLP adapters (UMLPAdapter -&gt; ... -&gt;
 * UMLPRTEImportAdapter -&gt; CollectionsAdapter, same chain the generator uses), verifies the
 * generated TypeScript with a real, independently pinned `tsc` (not JSweet's bundled one), and
 * lets the caller execute the compiled JS with `node` via a caller-supplied runner script.
 *
 * `@umlp/commonj2ts` is resolved via a facade (src/test/resources/tsc-check/commonj2ts-facade)
 * that imports the classes it exports directly by relative path from the real commonj2ts source,
 * bypassing its root barrel (which pulls in an Angular-coupled pipe, requiring @angular/core just
 * for tsc's module resolution). Extend that facade file if a check needs a class it doesn't
 * currently export -- everything else (common/, javautil/, logging/) is Angular/rxjs-free.
 *
 * Each check is staged under build/tsc-check/&lt;checkName&gt;, isolated from other checks.
 */
public final class RealTscCheck {

  private static final String TYPESCRIPT_VERSION = "typescript@7.0.2";

  private static final Path COMMONJ2TS_SRC = Paths.get("..")
      .resolve("umlp-rte/frontend/commonj2ts/src/main/gui/mc/fenix/commonj2ts")
      .toAbsolutePath()
      .normalize();

  private RealTscCheck() {
  }

  /** Whether umlp-rte/frontend/commonj2ts is present in this checkout; gate tests on this with assumeTrue. */
  public static boolean commonj2tsAvailable() {
    return Files.isDirectory(COMMONJ2TS_SRC);
  }

  public record Result(int exitCode, String output) {
    /** The value printed by the {@code RESULT=...} line (see {@link #staticMethodRunner}), or null if absent. */
    public String resultValue() {
      return output.lines()
          .filter(line -> line.startsWith("RESULT="))
          .map(line -> line.substring("RESULT=".length()))
          .findFirst()
          .orElse(null);
    }

    /** The values printed by {@code RESULT:<methodName>=...} lines (see {@link #multiMethodRunner}), keyed by method name. */
    public Map<String, String> resultValues() {
      Map<String, String> values = new LinkedHashMap<>();
      for (String line : output.lines().filter(line -> line.startsWith("RESULT:")).toList()) {
        int eq = line.indexOf('=', "RESULT:".length());
        if (eq > 0) {
          values.put(line.substring("RESULT:".length(), eq), line.substring(eq + 1));
        }
      }
      return values;
    }
  }

  /**
   * Builds a Node runner script for {@link #compileAndRun} that calls a static, no-arg method on
   * the compiled fixture (the JSweet adapters make most methods touching collections/optionals
   * async, so the result is treated as a possible Promise either way) and prints it as
   * {@code RESULT=<JSON>} before exiting 0, or prints the error and exits 1 on failure.
   *
   * <p>For anything more involved (arguments, instance methods, multiple calls), write the
   * runner script directly instead -- see {@link #compileAndRun} for the contract it must follow.
   *
   * @param className  the fixture's Java/TS class name (used both as the require path and the
   *                   export name, which JSweet always keeps identical)
   * @param methodName the static method to call
   */
  public static String staticMethodRunner(String className, String methodName) {
    return """
        const { %1$s } = require("./%1$s");

        Promise.resolve(%1$s["%2$s"]())
          .then((result) => {
            console.log("RESULT=" + JSON.stringify(result));
            process.exit(0);
          })
          .catch((e) => {
            console.error(e);
            process.exit(1);
          });
        """.formatted(className, methodName);
  }

  /**
   * Like {@link #staticMethodRunner}, but for many static, no-arg methods on the same fixture in
   * a single compile+run: one {@code npx tsc} and one {@code node} invocation cover the whole
   * batch instead of one pair per method. Prints one {@code RESULT:<methodName>=<JSON>} line per
   * method (read back via {@link Result#resultValues()}); a thrown method logs the error and
   * fails the whole run (exit 1) rather than skipping just that one method, so a broken method is
   * never silently missing from the results.
   */
  public static String multiMethodRunner(String className, List<String> methodNames) {
    StringBuilder script = new StringBuilder();
    script.append("const { ").append(className).append(" } = require(\"./").append(className).append("\");\n\n");
    script.append("async function main() {\n");
    for (String methodName : methodNames) {
      script.append("  const result_").append(methodName)
          .append(" = await ").append(className).append("[\"").append(methodName).append("\"]();\n");
      script.append("  console.log(\"RESULT:").append(methodName)
          .append("=\" + JSON.stringify(result_").append(methodName).append("));\n");
    }
    script.append("}\n\n");
    script.append("main().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });\n");
    return script.toString();
  }

  private static final Path RUNTIME_FIXTURES_DIR = Paths.get("src/test/java/resources/exectests");

  public static Result compileAndRun(Class<?> fixtureClass, String runJs) throws IOException {
    return compileAndRun(fixtureClass, runJs, new Class<?>[0]);
  }

  public static Result compileAndRun(Class<?> fixtureClass, String runJs,
      Class<?>... supportingClasses) throws IOException {
    List<Path> sources = new ArrayList<>();
    sources.add(RUNTIME_FIXTURES_DIR.resolve(fixtureClass.getSimpleName() + ".java"));
    for (Class<?> supportingClass : supportingClasses) {
      sources.add(RUNTIME_FIXTURES_DIR.resolve(supportingClass.getSimpleName() + ".java"));
    }
    return compileAndRun(fixtureClass.getSimpleName(), sources, runJs);
  }

  /**
   * Same as {@link #compileAndRun(String, Path, String)}, deriving the fixture's source path
   * from its simple name under src/test/java/runtime -- see e.g. {@code
   * runtime.CollectionsSingletonRuntimeCheck}. Every fixture used with {@link RealTscCheck} lives
   * there, so callers don't need to spell out the path themselves.
   */
  public static Result compileAndRun(String checkName, Class<?> fixtureClass, String runJs) throws IOException {
    return compileAndRun(checkName, RUNTIME_FIXTURES_DIR.resolve(fixtureClass.getSimpleName() + ".java"), runJs);
  }

  /**
   * Transpiles {@code fixture} through the real UMLP domain adapters, compiles the result with a
   * real, independently pinned tsc, then runs {@code runJs} against the compiled output with
   * node and returns its result for the caller to assert on.
   *
   * @param checkName unique name for this check; staged under build/tsc-check/&lt;checkName&gt;
   * @param fixture   path to the Java source to transpile
   * @param runJs     Node script content executed from the compiled output directory; require
   *                  the compiled fixture as {@code "./<FixtureClassName>"}
   */
  public static Result compileAndRun(String checkName, Path fixture, String runJs) throws IOException {
    return compileAndRun(checkName, List.of(fixture), runJs);
  }

  private static Result compileAndRun(String checkName, List<Path> fixtures, String runJs)
      throws IOException {
    JSweetTranspiler transpiler = TranspilerSetupUtil.getGUITranspiler(
        Paths.get("build/transpile/" + checkName).toFile());
    ErrorCountTranspilationHandler handler = new ErrorCountTranspilationHandler(
        new ConsoleTranspilationHandler());
    SourceFile[] sourceFiles = getSourceFiles(
        fixtures.stream().map(Path::toFile).toArray(File[]::new));
    transpiler.transpile(handler, sourceFiles);
    assertEquals(0, handler.getErrorCount(),
        "JSweet transpilation of " + fixtures + " reported errors");

    // Stage a self-contained directory: the real commonj2ts source (via link), our facade, the
    // generated fixture, the runner script and the shared tsconfig.
    Path tscCheckDir = Paths.get("build/tsc-check/" + checkName).toAbsolutePath();
    Files.createDirectories(tscCheckDir);
    Files.createDirectories(tscCheckDir.resolve("out"));

    createDirectoryLink(tscCheckDir.resolve("commonj2ts-src"), COMMONJ2TS_SRC);

    Path facadeDir = tscCheckDir.resolve("commonj2ts-facade");
    Files.createDirectories(facadeDir);
    Files.copy(
        Paths.get("src/test/resources/tsc-check/commonj2ts-facade/index.ts"),
        facadeDir.resolve("index.ts"),
        StandardCopyOption.REPLACE_EXISTING);

    for (SourceFile sourceFile : sourceFiles) {
      assertNotNull(sourceFile.getTsFile());
      Path generatedTs = sourceFile.getTsFile().toPath();
      Files.copy(
          generatedTs,
          tscCheckDir.resolve(generatedTs.getFileName()),
          StandardCopyOption.REPLACE_EXISTING);
    }

    Files.writeString(tscCheckDir.resolve("out/run.js"), runJs);

    Files.copy(
        Paths.get("src/test/resources/tsc-check/tsconfig.json"),
        tscCheckDir.resolve("tsconfig.json"),
        StandardCopyOption.REPLACE_EXISTING);

    // tsc resolves '@umlp/commonj2ts' via the tsconfig "paths" mapping to the facade, which
    // makes tsc treat it as first-party code it actually compiles and emits (a real node_modules
    // entry here would make tsc treat it as an external dependency and skip emitting it).

    // Compile with a real, independently pinned tsc via a single npx call, using the node
    // already downloaded for this build (see build.gradle's addNodePathToTestResources).
    String nodeDir = resolveNodeDir();
    String npxExecutable = nodeDir + File.separator + (isWindows() ? "npx.cmd" : "npx");
    String nodeExecutable = nodeDir + File.separator + (isWindows() ? "node.exe" : "node");

    List<String> tscCommand = new ArrayList<>();
    if (isWindows()) {
      // npx on Windows is a .cmd batch script; run it through cmd.exe explicitly rather than
      // relying on ProcessBuilder's own .cmd handling.
      tscCommand.add("cmd");
      tscCommand.add("/c");
    }
    // "--package typescript@..." names the package explicitly, separate from the command to run
    // ("tsc"). Without it, npx treats the package spec as both package *and* command name, falls
    // back to the package's one bin ("tsc") but still forwards the literal "tsc" token as an
    // argument to it, which tsc then misreads as a source file alongside -p.
    tscCommand.addAll(List.of(npxExecutable, "--yes", "--package", TYPESCRIPT_VERSION, "tsc", "-p", "tsconfig.json"));

    ProcessResult tscResult = runProcess(tscCommand, tscCheckDir, nodeDir);
    assertEquals(0, tscResult.exitCode(), "npx tsc failed:\n" + tscResult.output());

    // Unlike tsc, node has no idea about tsconfig "paths" -- the compiled fixture still contains
    // the literal require("@umlp/commonj2ts"), so give node a real node_modules entry to resolve
    // it at runtime, now pointing at the *compiled* facade (which exists because tsc emitted it,
    // having treated it as first-party code via "paths" rather than as an external dependency).
    Files.createDirectories(tscCheckDir.resolve("out/node_modules/@umlp"));
    createDirectoryLink(
        tscCheckDir.resolve("out/node_modules/@umlp/commonj2ts"),
        tscCheckDir.resolve("out/commonj2ts-facade"));

    // Execute the compiled JS directly with node (not through npx).
    ProcessResult nodeResult = runProcess(List.of(nodeExecutable, "out/run.js"), tscCheckDir, nodeDir);
    return new Result(nodeResult.exitCode(), nodeResult.output());
  }

  /** Same node download the rest of this build uses, resolved via the generated nodeDir.properties (see Util). */
  private static String resolveNodeDir() throws IOException {
    Properties nodeDirProps = new Properties();
    try (InputStream in = RealTscCheck.class.getResourceAsStream("/nodeDir.properties")) {
      assertNotNull(in, "nodeDir.properties not found on classpath; run via Gradle so "
          + "addNodePathToTestResources has generated it");
      nodeDirProps.load(in);
    }
    return Objects.requireNonNull(nodeDirProps.getProperty("node_dir"), "node_dir property missing");
  }

  private record ProcessResult(int exitCode, String output) {
  }

  private static ProcessResult runProcess(List<String> command, Path workingDir) throws IOException {
    return runProcess(command, workingDir, null);
  }

  /**
   * @param extraPathEntry prepended to the child process's PATH, same as root build.gradle's
   *                       execOverrides do for NpmTask/NpxTask/NodeTask -- without it, npx.cmd's
   *                       own internal call to "node" can't find it and fails.
   */
  private static ProcessResult runProcess(List<String> command, Path workingDir, String extraPathEntry)
      throws IOException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workingDir.toFile());
    pb.redirectErrorStream(true);
    if (extraPathEntry != null) {
      String currentPath = pb.environment().getOrDefault("PATH", "");
      pb.environment().put("PATH", extraPathEntry + File.pathSeparator + currentPath);
    }
    Process process = pb.start();
    String output;
    try (InputStream in = process.getInputStream()) {
      output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    int exitCode;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while waiting for: " + String.join(" ", command), e);
    }
    return new ProcessResult(exitCode, output);
  }

  /**
   * Creates a directory link at {@code link} pointing at {@code target}: a real symlink on
   * POSIX, or a junction on Windows (via {@code mklink /J}) since creating actual NTFS
   * symlinks requires SeCreateSymbolicLinkPrivilege that a plain Gradle/JVM process usually
   * doesn't hold. Adapted from
   * umlp-tool/gradle-plugin/.../de/montigem/gradle/specific/CreateSymlinkTask.java.
   */
  private static void createDirectoryLink(Path link, Path target) throws IOException {
    String linkPath = link.toAbsolutePath().normalize().toString();
    String targetPath = target.toFile().getCanonicalPath();

    if (Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
      if (isLink(link)) {
        Files.delete(link);
      } else {
        throw new IOException("Cannot create link, a real path already occupies " + linkPath);
      }
    }

    if (isWindows()) {
      createJunction(linkPath, targetPath);
    } else {
      Files.createSymbolicLink(link, target);
    }
  }

  // Windows junctions are not symbolic links to NIO, they only show up as "other"
  private static boolean isLink(Path path) throws IOException {
    if (Files.isSymbolicLink(path)) {
      return true;
    }
    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    return attrs.isOther();
  }

  private static void createJunction(String linkPath, String targetPath) throws IOException {
    ProcessResult result = runProcess(List.of("cmd", "/c", "mklink", "/J", linkPath, targetPath),
        Paths.get(".").toAbsolutePath());
    if (result.exitCode() != 0) {
      throw new IOException("mklink /J failed (exit " + result.exitCode() + "):\n" + result.output());
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }
}
