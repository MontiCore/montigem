package umlp.generator.gui.gentypescript.decorator;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.io.paths.MCPath;
import de.se_rwth.commons.Names;
import umlp.generator.domain.UMLPGeneratorConfig;
import umlp.generator.gui.GUIGeneratorUtil;
import umlp.generator.gui.gentypescript.printer.TSImportMap;
import umlp.generator.gui.gentypescript.printer.TypeScriptTypes;

import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TSImportHelper {

  protected Set<String> importSet = new LinkedHashSet<>();
  protected final GlobalExtensionManagement glex;
  protected final ASTCDType cdType;
  protected final MCPath hwPath;
  protected final Path outPath;
  protected final MCPath importsPath;

  public TSImportHelper(GlobalExtensionManagement glex, ASTCDType cdType, MCPath hwPath, Path outPath) {
    this(glex, cdType, hwPath, outPath, new MCPath());
  }

  public TSImportHelper(GlobalExtensionManagement glex, ASTCDType cdType, MCPath hwPath, Path outPath, MCPath importsPath) {
    this.glex = glex;
    this.cdType = cdType;
    this.hwPath = hwPath;
    this.outPath = outPath;
    this.importsPath = importsPath;
  }

  public void addImport(String importedQName, Optional<String> as,
      boolean importIntermediateAngularClass, String importerQName) {
    String alias = "";
    if (as.isPresent()) {
      alias = " as " + as.get();
    }
    Optional<String> tsImportPath = TypeScriptTypes.getTsImportPath(importedQName);
    // Optional doesn't exist in TypeScript
    if (importedQName.equals("java.util.Optional") || importedQName.isBlank()) {
      return;
    }
    if (tsImportPath.isPresent()) {
      // a type in a namespace (Temporal.PlainDate) imports the namespace
      String importedName = Iterables.getFirst(Splitter.on('.').split(TypeScriptTypes.toTSType(importedQName)), "");
      String importStr = "import { " + importedName + alias
          + " } from '" + tsImportPath.get() + "';";
      addImportString(importStr);
    }
    else if (!TypeScriptTypes.isDefault(importedQName)) {
      StringBuilder importBuilder = new StringBuilder();
      importBuilder.append("import { ");

      List<String> parts = Lists.newArrayList(Splitter.on('.').split(importedQName));
      String baseName = Iterables.getLast(Splitter.on('.').split(importedQName), importedQName);
      importBuilder.append(baseName);
      importBuilder.append(alias);

      importBuilder.append(" }");
      importBuilder.append(" from '");

      // generated path doesn't match package for domain classes
      // hardy.Flensburg.Human -> hardy/flensburg/Human
      int index = parts.size() - 2;
      if (index >= 0) {
        parts.set(index, parts.get(index).toLowerCase());
      }
      // case: a component in an app imports a component from the app
      String from = GUIGeneratorUtil.getInstance().qName2path(String.join(".", parts));
      if (!GUIGeneratorUtil.getInstance().isInitialized() || !GUIGeneratorUtil.getInstance().isDomainImport(importedQName)) {
        // case: a component imports currently generated other component, e.g.
        // "gen/gui/foo/ExampleComponent.ts" imports "gen/gui/foo/Example.ts" -> "./Example"
        // or: a class imports currently generated other class, e.g.
        // "gen/cd/SomeManager.ts" imports "gen/cd/commands/Some_create.ts"
        if (UMLPGeneratorConfig.getInstance().is4TSLib()) {
          Optional<String> importedPath = getRelativeQualifiedPath(
              Names.getQualifier(importerQName), Names.getQualifier(importedQName)
          );
          if (importedPath.isPresent()) {
            from = importedPath.get() + Names.getSimpleName(importedQName);
          }
        }
        if (importIntermediateAngularClass) {
          from += "Component";
        }
        // above case gets overruled if there is a HW path
        Optional<String> relativeFrom = getImportPath(
            hwPath, outPath, importsPath, importerQName, importedQName);
        if (relativeFrom.isPresent()) {
          from = relativeFrom.get();
        }
      }
      tsImportPath = TSImportMap.INSTANCE.getTSPackage(importedQName);

      Optional<String> componentImportPath = TSImportMap.INSTANCE.getTSPackage(importerQName);
      if (tsImportPath.isPresent() &&
          (componentImportPath.isEmpty() || !componentImportPath.get().equals(tsImportPath.get()))) {
        from = tsImportPath.get();
      }
      importBuilder.append(from);

      importBuilder.append("';");

      addImportString(importBuilder.toString());
    }
  }

  public void addImport(String importedQName, String importerQName) {
    addImport(importedQName, Optional.empty(), false, importerQName);
  }

  public void addImportString(String importStr) {
    if (!importSet.contains(importStr)) {
      importSet.add(importStr);
      glex.addAfterTemplate("umlp.gui2ts.TSEmptyImports", cdType,
          new StringHookPoint(importStr + "\n"));
    }
  }

  protected Optional<String> getImportPath(
      MCPath hwPath, Path outPath, MCPath importsPath,
      final String importerQName, final String importedQName
  ) {
    String importedQualifier = Names.getQualifier(importedQName);
    String importedSimpleName = Names.getSimpleName(importedQName);
    final String importedMaybeCDName;

    if (!importedQualifier.isBlank()) {
      List<String> parts = Splitter.on('.').splitToList(importedQualifier);
      // Lowercase the last part
      int lastIndex = parts.size() - 1;
      String lastPart = parts.get(lastIndex).toLowerCase();
      // Rejoin the parts with the modified last part
      importedMaybeCDName = Joiner.on('/').join(parts.subList(0, lastIndex))
          + (lastIndex > 0 ? "/" : "") + lastPart
          + "/" + importedSimpleName;
    }
    else {
      importedMaybeCDName = importedQualifier;
    }

    String importedMaybeCD = importedMaybeCDName + ".ts";
    String imported = Names.getPathFromPackage(importedQName) + ".ts";
    String importer = Names.getPathFromPackage(importerQName) + ".ts";

    Optional<URL> hwFilePath = hwPath.find(imported);
    if (hwFilePath.isEmpty()) {
      importedMaybeCD = importedMaybeCDName + ".java";
      imported = Names.getPathFromPackage(importedQName) + ".java";
      importer = Names.getPathFromPackage(importerQName) + ".java";
      hwFilePath = hwPath.find(imported);

      if (hwFilePath.isPresent()) {
        if (UMLPGeneratorConfig.getInstance().is4TSLib()) {
          // The java files are converted to TypeScript files in outDir, e.g.
          // src/main/java/montigem/types/Option.java -> <outDir>/montigem/types/Option.ts
          String ts = Names.getPathFromPackage(Names.getQualifier(importedQName).toLowerCase()
              + "." + Names.getSimpleName(importedQName)) + ".ts";
          return Optional.of(path2TSRelativeImport(outPath.resolve(importer).getParent()
              .toAbsolutePath()
              .normalize()
              .relativize(outPath.resolve(ts))
              .toString()
              .replaceAll("\\\\", "/")));
        }
        else {
          return Optional.of(path2TSRootImport(imported));
        }
      }
    }

    // case: TOP is used, use path to HW file, e.g.,
    // "gen/gui/ExampleComponent.ts" imports "hw/gui/Example.ts" -> "../../hw/gui/Example"
    if (hwFilePath.isPresent()) {
      if (UMLPGeneratorConfig.getInstance().is4TSLib()) {
        Optional<Path> hw = MCPath.toPath(hwFilePath.get());
        if (hw.isPresent()) {
          return Optional.of(path2TSRelativeImport(outPath.resolve(importer).getParent()
              .toAbsolutePath()
              .normalize()
              .relativize(hw.get().toAbsolutePath())
              .toString()
              .replaceAll("\\\\", "/")));
        }
      }
      else {
        return Optional.of(path2TSRootImport(imported));
      }
    }
    else {
      importedMaybeCD = importedMaybeCDName + ".ts";
      imported = Names.getPathFromPackage(importedQName) + ".ts";
      importer = Names.getPathFromPackage(importerQName) + ".ts";

      Optional<URL> importedURLOpt = importsPath.find(imported);
      Optional<URL> importedMaybeCDURLOpt = importsPath.find(importedMaybeCD);
      // case: external (generated) file must be imported, e.g.,
      // "gen/gui/Example.ts" imports "gen/cd/fetypes/Option.ts" -> "../cd/fetypes/Option"
      if (importedURLOpt.isPresent() || importedMaybeCDURLOpt.isPresent()) {
        if (UMLPGeneratorConfig.getInstance().is4TSLib()) {
          Optional<Path> importedPath;
          // prefer "maybe CD" option
          if (importedMaybeCDURLOpt.isPresent()) {
            importedPath = MCPath.toPath(importedMaybeCDURLOpt.get());
          }
          else {
            importedPath = MCPath.toPath(importedURLOpt.get());
          }
          if (importedPath.isPresent()) {
            Path fromPath = outPath.resolve(importer);
            // Calculate the relative path
            return Optional.of(path2TSRelativeImport(fromPath.getParent()
                .normalize()
                .relativize(importedPath.get())
                .toString()
                .replaceAll("\\\\", "/")));
          }
        }
        else {
          if (importedMaybeCDURLOpt.isPresent()) {
            return Optional.of(path2TSRootImport(importedMaybeCD));
          }
          else {
            return Optional.of(path2TSRootImport(imported));
          }
        }
      }
      else {
        importedMaybeCD = importedMaybeCDName + ".java";
        imported = Names.getPathFromPackage(importedQName) + ".java";
        importer = Names.getPathFromPackage(importerQName) + ".java";

        importedURLOpt = importsPath.find(imported);
        importedMaybeCDURLOpt = importsPath.find(importedMaybeCD);

        if (importedURLOpt.isPresent() || importedMaybeCDURLOpt.isPresent()) {
          if (UMLPGeneratorConfig.getInstance().is4TSLib()) {
            Optional<Path> importedPath;
            // prefer "maybe CD" option
            if (importedMaybeCDURLOpt.isPresent()) {
              importedPath = MCPath.toPath(importedMaybeCDURLOpt.get());
            }
            else {
              importedPath = MCPath.toPath(importedURLOpt.get());
            }
            if (importedPath.isPresent()) {
              Path fromPath = outPath.resolve(importer);
              // Calculate the relative path
              return Optional.of(path2TSRelativeImport(fromPath.getParent()
                  .normalize()
                  .relativize(importedPath.get())
                  .toString()
                  .replaceAll("\\\\", "/")));
            }
          }
          else {
            if (importedMaybeCDURLOpt.isPresent()) {
              return Optional.of(path2TSRootImport(importedMaybeCD));
            }
            else {
              return Optional.of(path2TSRootImport(imported));
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  public static Optional<String> getRelativeQualifiedPath(String importer, String imported) {
    String[] fullParts = imported.split("\\.");
    String[] candidateParts = importer.split("\\.");

    int commonLength = 0;
    while (commonLength < fullParts.length
        && commonLength < candidateParts.length
        && fullParts[commonLength].equals(candidateParts[commonLength])) {
      commonLength++;
    }

    if (commonLength == 0) {
      return Optional.empty(); // No shared root at all
    }

    StringBuilder path = new StringBuilder("./");

    // Add ".." for each extra level in importer
    for (int i = commonLength; i < candidateParts.length; i++) {
      path.append("../");
    }

    // Add remaining parts from imported
    for (int i = commonLength; i < fullParts.length; i++) {
      path.append(fullParts[i]);
      path.append('/');
    }

    // TODO AGe: remove toLowerCase when corresponding umlp trafo is fixed
    return Optional.of(path.toString().toLowerCase());
  }

  protected String path2TSRelativeImport(String relativePath) {
    if (!relativePath.startsWith("./")) {
      // Same folder, prepend "./"
      relativePath = "./" + relativePath;
    }
    return path2TSRootImport(relativePath);
  }

  protected String path2TSRootImport(String rootPath) {
    if (rootPath.endsWith(".ts")) {
      // Remove file extension
      rootPath = rootPath.replace(".ts", "");
    }
    if (rootPath.endsWith(".java")) {
      // Remove file extension
      rootPath = rootPath.replace(".java", "");
    }
    return rootPath.replaceAll("\\\\", "/");
  }

}
