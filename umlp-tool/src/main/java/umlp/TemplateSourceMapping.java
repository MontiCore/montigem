/* (c) https://github.com/MontiCore/monticore */

package umlp;

import de.se_rwth.commons.logging.Log;

import java.util.Locale;

/**
 * Whether source maps for template generated files should be generated,
 * see {@link UMLPToolOptions#TEMPL_SOURCE_MAPPING}
 */
public enum TemplateSourceMapping {

  /**
   * No .map files should be generated for template based files.
   */
  OFF,

  /**
   * The content of the templates is not included in the produced .map files.
   */
  NO_SOURCES,

  /**
   * The content of the templates is included in the produced .map files.
   */
  INCLUDE_SOURCES;

  public static TemplateSourceMapping of(String value){
    if(value == null) {
      Log.warn("0xA0149 [null] provided as value for TemplateSourceMapping, fallback to OFF.");
      return OFF;
    }

    String normalized = value.trim().toLowerCase(Locale.ROOT);
    switch (normalized){
      case "true":
      case "include_sources":
        return INCLUDE_SOURCES;
      case "no_sources":
        return NO_SOURCES;
      case "false":
      case "off":
        return OFF;
      default:
        Log.error("0xA0150 Invalid value '" + value + "' for template source mapping. "
        + "Expected one of: 'true', 'false', 'include_sources', 'no_sources', 'off'.");
        return OFF;
    }
  }
}
