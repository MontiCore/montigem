/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.module;

import org.apache.commons.lang3.StringUtils;
import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.CompilationUnitElement;
import org.jsweet.transpiler.model.ImportElement;
import org.jsweet.transpiler.model.TypeCastElement;
import umlp.jsweet.extension.util.GUIAdapterUtils;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RelativeImportAdapter extends PrinterAdapter {

  private final GUIAdapterUtils adapterUtils;

  public RelativeImportAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    this.adapterUtils = new GUIAdapterUtils(this, types(), context, util());
  }

  @Override
  public ModuleImportDescriptor getModuleImportDescriptor(
      CompilationUnitElement compilationUnit, String importedName,
      TypeElement importedClass
  ) {
    String qName = importedClass.getQualifiedName().toString();
    ModuleImportDescriptor res = super.getModuleImportDescriptor(compilationUnit, importedName, importedClass);

    // erased classes (e.g., backend-only helpers) are not transpiled, thus there is nothing to import
    if (qName != null && !context.hasAnnotationType(importedClass, JSweetConfig.ANNOTATION_ERASED)) {
      Optional<String> tsPath = getRelativePath(
          compilationUnit.getPackage().getQualifiedName().toString(), qName);
      if (tsPath.isPresent()) {
        return new ModuleImportDescriptor(false,
            (PackageElement) importedClass.getEnclosingElement(), importedName,
            tsPath.get(), importedClass);
      }
      else {
        Optional<String> predictedPath = predictRelativePath(
            compilationUnit.getPackage().getQualifiedName().toString(), qName);
        if (predictedPath.isPresent()) {
          return new ModuleImportDescriptor(false,
              (PackageElement) importedClass.getEnclosingElement(), importedName,
              predictedPath.get(), importedClass);
        }
      }
    }
    return res;
  }

  @Override
  public boolean substituteType(TypeElement type) {
    // no type substitution, just adding manager to component classes
    if (adapterUtils.isHWCComponentOrDirective(type)) {
      String cdName = adapterUtils.getAnnotationPropertyValue(type, "cdName", String.class, "");
      if (!cdName.isEmpty()) {
        int lastDot;
        lastDot = cdName.lastIndexOf('.');
        String simpleName = (lastDot == -1) ? cdName : cdName.substring(lastDot + 1);
        String managerName = simpleName + "Manager";
        String managerQName = StringUtils.lowerCase(cdName) + "." + managerName;

        String typeQName = type.getQualifiedName().toString();
        lastDot = typeQName.lastIndexOf('.');
        String typeQ = (lastDot == -1) ? typeQName : typeQName.substring(0, lastDot);
        Optional<String> managerPath = getRelativePath(typeQ, managerQName);
        if (managerPath.isPresent()) {
          adapterUtils.importFromWithModule(managerName, managerPath.get());
        }
      }
    }
    return super.substituteType(type);
  }

  public Optional<String> getRelativePath(String importerQ, String importedQName) {
    String importerPath = context.options.getTsOutputDir().getAbsolutePath()
        + "/" + importerQ.replace('.', '/');
    String relativePath = importedQName.replace('.', '/') + ".ts";
    Path importer = Paths.get(importerPath);
    for (String path : getLocalPaths()) {
      Path srcPath = Paths.get(path);
      Path p = srcPath.resolve(relativePath);
      if (Files.exists(p)) {
        return Optional.of("./" + importer.relativize(p).normalize()
            .toString()
            .replaceAll("\\\\", "/")
            .replaceAll("\\.ts", "")
        );
      } else if(srcPath.isAbsolute() && srcPath.endsWith(relativePath)) { // We assume the srcPath is the file we want to import
        return Optional.of("./" + importer.relativize(srcPath).normalize()
            .toString()
            .replaceAll("\\\\", "/")
            .replaceAll("\\.ts", "")
        );
      }
    }
    return Optional.empty();
  }

  public Optional<String> predictRelativePath(String importerQ, String importedQName) {
    String relativePath = importedQName.replace('.', '/') + ".java";
    for (String path : getLocalPaths()) {
      Path p = Paths.get(path).resolve(relativePath);
      if (Files.exists(p)) {
        String[] importedParts = importedQName.split("\\.");
        String importedSimpleName;
        String[] importedQualifier;
        if (importedParts.length == 0) {
          return Optional.empty();
        }
        else if (importedParts.length == 1) {
          importedQualifier = new String[0];
          importedSimpleName = importedParts[importedParts.length - 1];
        }
        else {
          importedQualifier = Arrays.copyOf(importedParts, importedParts.length - 1);
          importedSimpleName = importedParts[importedParts.length - 1];
        }
        String[] importerQualifier = importerQ.split("\\.");

        int commonLength = 0;
        while (commonLength < importedQualifier.length
            && commonLength < importerQualifier.length
            && importedQualifier[commonLength].equals(importerQualifier[commonLength])) {
          commonLength++;
        }

        StringBuilder pathSB = new StringBuilder("./");

        // Add ".." for each extra level in importer
        for (int i = commonLength; i < importerQualifier.length; i++) {
          pathSB.append("../");
        }

        // Add remaining parts from imported
        for (int i = commonLength; i < importedQualifier.length; i++) {
          pathSB.append(importedQualifier[i]);
          pathSB.append('/');
        }
        pathSB.append(importedSimpleName);
        return Optional.of(pathSB.toString());
      }
    }
    return Optional.empty();
  }

  protected List<String> getLocalPaths() {
    List<String> res = new ArrayList<>();
    if (context.options.getConfiguration().containsKey("srcDirs")) {
      List<?> absolutePaths = (List<?>) context.options.getConfiguration().get("srcDirs");
      for (Object absolutePath : absolutePaths) {
        if (absolutePath instanceof String) {
          res.add((String) absolutePath);
        }
      }
    }
    return res;
  }

}
