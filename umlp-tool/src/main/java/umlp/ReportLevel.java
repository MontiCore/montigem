/* (c) https://github.com/MontiCore/monticore */
package umlp;

import java.util.Locale;

/**
 * Which MontiCore reports the generators write, see {@link UMLPToolOptions#REPORT_LEVEL}.
 */
public enum ReportLevel {
  /**
   * All reports, about 18 files per model.
   */
  ALL,

  /**
   * Only the IncGen report that the incremental Gradle build reads.
   */
  INCGEN;

  /**
   * The level for an option value, e.g. {@code incgen}. Case does not matter.
   */
  public static ReportLevel of(String value) {
    return valueOf(value.trim().toUpperCase(Locale.ROOT));
  }
}
