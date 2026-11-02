package aas.decorator;

import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.se_rwth.commons.logging.Log;
import java.util.HashMap;
import static aas.AAS2UMLPGenerator.NAMETRANSFORMATIONS;

public class NameTransformationDecorator {
  private GlobalExtensionManagement glex;

  public NameTransformationDecorator(GlobalExtensionManagement glex) {
    this.glex = glex;
  }

  public String decorate() {
    StringBuilder builder = new StringBuilder();
    if (glex == null) {
      Log.error("glex is null.");
      return "";
    }

    if (!glex.hasGlobalVar(NAMETRANSFORMATIONS)) {
      Log.error("glex has no name transformation global variable.");
      return "";
    }

    HashMap<String,String> map = ((HashMap<String, String>) glex.getGlobalVar(NAMETRANSFORMATIONS));

    if (!map.isEmpty()) {
      for (String key : map.keySet()) {
        builder.append(key)
            .append(": ")
            .append(map.get(key))
            .append("\n");
      }

      builder.deleteCharAt(builder.length() - 1);
    }

    return builder.toString();
  }
}
