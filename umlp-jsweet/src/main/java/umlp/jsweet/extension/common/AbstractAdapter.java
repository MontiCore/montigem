/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import de.se_rwth.commons.logging.Log;
import org.jsweet.transpiler.Java2TypeScriptTranslator;
import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.CompilationUnitElement;
import org.jsweet.transpiler.model.ImportElement;
import umlp.jsweet.extension.util.UMLPAdapterUtils;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashMap;

/**
 * Combines common elements for all Adapters
 *
 * This class uses a "static library" to remap types and import from the rte
 */
public abstract class AbstractAdapter extends PrinterAdapter {
  private static final String UMLP_COMMONJ2TS_RTE_POINTER = "@umlp/commonj2ts";

  private final LinkedHashMap<String, String> staticLibrary = new LinkedHashMap<>();

  protected boolean isAsyncList = true;

  public AbstractAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    initStaticLibrary();

    Object asyncList = context.options.getConfiguration().get("asyncList");
    if (asyncList != null && "false".equals(asyncList.toString())) {
      isAsyncList = false;
    }
  }

  protected void initStaticLibrary() {}

  @Override
  public String needsImport(ImportElement importElement, String qualifiedName) {
    if (importElement.getImportedType() != null && staticLibrary.containsKey(qualifiedName)) {
      return getRootRelativeName(importElement.getImportedType());
    }
    return super.needsImport(importElement, qualifiedName);
  }

  @Override
  public ModuleImportDescriptor getModuleImportDescriptor(
      CompilationUnitElement currentCompilationUnit, String importedName,
      TypeElement importedClass) {
    String fqn = importedClass.getQualifiedName().toString();
    if (staticLibrary.containsKey(fqn)) {
      String staticClass = staticLibrary.get(fqn);
      return new ModuleImportDescriptor(false, (PackageElement) importedClass.getEnclosingElement(),
          UMLPAdapterUtils.getSimpleNameOfFQN(staticClass), getLibraryLocation(), importedClass);
    }
    return super.getModuleImportDescriptor(currentCompilationUnit, importedName, importedClass);
  }

  protected void addToStaticLibrary(String staticClass) {
    staticLibrary.put(staticClass, staticClass);
  }
  protected void putToStaticLibrary(String staticClass, String replaceClass) {
    staticLibrary.put(staticClass, replaceClass);
  }

  protected boolean containedInStaticLibrary(String type) {
    return this.staticLibrary.containsKey(type);
  }

  protected LinkedHashMap<String, String> getStaticLibrary() {
    return this.staticLibrary;
  }

  protected String getLibraryLocation() {
    return UMLP_COMMONJ2TS_RTE_POINTER;
  }

  @Override
  public Java2TypeScriptTranslator getPrinter() {
    if (super.getPrinter() instanceof Java2TypeScriptTranslator) {
      return (Java2TypeScriptTranslator) super.getPrinter();
    }
    else {
      Log.error("0JSC001 Java2TypeScriptTranslater must be core Adapter");
      return null;
    }
  }
}
