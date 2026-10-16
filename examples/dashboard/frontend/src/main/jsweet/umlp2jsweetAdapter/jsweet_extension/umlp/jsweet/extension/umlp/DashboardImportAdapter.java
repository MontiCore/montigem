/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.umlp;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.CompilationUnitElement;
import org.jsweet.transpiler.model.ImportElement;
import org.jsweet.transpiler.model.NewClassElement;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Imports the classes of the dashboard module (package dashboardmodule) from its npm library.
 * Workaround, as the tooling does not yet map the domain of a module to the npm library of the module.
 * (Keep in sync with the copy in gui2jsweetAdapter)
 */
public class DashboardImportAdapter extends DashboardImportAdapterTOP {

  protected static final String MODULE_PACKAGE = "dashboardmodule";
  protected static final String MODULE_LIBRARY = "@montigem/dashboard";

  public DashboardImportAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    // Fully qualified usages (e.g., casts in generated code) are resolved via a namespace import
    addTypeMapping((TypeMirror type) -> {
      if (isModuleClass(type.toString())) {
        importModuleNamespace();
      }
      return null;
    });
  }

  @Override
  public String needsImport(ImportElement importElement, String qualifiedName) {
    if (importElement.getImportedType() != null && isModuleClass(qualifiedName)) {
      return getRootRelativeName(importElement.getImportedType());
    }
    return super.needsImport(importElement, qualifiedName);
  }

  @Override
  public ModuleImportDescriptor getModuleImportDescriptor(
      CompilationUnitElement currentCompilationUnit, String importedName,
      TypeElement importedClass) {
    if (isModuleClass(importedClass.getQualifiedName().toString())) {
      return new ModuleImportDescriptor(false,
          (PackageElement) importedClass.getEnclosingElement(), importedName,
          MODULE_LIBRARY, importedClass);
    }
    return super.getModuleImportDescriptor(currentCompilationUnit, importedName, importedClass);
  }

  @Override
  public boolean substituteNewClass(NewClassElement newClass) {
    // e.g., "new dashboardmodule.CellConfigDeSer()" in generated code
    if (newClass.getConstructor() != null
        && newClass.getConstructor().getEnclosingElement() instanceof TypeElement
        && isModuleClass(((TypeElement) newClass.getConstructor().getEnclosingElement()).getQualifiedName().toString())) {
      importModuleNamespace();
    }
    return super.substituteNewClass(newClass);
  }

  protected void importModuleNamespace() {
    addHeader("import." + MODULE_PACKAGE, "import * as " + MODULE_PACKAGE + " from '" + MODULE_LIBRARY + "';");
  }

  protected boolean isModuleClass(String qualifiedName) {
    return qualifiedName != null && qualifiedName.startsWith(MODULE_PACKAGE + ".");
  }
}
