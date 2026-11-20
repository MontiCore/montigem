/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.gui;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.CompilationUnitElement;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.ImportElement;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.model.support.ExtendedElementSupport;
import standalone.com.sun.tools.javac.tree.JCTree;
import umlp.jsweet.extension.common.AbstractAdapter;
import umlp.jsweet.extension.util.GUIAdapterUtils;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashMap;
import java.util.Map;

public class GUIUtilAdapter extends AbstractAdapter {

  Map<String, String> utilClasses = new LinkedHashMap();

  public GUIUtilAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
//    utilClasses.put("umlp.guiutil.Router", "@angular/router");
    utilClasses.put("umlp.guiutil.Location", "@angular/common");
  }

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    if (invocation.getTargetType() == null) {
      return super.substituteMethodInvocation(invocation);
    }

    String targetType = invocation.getTargetType().toString();
    String methodName = invocation.getMethodName();

    switch (targetType) {
      case "umlp.guiutil.Router":
        switch (methodName) {
          case "navigate":
            this.print("this._router.navigate([").printArgList(invocation.getArguments())
                .print("])");
            return true;
        }
      case "umlp.guiutil.Location":
        switch (methodName) {
          case "back":
            this.print("this.location.back()");
            return true;
        }
      case "umlp.guiutil.HTMLUtils":
        switch (methodName) {
          case "alert":
            this.print("alert(").printArgList(invocation.getArguments()).print(")");
            return true;
        }
      case "umlp.guiutil.GUIUtils":
        switch (methodName) {
          case "update":
            ExtendedElement argument = invocation.getArgument(0);
            if (argument instanceof ExtendedElementSupport &&
                ((ExtendedElementSupport<?>) argument).getTree() instanceof JCTree.JCLambda &&
                ((JCTree.JCLambda) ((ExtendedElementSupport<?>) argument).getTree()).getBody() instanceof JCTree.JCMethodInvocation) {
              JCTree.JCMethodInvocation body = (JCTree.JCMethodInvocation) ((JCTree.JCLambda) ((ExtendedElementSupport<?>) argument).getTree()).getBody();

              // This method is used for manually triggering an update of a method that depends only on Java local class fields
              getPrinter().print("/*You must only use methods for which a 'ForAngular' pendent exists in the generated TOP Class*/");
              getPrinter().print(body.getMethodSelect()).print("ForAngular"+GUIAdapterUtils.UPDATE_SUFFIX + "(").printArgList(body).print(")");
            }
            return true;
          case "registerObserverToDomainObject":
            print(GUIComponentObserverPatternAdapter.DOMAIN_OBSERVER_FUNCTION_NAME + "(")
                .printArgList(invocation.getArguments()).print(")");
            return true;
          case "registerObserverToStateVariable":
            print(GUIComponentObserverPatternAdapter.STATEVAR_OBSERVER_FUNCTION_NAME + "(")
                .printArgList(invocation.getArguments()).print(")");
            return true;
          case "ignoreUpdatesOnDomainObject":
          case "ignoreUpdatesOnStateVar":
            printArgList(invocation.getArguments());
            return true;
        }
    }

    return super.substituteMethodInvocation(invocation);
  }


  @Override
  public String needsImport(ImportElement importElement, String qualifiedName) {
    if (importElement.getImportedType() != null) {
      for (String utilClass : utilClasses.keySet()) {
        if (qualifiedName.equals(utilClass)) {
          return getRootRelativeName(importElement.getImportedType());
        }
      }
    }
    return super.needsImport(importElement, qualifiedName);
  }

  @Override
  public ModuleImportDescriptor getModuleImportDescriptor(
      CompilationUnitElement currentCompilationUnit, String importedName, TypeElement importedClass) {
    String fqn = importedClass.getQualifiedName().toString();
    if (fqn != null) {
      for (String utilClass : utilClasses.keySet()) {
        if (fqn.equals(utilClass)) {
          return new ModuleImportDescriptor(false,
              (PackageElement) importedClass.getEnclosingElement(), importedName,
              utilClasses.get(utilClass), importedClass);
        }
      }
    }
    return super.getModuleImportDescriptor(currentCompilationUnit, importedName, importedClass);
  }

}
