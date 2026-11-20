/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.gui;

import org.jsweet.transpiler.extension.PrinterAdapter;
import umlp.jsweet.extension.common.AbstractAdapter;

import javax.lang.model.type.TypeMirror;
import java.util.function.Function;

public class TSTypesAdapter extends AbstractAdapter {
  private static final String ANGULAR_CORE_PATH = "@angular/core";

  public TSTypesAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    this.initTypeMappings();
  }

  @Override
  protected void initStaticLibrary() {
    putToStaticLibrary("de.monticore.guidsl.types.GUIViewElement", "TemplateRef");
  }

  private void initTypeMappings() {
    getTypeMirrorMappings().add(0, computeGUITypeMapping());
  }

  private Function<TypeMirror, String> computeGUITypeMapping() {
    return (TypeMirror type) -> {
      switch (type.toString()) {
        case "de.monticore.guidsl.types.GUIViewElement":
          return "TemplateRef<any>";
      }
      return null;
    };
  }

  @Override
  protected String getLibraryLocation() {
    return ANGULAR_CORE_PATH;
  }
}
