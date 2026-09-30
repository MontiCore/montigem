/* (c) https://github.com/MontiCore/monticore */
package de.montigem.gradle.specific;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The settings of one JSweet transpilation. {@link Java2TSAction} passes them as strings into the warm JSweet
 * class loader, {@link JSweetInvoker} reads them there. Only JDK types, the class is loaded on both sides.
 */
public class JSweetSettings {

  private static final String BASE = "--base";
  private static final String WORKING = "--working";
  private static final String OUT = "--out";
  private static final String CANDIES = "--candies";
  private static final String CLASSPATH = "--classpath";
  private static final String NODE_VERSION = "--node-version";
  private static final String VERBOSE = "--verbose";
  private static final String SOURCE_MAPS = "--source-maps";
  private static final String INPUT = "--input";
  private static final String EXCLUDE = "--exclude";

  public File base;
  public File working;
  public File out;
  public File candies;
  public String classPath;
  public String nodeVersion;
  public boolean verbose;
  public boolean sourceMaps;
  public final List<File> input = new ArrayList<>();
  public final List<File> excluded = new ArrayList<>();

  public String[] toArgs() {
    List<String> args = new ArrayList<>(List.of(
        BASE, base.getPath(),
        WORKING, working.getPath(),
        OUT, out.getPath(),
        CANDIES, candies.getPath(),
        CLASSPATH, classPath,
        NODE_VERSION, nodeVersion,
        VERBOSE, String.valueOf(verbose),
        SOURCE_MAPS, String.valueOf(sourceMaps)));
    input.forEach(f -> args.addAll(List.of(INPUT, f.getPath())));
    excluded.forEach(f -> args.addAll(List.of(EXCLUDE, f.getPath())));
    return args.toArray(String[]::new);
  }

  public static JSweetSettings parse(String[] args) {
    if (args.length % 2 != 0) {
      throw new IllegalArgumentException("JSweet arguments come in pairs, got " + args.length);
    }
    JSweetSettings s = new JSweetSettings();
    for (int i = 0; i < args.length; i += 2) {
      String value = args[i + 1];
      switch (args[i]) {
        case BASE -> s.base = new File(value);
        case WORKING -> s.working = new File(value);
        case OUT -> s.out = new File(value);
        case CANDIES -> s.candies = new File(value);
        case CLASSPATH -> s.classPath = value;
        case NODE_VERSION -> s.nodeVersion = value;
        case VERBOSE -> s.verbose = Boolean.parseBoolean(value);
        case SOURCE_MAPS -> s.sourceMaps = Boolean.parseBoolean(value);
        case INPUT -> s.input.add(new File(value));
        case EXCLUDE -> s.excluded.add(new File(value));
        default -> throw new IllegalArgumentException("Unknown JSweet argument " + args[i]);
      }
    }
    return s;
  }
}
