/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.gui;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.CompilationUnitElement;
import org.jsweet.transpiler.model.ImportElement;
import umlp.jsweet.extension.common.AbstractAdapter;
import umlp.jsweet.extension.util.UMLPAdapterUtils;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static umlp.jsweet.extension.util.GUIAdapterUtils.AMODULE_ANNOTATION;

/**
 * Adapter for delegating GUI type imports to the correct package.
 */
public class GUITypesImportAdapter extends AbstractAdapter {
  public GUITypesImportAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    initTypeMappings();
  }

  private void initTypeMappings() {
    addTypeMapping(
        (TypeMirror type) -> {
          for (TSImportMap value : TSImportMap.values()) {
            if (type.toString().startsWith(value.getJavaPackage())
                && !getCompilationUnit().getPackage().getQualifiedName().toString().startsWith(value.getJavaPackage())
            ) {
              return UMLPAdapterUtils.getSimpleNameOfFQN(type.toString());
            }
          }
          return null;
        }
    );
  }

  @Override
  public String needsImport(ImportElement importElement, String qualifiedName) {
    if (importElement.getImportedType() != null) {
      for (TSImportMap value : TSImportMap.values()) {
        if (qualifiedName.startsWith(value.getJavaPackage())
            && !getCompilationUnit().getPackage().getQualifiedName().toString().startsWith(value.getJavaPackage())
        ) {
          return getRootRelativeName(importElement.getImportedType());
        }
      }
    }
    return super.needsImport(importElement, qualifiedName);
  }

  @Override
  public ModuleImportDescriptor getModuleImportDescriptor(
      CompilationUnitElement currentCompilationUnit, String importedName,
      TypeElement importedClass) {
    if (getContext().hasAnnotationType(importedClass, AMODULE_ANNOTATION)) {
      // Similar to @Module, just for annotations
      return new ModuleImportDescriptor(false,
          (PackageElement) importedClass.getEnclosingElement(), importedName,
          getContext().getAnnotationValue(importedClass, AMODULE_ANNOTATION, String.class, "##missing##"), importedClass);
    }
    String fqn = importedClass.getQualifiedName().toString();
    if (fqn != null) {
      for (TSImportMap value : TSImportMap.values()) {
        if (fqn.startsWith(value.getJavaPackage())
            && !getCompilationUnit().getPackage().getQualifiedName().toString().startsWith(value.getJavaPackage())
        ) {
          return new ModuleImportDescriptor(false,
              (PackageElement) importedClass.getEnclosingElement(), importedName,
              value.getTSPackage(), importedClass);
        }
      }

    }
    return super.getModuleImportDescriptor(currentCompilationUnit, importedName, importedClass);
  }
}
