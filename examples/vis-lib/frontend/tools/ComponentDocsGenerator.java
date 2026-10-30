/* (c) https://github.com/MontiCore/monticore */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates {@code common/ComponentDocs.java} for the vis-lib demonstrator from the doc
 * comments of the component models of the MontiGem runtime library
 * ({@code umlp-rte/frontend/<lib>/src/main/gui/mc/fenix/**.gui}).
 *
 * <p>The same doc comments feed the online documentation
 * (https://monticore.pages.rwth-aachen.de/umlp/gendoc_components/); this generator makes them
 * available inside the running demonstrator, so every example page explains its component
 * with the current text of the library documentation.
 *
 * <p>Run by the Gradle task {@code generateComponentDocs} (see frontend/build.gradle) through the
 * JDK source-file launcher: {@code java ComponentDocsGenerator.java <rte-frontend-dir> <output-file>}.
 * It is a single file without dependencies on purpose - it must not need a build of its own.
 *
 * <p>Parsing is deliberately simple and tolerant: a component's doc comment is the last
 * {@code /** ... *}{@code /} block before {@code component Name(}, and every parameter of the
 * declaration may carry its own doc comment directly in front of it. Line comments, string
 * literals and nested generics are respected when the parameter list is split.
 */
public class ComponentDocsGenerator {

  static class Param {
    String name;
    String type;
    String def;
    String doc;
  }

  static class Comp {
    String pkg;
    String name;
    String doc;
    List<Param> params = new ArrayList<>();
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("usage: java ComponentDocsGenerator.java <rte-frontend-dir> <output-java-file>");
      System.exit(2);
    }
    Path root = Paths.get(args[0]);
    Path out = Paths.get(args[1]);

    List<Path> files;
    try (Stream<Path> s = Files.walk(root)) {
      files = s.filter(p -> p.toString().endsWith(".gui"))
          .filter(p -> p.toString().replace('\\', '/').contains("/src/main/gui/mc/fenix/"))
          .sorted()
          .collect(Collectors.toList());
    }

    List<Comp> comps = new ArrayList<>();
    for (Path f : files) {
      Comp c = parse(Files.readString(f, StandardCharsets.UTF_8));
      if (c != null) {
        comps.add(c);
      }
    }

    String java = render(comps);
    Files.createDirectories(out.getParent());
    // Only touch the file when the content changed, so the source tree stays clean for Git
    // and Gradle's up-to-date checks of the consumers are not invalidated needlessly.
    if (!Files.exists(out) || !Files.readString(out, StandardCharsets.UTF_8).equals(java)) {
      Files.writeString(out, java, StandardCharsets.UTF_8);
      System.out.println("ComponentDocs: wrote " + comps.size() + " components to " + out);
    } else {
      System.out.println("ComponentDocs: " + comps.size() + " components, up to date");
    }
  }

  // ---------------------------------------------------------------- parsing

  static Comp parse(String text) {
    Matcher pm = Pattern.compile("package\\s+([\\w.]+)\\s*;").matcher(text);
    if (!pm.find()) {
      return null;
    }
    Matcher cm = Pattern.compile("component\\s+(\\w+)\\s*\\(").matcher(text);
    if (!cm.find()) {
      return null;
    }
    Comp c = new Comp();
    c.pkg = pm.group(1);
    c.name = cm.group(1);
    c.doc = lastDocComment(text.substring(0, cm.start()));
    int start = cm.end();
    int end = matchParen(text, start - 1);
    for (String chunk : splitParams(text.substring(start, end))) {
      Param p = parseParam(chunk);
      if (p != null) {
        c.params.add(p);
      }
    }
    return c;
  }

  /** The doc comment directly in front of the declaration, "" if there is none. */
  static String lastDocComment(String s) {
    int idx = s.lastIndexOf("/**");
    if (idx < 0) {
      return "";
    }
    int endIdx = s.indexOf("*/", idx);
    if (endIdx < 0) {
      return "";
    }
    if (!s.substring(endIdx + 2).trim().isEmpty()) {
      return "";
    }
    return cleanDoc(s.substring(idx + 3, endIdx));
  }

  /** Index of the ')' matching the '(' at openIdx, skipping strings and comments. */
  static int matchParen(String t, int openIdx) {
    int depth = 0;
    int i = openIdx;
    while (i < t.length()) {
      char c = t.charAt(i);
      if (t.startsWith("/*", i)) {
        i = t.indexOf("*/", i + 2);
        i = i < 0 ? t.length() : i + 2;
        continue;
      }
      if (t.startsWith("//", i)) {
        int nl = t.indexOf('\n', i);
        i = nl < 0 ? t.length() : nl + 1;
        continue;
      }
      if (c == '"') {
        i = skipString(t, i);
        continue;
      }
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
      i++;
    }
    return t.length();
  }

  static int skipString(String t, int i) {
    i++;
    while (i < t.length()) {
      char c = t.charAt(i);
      if (c == '\\') {
        i += 2;
        continue;
      }
      if (c == '"') {
        return i + 1;
      }
      i++;
    }
    return i;
  }

  /** Splits the parameter region on depth-0 commas; block comments stay with the following parameter. */
  static List<String> splitParams(String r) {
    List<String> parts = new ArrayList<>();
    StringBuilder cur = new StringBuilder();
    int depth = 0;
    int i = 0;
    while (i < r.length()) {
      char c = r.charAt(i);
      if (r.startsWith("/*", i)) {
        int e = r.indexOf("*/", i + 2);
        e = e < 0 ? r.length() : e + 2;
        cur.append(r, i, e);
        i = e;
        continue;
      }
      if (r.startsWith("//", i)) {
        int nl = r.indexOf('\n', i);
        i = nl < 0 ? r.length() : nl + 1;
        cur.append('\n');
        continue;
      }
      if (c == '"') {
        int e = skipString(r, i);
        cur.append(r, i, e);
        i = e;
        continue;
      }
      if (c == '<' || c == '(' || c == '[') {
        depth++;
      } else if (c == '>' || c == ')' || c == ']') {
        depth--;
      }
      if (c == ',' && depth == 0) {
        parts.add(cur.toString());
        cur.setLength(0);
        i++;
        continue;
      }
      cur.append(c);
      i++;
    }
    if (!cur.toString().trim().isEmpty()) {
      parts.add(cur.toString());
    }
    return parts;
  }

  static Param parseParam(String chunk) {
    String doc = "";
    Matcher m = Pattern.compile("/\\*\\*(.*?)\\*/", Pattern.DOTALL).matcher(chunk);
    int lastEnd = 0;
    while (m.find()) {
      doc = cleanDoc(m.group(1));
      lastEnd = m.end();
    }
    String decl = chunk.substring(lastEnd).replaceAll("(?s)/\\*.*?\\*/", "").trim();
    if (decl.isEmpty()) {
      return null;
    }
    String def = "";
    int eq = decl.indexOf('=');
    if (eq >= 0) {
      def = decl.substring(eq + 1).trim();
      decl = decl.substring(0, eq).trim();
    }
    decl = decl.replaceAll("\\s+", " ");
    int sp = decl.lastIndexOf(' ');
    if (sp < 0) {
      return null;
    }
    Param p = new Param();
    p.type = decl.substring(0, sp).trim();
    p.name = decl.substring(sp + 1).trim();
    p.def = def;
    p.doc = doc;
    if (!p.name.matches("\\w+")) {
      return null;
    }
    return p;
  }

  /**
   * Normalizes a doc comment: leading stars and hard line wraps are removed, blank lines and
   * list-like lines (starting with a backtick, "-", "*" or a number) keep a line break.
   */
  static String cleanDoc(String raw) {
    String[] lines = raw.split("\\r?\\n");
    StringBuilder sb = new StringBuilder();
    boolean blank = false;
    for (String l : lines) {
      String s = l.trim();
      if (s.equals("*") || s.startsWith("* ")) {
        s = s.substring(1).trim();
      }
      if (s.isEmpty()) {
        blank = sb.length() > 0;
        continue;
      }
      boolean listy = s.matches("^(`|- |\\* |\\d+\\. ).*");
      if (sb.length() > 0) {
        sb.append(blank || listy ? "\n" : " ");
      }
      sb.append(s);
      blank = false;
    }
    return sb.toString().trim();
  }

  // -------------------------------------------------------------- rendering

  static String render(List<Comp> comps) {
    StringBuilder sb = new StringBuilder();
    sb.append("/* (c) https://github.com/MontiCore/monticore */\n");
    sb.append("package common;\n\n");
    sb.append("import java.util.ArrayList;\n");
    sb.append("import java.util.List;\n\n");
    sb.append("/**\n");
    sb.append("  GENERATED - do not edit. Regenerated by the Gradle task generateComponentDocs of\n");
    sb.append("  examples/vis-lib/frontend (tools/ComponentDocsGenerator.java) from the doc comments of\n");
    sb.append("  the component models in umlp-rte/frontend/<lib>/src/main/gui/mc/fenix. The task runs\n");
    sb.append("  before every frontend build, so the texts follow the library documentation.\n\n");
    sb.append("  Used by common.ExampleNotes / common.ParameterList to explain the component of an\n");
    sb.append("  example page with the same text as the online documentation.\n");
    sb.append("*/\n");
    sb.append("public class ComponentDocs {\n\n");
    sb.append("  /** Base URL of the generated component documentation. */\n");
    sb.append("  public static final String DOC_BASE_URL = \"https://monticore.pages.rwth-aachen.de/umlp/gendoc_components/\";\n\n");

    sb.append("  /** Qualified package of the component, e.g. \"mc.fenix.charts\"; \"\" for unknown components. */\n");
    sb.append("  public static String packageOf(String component) {\n");
    sb.append("    switch (component) {\n");
    for (Comp c : comps) {
      sb.append("      case ").append(lit(c.name)).append(": return ").append(lit(c.pkg)).append(";\n");
    }
    sb.append("      default: return \"\";\n");
    sb.append("    }\n");
    sb.append("  }\n\n");

    sb.append("  /** Description from the component's doc comment; \"\" for unknown components. */\n");
    sb.append("  public static String description(String component) {\n");
    sb.append("    switch (component) {\n");
    for (Comp c : comps) {
      sb.append("      case ").append(lit(c.name)).append(": return ").append(lit(c.doc)).append(";\n");
    }
    sb.append("      default: return \"\";\n");
    sb.append("    }\n");
    sb.append("  }\n\n");

    sb.append("  /**\n");
    sb.append("    Parameters of the component as \"name|type|default|doc\" entries in declaration order\n");
    sb.append("    (default is empty when the parameter has none); an empty list for unknown components.\n");
    sb.append("  */\n");
    sb.append("  public static List<String> parameters(String component) {\n");
    sb.append("    List<String> list = new ArrayList<>();\n");
    sb.append("    switch (component) {\n");
    for (Comp c : comps) {
      sb.append("      case ").append(lit(c.name)).append(":\n");
      for (Param p : c.params) {
        String entry = bar(p.name) + "|" + bar(p.type) + "|" + bar(p.def) + "|" + bar(p.doc);
        sb.append("        list.add(").append(lit(entry)).append(");\n");
      }
      sb.append("        break;\n");
    }
    sb.append("      default:\n");
    sb.append("        break;\n");
    sb.append("    }\n");
    sb.append("    return list;\n");
    sb.append("  }\n\n");

    sb.append("  /** URL of the component's page in the generated online documentation. */\n");
    sb.append("  public static String docUrl(String component) {\n");
    sb.append("    String pkg = packageOf(component);\n");
    sb.append("    if (pkg.isEmpty()) {\n");
    sb.append("      return DOC_BASE_URL + \"Frontend-RTE.html\";\n");
    sb.append("    }\n");
    sb.append("    return DOC_BASE_URL + pkg.replace('.', '/') + \"/\" + component + \".html\";\n");
    sb.append("  }\n");
    sb.append("}\n");
    return sb.toString();
  }

  /** "|" separates the fields of a parameter entry, so it must not occur inside one. */
  static String bar(String s) {
    return s == null ? "" : s.replace("|", "/");
  }

  static String lit(String s) {
    if (s == null) {
      s = "";
    }
    StringBuilder sb = new StringBuilder("\"");
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\': sb.append("\\\\"); break;
        case '"': sb.append("\\\""); break;
        case '\n': sb.append("\\n"); break;
        case '\r': break;
        case '\t': sb.append("\\t"); break;
        default:
          if (c < 0x20 || c > 0x7e) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
      }
    }
    return sb.append('"').toString();
  }
}
