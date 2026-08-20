/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.gui;

import de.se_rwth.commons.logging.Log;
import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.MethodInvocationElement;
import standalone.com.sun.source.tree.MethodTree;
import standalone.com.sun.source.tree.Tree;
import umlp.jsweet.extension.AbstractAdapter;
import umlp.jsweet.extension.util.GUIAdapterUtils;
import umlp.jsweet.extension.util.RouterAdapterUtils;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapting generated GUI components for angular
 */
public class AngularSpecificsAdapter extends AbstractAdapter {
  private final GUIAdapterUtils adapterUtils;

  public AngularSpecificsAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    this.adapterUtils = new GUIAdapterUtils(this, types(), context, util());
  }

  @Override
  public void beforeTypeBody(TypeElement type) {
    if (adapterUtils.hasDirectiveAnnotation(type)) {
      println().printIndent().print("protected _router: Router;").println().println();
    }
    super.beforeTypeBody(type);
  }

  @Override
  public boolean substituteType(TypeElement classElement) {
    if (classElement.getSimpleName().toString().equals(RouterAdapterUtils.APP_ROUTER)) {
      print("import { Injectable, inject } from '@angular/core';").println();
      print("import { Router } from '@angular/router';").println().println();
    }

    addAngularComponentAnnotations(classElement);
    return super.substituteType(classElement);
  }

  /**
   * Angular Router
   * @param executable the executable being printed
   * @return
   */
  @Override
  public boolean substituteExecutable(ExecutableElement executable) {
    Element enclosingElement = executable.getEnclosingElement();
    String className = enclosingElement.getSimpleName().toString();
    if (className.equals(RouterAdapterUtils.APP_ROUTER)) {
      String methodName = executable.getSimpleName().toString();

      if (methodName != null && methodName.startsWith(RouterAdapterUtils.APP_ROUTER_NAVIGATE_PREFIX)) {
        for (Modifier modifier : executable.getModifiers()) {
          if (modifier == Modifier.PUBLIC || modifier == Modifier.PROTECTED ||
              modifier == Modifier.PRIVATE || modifier == Modifier.STATIC) {
            print(modifier.toString()).print(" ");
          }
        }

        print(methodName).print("(");
        List<? extends VariableElement> parameters = executable.getParameters();
        if (!parameters.isEmpty()) {
          for (VariableElement param : parameters) {
            print(param.getSimpleName().toString() + ": ");
            print(getMappedType(param.asType()) + ", ");
          }
        }
        print(RouterAdapterUtils.ROUTER_VAR + ":" + RouterAdapterUtils.ROUTER + ")");

        if (!executable.getReturnType().toString().equals("void")) {
          print(": ").print(getMappedType(executable.getReturnType()));
        }

        Tree methodTree = util().lookupTree(context, executable);
        if (methodTree instanceof MethodTree method) {
          if (method.getBody() != null) {
            print(method.getBody());
          }
        }

        return true;
      }
    }
    return super.substituteExecutable(executable);
  }

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName();
    if (methodName.startsWith(RouterAdapterUtils.APP_ROUTER_NAVIGATE_PREFIX)) {
      ExtendedElement target = invocation.getTargetExpression();
      // Invoke navigateTo inside of AppRouter class
      if (target != null && target.toString().equals(RouterAdapterUtils.ROUTER)) {
        print(RouterAdapterUtils.ROUTER_VAR + "." + RouterAdapterUtils.NAVIGATE + "([");
        if (invocation.getArguments() != null && !invocation.getArguments().isEmpty()) {
          String argNames = invocation.getArguments().stream()
              .map(Object::toString)
              .collect(Collectors.joining(", "));
          print(argNames);
        }
        print("])");
        return true;
      }
      // Invoke navigateToX from AppRouter
      else if (target != null && target.toString().equals(RouterAdapterUtils.APP_ROUTER)) {
        print(RouterAdapterUtils.APP_ROUTER + "." + invocation.getMethodName() + "(");
        if (invocation.getArguments() != null && !invocation.getArguments().isEmpty()) {
          for (ExtendedElement arg : invocation.getArguments()) {
            print(arg + ", ");
          }
        }
        print("this." + RouterAdapterUtils.ROUTER_GLOBAL_VAR + ")");
        return true;
      }
    }
    // Enable Angular injection
    else if(methodName.startsWith("inject")) {
      if(invocation.getTargetType() != null) {
        if(invocation.getTargetType().getKind() == TypeKind.DECLARED) {
          TypeElement element = (TypeElement) ((DeclaredType)invocation.getTargetType()).asElement();
          String qualifiedName = element.getQualifiedName().toString();
          if(qualifiedName.startsWith("umlp.guiutil.AngularUtils") && invocation.getMethodName().equals("inject")) {
            print("inject(").printArgList(invocation.getArguments()).print(")");
            getPrinter().useModule(new ModuleImportDescriptor("inject", "@angular/core"));
            return true;
          }
        }
      }
    }

    return super.substituteMethodInvocation(invocation);
  }

  /**
   * Extend *Component.ts as in between class
   * @param type
   * @return
   */
  @Override
  public boolean substituteExtends(TypeElement type) {
    if (adapterUtils.isHWCofDirective(type)) {
      String componentName = type.getSimpleName().toString() + "Component";
      print(" extends " + componentName);
      String path = type.getQualifiedName().toString().replace(".", "/");

      adapterUtils.importFromWithModule(componentName + ", config", path + "Component");
      adapterUtils.importFromWithModule("ActivatedRoute, Router", "@angular/router");
      return true;
    }
    return super.substituteExtends(type);
  }

  public void addAngularComponentAnnotations(TypeElement classElement) {
    if (adapterUtils.isHWCComponent(classElement) && !adapterUtils.hasComponentAnnotation(classElement)) {
      Log.warn("0JSC003 " + classElement.getQualifiedName().toString()
          + " Handwritten Adaption of a GUI Component should be tagged with @Component()");
    }
    if (adapterUtils.isHWCComponent(classElement)) {
      Boolean standAlone = adapterUtils.getAnnotationPropertyValue(classElement, "standalone", Boolean.class,
          true);
      String changeDetection = adapterUtils.getAnnotationPropertyValue(classElement, "changeDetection", String.class,
          "ChangeDetectionStrategy.Eager");
      String[] imports = adapterUtils.getAnnotationPropertyValue(classElement, "imports",
          String[].class, new String[] { "...config.imports" });
      String selector = adapterUtils.getAnnotationPropertyValue(classElement, "selector", String.class,
          "config.selector");
      String templateUrl = adapterUtils.getAnnotationPropertyValue(classElement, "templateUrl", String.class,
          "config.templateUrl");
      String styles = adapterUtils.getAnnotationPropertyValue(classElement, "styles", String.class,
          "config.styles");
      String[] styleUrls = adapterUtils.getAnnotationPropertyValue(classElement, "styleUrls",
          String[].class, new String[] { "...config.styleUrls" });
      String[] tsImports = adapterUtils.getAnnotationPropertyValue(classElement, "tsImports",
          String[].class, new String[] {});
      print("@Component({").println().startIndent().printIndent()
          .print("standalone: " + standAlone + ",").println().printIndent()
          .print("changeDetection: " + changeDetection + ",").println().printIndent()
          .print("imports: " + Arrays.toString(imports) + ",").println().printIndent()
          .print("selector: " + selector + ",").println().printIndent()
          .print("templateUrl: " + templateUrl + ",").println().printIndent()
          .print("styles: " + styles + ",").println().printIndent()
          .print("styleUrls: " + Arrays.toString(styleUrls)).println().endIndent()
          .print("})").println().printIndent();
      adapterUtils.importFromWithModule("Component", "@angular/core");
      adapterUtils.importFromWithModule("ChangeDetectionStrategy", "@angular/core");

      for (String componentImport : tsImports) {
        String[] split = componentImport.split(",");
        if (split.length != 2) {
          Log.error("0JSC004 " + componentImport + " is not in the correct format (importName, importFrom)");
        }
        adapterUtils.importFromWithModule(split[0].strip(), split[1].strip());
      }
    }
    else if (adapterUtils.hasDirectiveAnnotation(classElement)) {
      print("@Directive()").println().printIndent();
      adapterUtils.importFromWithModule("Directive", "@angular/core");
    }
    if (adapterUtils.isHWCComponentOrDirective(classElement)) {
      adapterUtils.importFromWithModule("ActivatedRoute, Router", "@angular/router");
      adapterUtils.importFromWithModule("ComponentObserverManager", GUIAdapterUtils.UMLP_COMMON_POINTER);
    }
  }
}
