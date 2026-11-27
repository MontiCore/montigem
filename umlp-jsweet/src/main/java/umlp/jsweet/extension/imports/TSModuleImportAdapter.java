/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.imports;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.CompilationUnitElement;
import org.jsweet.transpiler.model.ImportElement;
import umlp.jsweet.extension.common.AbstractAdapter;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

import static umlp.jsweet.extension.util.GUIAdapterUtils.AMODULE_ANNOTATION;
import static umlp.jsweet.extension.util.GUIAdapterUtils.TS_MODULE_ANNOTATION;

/**
 * Imports types from the TypeScript module they are declared to belong to.
 * <p>
 * The module is declared with {@code @TSModule}, either on the type or on one of its enclosing
 * packages (in a {@code package-info.java}), e.g., {@code @TSModule("@umlp/basic")} on
 * {@code mc.fenix.basic}. Annotation types use {@code @AnnotationModule} instead.
 */
public class TSModuleImportAdapter extends AbstractAdapter {

  /**
   * A TypeScript module and the Java package whose types (including subpackages) belong to it.
   */
  protected record TSModuleRef(String module, String javaScope) {
  }

  public TSModuleImportAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    // print imported types by their simple name; generic types are left to JSweet,
    // as a type mapping replaces the type including its type arguments
    addTypeMapping(
        (TypeMirror type) -> {
          Element element = types().asElement(type);
          if (element instanceof TypeElement typeElement
              && typeElement.getTypeParameters().isEmpty()
              && findExternalTSModule(typeElement).isPresent()) {
            return typeElement.getSimpleName().toString();
          }
          return null;
        }
    );
  }

  @Override
  public String needsImport(ImportElement importElement, String qualifiedName) {
    TypeElement importedType = importElement.getImportedType();
    if (importedType != null && findExternalTSModule(importedType).isPresent()) {
      return getRootRelativeName(importedType);
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
    Optional<TSModuleRef> tsModule = findExternalTSModule(importedClass);
    if (tsModule.isPresent()) {
      return new ModuleImportDescriptor(false,
          (PackageElement) importedClass.getEnclosingElement(), importedName,
          tsModule.get().module(), importedClass);
    }
    return super.getModuleImportDescriptor(currentCompilationUnit, importedName, importedClass);
  }

  /**
   * The TypeScript module the given type is imported from, unless the type belongs to the same
   * module as the compilation unit currently printed (then it is imported like any local type).
   */
  protected Optional<TSModuleRef> findExternalTSModule(TypeElement type) {
    String importerPackage = getCompilationUnit().getPackage().getQualifiedName().toString();
    return findTSModule(type).filter(ref -> !isInScope(importerPackage, ref.javaScope()));
  }

  /**
   * The {@code @TSModule} of the type itself, or else of its nearest enclosing package.
   * The scope of a module declared on a type is the type's package.
   */
  protected Optional<TSModuleRef> findTSModule(TypeElement type) {
    PackageElement typePackage = util().getParentElement(type, PackageElement.class);
    Optional<String> typeModule = getTSModule(type);
    if (typeModule.isPresent() && typePackage != null) {
      return Optional.of(new TSModuleRef(typeModule.get(), typePackage.getQualifiedName().toString()));
    }
    for (PackageElement pkg = typePackage; pkg != null; pkg = util().getParentPackage(pkg)) {
      Optional<String> packageModule = getTSModule(pkg);
      if (packageModule.isPresent()) {
        return Optional.of(new TSModuleRef(packageModule.get(), pkg.getQualifiedName().toString()));
      }
    }
    return Optional.empty();
  }

  protected Optional<String> getTSModule(Element element) {
    if (!getContext().hasAnnotationType(element, TS_MODULE_ANNOTATION)) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        getContext().getAnnotationValue(element, TS_MODULE_ANNOTATION, String.class, null));
  }

  /**
   * @return whether the package {@code pkg} is {@code scope} or nested in it
   */
  protected static boolean isInScope(String pkg, String scope) {
    return pkg.equals(scope) || pkg.startsWith(scope + ".");
  }
}
