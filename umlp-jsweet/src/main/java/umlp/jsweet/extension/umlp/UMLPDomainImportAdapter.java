/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.umlp;

import com.google.common.collect.FluentIterable;
import de.se_rwth.commons.Splitters;
import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.CompilationUnitElement;
import org.jsweet.transpiler.model.ImportElement;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class UMLPDomainImportAdapter extends PrinterAdapter {
  private static final String DOMAIN_LOCATION = "";

  private Set<String> domainNames = new LinkedHashSet<>();

  public UMLPDomainImportAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    this.setDomainName();
  }

  // Overrides the needsImport method of PrinterAdapter to customize the required imports for domain names.
  @Override
  public String needsImport(ImportElement importElement, String qualifiedName) {
    if (importElement.getImportedType() != null && startsWithDomainName(qualifiedName)) {
      return getRootRelativeName(importElement.getImportedType());
    }
    return super.needsImport(importElement, qualifiedName);
  }

  // Retrieves the module import descriptor. It provides information about modules that need to be imported for domain names.
  @Override
  public ModuleImportDescriptor getModuleImportDescriptor(
      CompilationUnitElement currentCompilationUnit, String importedName,
      TypeElement importedClass) {
    String fqn = importedClass.getQualifiedName().toString();
    if (fqn != null && startsWithDomainName(fqn)) {
      if (super.getModuleImportDescriptor(currentCompilationUnit, importedName, importedClass) == null) {
        return null;
      }
      return new ModuleImportDescriptor(false,
          (PackageElement) importedClass.getEnclosingElement(), importedName,
          getDomainLocation(importedClass), importedClass);
    }
    return super.getModuleImportDescriptor(currentCompilationUnit, importedName, importedClass);
  }

  // Checks if the fully qualified name (fqn) starts with one of the domain names.
  private boolean startsWithDomainName(String fqn) {
    return domainNames.contains(FluentIterable.from(Splitters.DOT.split(fqn)).first().get());
  }

  private String getDomainLocation(TypeElement importedClass) {
    String loc = importedClass.getQualifiedName().toString().replace(".", "/");
    return DOMAIN_LOCATION + loc;
  }

  // Allows adding domain names to the domainNames set.
  protected final void addDomainName(String... domainName) {
    this.domainNames.addAll(Arrays.asList(domainName));
  }

  // Abstract method to set the domain name. This method must be implemented by all subclasses of UMLPDomainImportAdapter.
  protected abstract void setDomainName();
}
