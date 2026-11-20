package umlp.jsweet.extension.imports;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.CompilationUnitElement;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.HashSet;
import java.util.Set;

/**
 * An adapter to prevent Typescript import errors at runtime.
 * It uses the referenceAnalyzer to decide if an import can be printed at the bottom of the ts file.
 */
public class PrintImportsAtBottomAdapter extends PrinterAdapter {
  public PrintImportsAtBottomAdapter(PrinterAdapter printerAdapter) {
    super(printerAdapter);
  }

  Set<String> alreadyImported = new HashSet<>();

  @Override
  public ModuleImportDescriptor getModuleImportDescriptor(CompilationUnitElement currentCompilationUnit, String importedName, TypeElement importedClass) {
    ModuleImportDescriptor descriptor = super.getModuleImportDescriptor(currentCompilationUnit, importedName, importedClass);
    if(descriptor == null){
      return null;
    }

    var printer = getPrinter();
    var path = descriptor.getPathToImportedClass();
    String key = importedName + "@" + path;
    String lookupKey = currentCompilationUnit.getSourceFilePath() + "@" + key;
    if(alreadyImported.contains(lookupKey)){
      return null;
    }

    alreadyImported.add(lookupKey);

    if(path.startsWith("@")){
      return descriptor;
    }

    if (printer.getContext().referenceAnalyzer != null
        && printer.getContext().referenceAnalyzer.isDependent(printer.getCompilationUnit(), importedClass)) {
      return new ModuleImportDescriptor(false,
          descriptor.getTargetPackage() != null
              ? descriptor.getTargetPackage()
              : (PackageElement) importedClass.getEnclosingElement(),
          descriptor.getImportedName() != null ? descriptor.getImportedName() : importedName,
          path, importedClass);
    } else {
      printer.getContext().addTopFooterStatement("import." + key,
          "import { " + importedName + " } from '" + path + "';\n");
      printer.getContext().registerImportedName(printer.getCompilationUnit().getSourceFile().getName(), importedClass, importedName);
      return null;
    }
  }
}
