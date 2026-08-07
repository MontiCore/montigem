/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.util;

import org.apache.commons.lang3.StringUtils;
import org.jsweet.transpiler.JSweetContext;
import org.jsweet.transpiler.OverloadScanner;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.util.Util;
import umlp.jsweet.extension.gui.GUIComponentObserverPatternAdapter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

public class GUIAdapterUtils extends UMLPAdapterUtils {
  public static final String COMPONENT_ANNOTATION = "umlp.jsweet.extension.annotation.Component";

  public static final String DIRECTIVE_ANNOTATION = "umlp.jsweet.extension.annotation.Directive";

  public static final String INPUT_ANNOTATION = "umlp.jsweet.extension.annotation.Input";

  public static final String OUTPUT_ANNOTATION = "umlp.jsweet.extension.annotation.Output";

  /* This annotation marks methods whose result will be tracked by the Angular Change Detection
  *  i.e. they are used in the HTML template*/
  public static final String ANGULAR_TRACKED_FOR_CHANGE_ANNOTATION = "umlp.jsweet.extension.annotation.AngularTrackedUpdates";

  /* This annotation marks methods whose parameters contain observer registration function to
  * trigger updates of the Angular stored variables in the FrontEnd */
  public static final String NEEDS_OBSERVER_REGISTRATION_PARAMS_ANNOTATION = "umlp.jsweet.extension.annotation.ObserverRegistrationParams";

  /* This annotation marks binding methods whose ForAngular update runs must not fire in parallel;
  * they are chained and additional triggers coalesce */
  public static final String SERIALIZED_ANNOTATION = "umlp.jsweet.extension.annotation.Serialized";

  /* This annotation marks methods that trigger update for the angular change tracking, i.e. setters
  * for GUI Params */
  public static final String NOTIFY_ABOUT_FIELD_UPDATE_ANNOTATION = "umlp.jsweet.extension.annotation.NotifyAboutFieldUpdate";

  public static final String NOTIFY_ABOUT_FIELD_UPDATE_ANNOTATION_OBSERVED_FIELD = "observedField";
  public static final String NOTIFY_ABOUT_FIELD_UPDATE_ANNOTATION_METHOD = "methodRequiringUpdate";

  public static final String REPLACE_ANNOTATION = "jsweet.lang.Replace";

  public static final String AMODULE_ANNOTATION = "umlp.jsweet.extension.annotation.AnnotationModule";

  public static final String DECORATOR_NO_OBJ_ANNOTATION = "umlp.jsweet.extension.annotation.DecoratorNoObj";

  public static final String CD2JAVA_ANNOTATION = "umlp.jsweet.extension.annotation.CD2Java";

  public static final String SYNCHRONIZED_SUFFIX = "__ChangeDetection";

  public static final String RESULT_CACHE_SUFFIX = "__CachedResult";

  public static final String UPDATE_SUFFIX = "__update";

  public static final String UMLP_COMMON_POINTER = "@umlp/commonj2ts";

  public static final String STATEVAR_PREFIX = "_";

  public GUIAdapterUtils(PrinterAdapter adapter, Types types, JSweetContext context, Util util) {
    super(adapter, types, context, util);
  }

  public GUIAdapterUtils(Types types, JSweetContext context, Util util) {
    super(null, types, context, util);
  }

  public static String getMethodNameWithoutGetIs(MethodInvocationElement invocation) {
    String methodName = invocation.getMethodName().toString();
    return methodName.startsWith("get") ?
        StringUtils.uncapitalize(methodName.substring(3)) :
        StringUtils.uncapitalize(methodName.substring(2));
  }

  public boolean isHWCComponent(TypeElement type) {
    return hasComponentAnnotation(type) || isHWCofDirective(type);
  }

  public boolean isHWCComponentOrDirective(TypeElement type) {
    return isHWCComponent(type) || hasDirectiveAnnotation(type);
  }

  public boolean hasComponentAnnotation(TypeElement type) {
    return context.hasAnnotationType(type, COMPONENT_ANNOTATION);
  }

  public boolean hasDirectiveAnnotation(TypeElement classElement) {
    return context.hasAnnotationType(classElement, DIRECTIVE_ANNOTATION);
  }

  public boolean isHWCofDirective(TypeElement classElement) {
    if (classElement.getSuperclass() == null || classElement.getSuperclass().toString() == "none") {
      return false;
    }
    Element superType = types.asElement(classElement.getSuperclass());
    return superType.getSimpleName().toString().endsWith("TOP")
        && context.hasAnnotationType(superType, GUIAdapterUtils.DIRECTIVE_ANNOTATION);
  }

  public boolean isTOPOfComponent(TypeElement classElement) {
    return hasDirectiveAnnotation(classElement) && !isHWCofDirective(classElement) &&
        classElement.getSimpleName().toString().endsWith("TOP");
  }

  public boolean isMethodOfHWCComponentOrDirective(Element element) {
    return element.getEnclosingElement() instanceof TypeElement &&
        isHWCComponentOrDirective((TypeElement) element.getEnclosingElement());
  }

  public boolean isMethodOfHWCComponent(Element element) {
    return element.getEnclosingElement() instanceof TypeElement &&
        isHWCComponent((TypeElement) element.getEnclosingElement());
  }

  public boolean elementOrRelatedHasAnnotation(ExecutableElement executable, String annotation) {
    if (context.hasAnnotationType(executable, annotation)) {
      return true;
    }
    if (context.hasAnnotationType(executable, REPLACE_ANNOTATION)) {
      return false;
    }
    OverloadScanner.Overload overload = context.getOverload(
        (TypeElement) executable.getEnclosingElement(), executable);
    if (overload == null || overload.getEntries() == null) {
      return false;
    }

    for (OverloadScanner.OverloadMethodEntry overloadEntry : overload.getEntries()) {
      if (context.hasAnnotationType(overloadEntry.methodElement, annotation)) {
        return true;
      }
    }

    List<ExecutableElement> candidates = new ArrayList<>();
    this.util.collectMatchingMethodDeclarationsInType(
        (TypeElement) executable.getEnclosingElement(), executable.getSimpleName().toString(),
        (ExecutableType) executable.asType(), true, candidates);
    for (ExecutableElement candidate : candidates) {
      if (context.hasAnnotationType(candidate, annotation)) {
        return true;
      }
    }

    return false;
  }

  public boolean isAngularTrackedUpdatesAnnotated(ExecutableElement executable) {
    return elementOrRelatedHasAnnotation(executable, ANGULAR_TRACKED_FOR_CHANGE_ANNOTATION);
  }

  public boolean isObserverRegistrationParamsAnnotated(ExecutableElement executable) {
    return elementOrRelatedHasAnnotation(executable, NEEDS_OBSERVER_REGISTRATION_PARAMS_ANNOTATION);
  }

  boolean isDirectiveMethod(ExecutableElement executable, TypeElement classElement) {
    return hasDirectiveAnnotation(classElement) &&
        executable.getKind().toString().equals("METHOD") &&
        !util.isInterface(adapter.getParentElement(TypeElement.class)) &&
        !executable.getModifiers().contains(Modifier.ABSTRACT) &&
        !executable.getModifiers().contains(Modifier.STATIC) &&
        !executable.getModifiers().contains(Modifier.DEFAULT);
  }

  public boolean isAngularTrackedUpdatesMethod(ExecutableElement executable,
                                               GUIComponentObserverPatternAdapter guiComponentAdapter) {
    return isAngularTrackedUpdatesAnnotated(executable) &&
        !guiComponentAdapter.util().isInterface(guiComponentAdapter.getParentElement(TypeElement.class)) &&
        !executable.getModifiers().contains(Modifier.ABSTRACT) &&
        !executable.getModifiers().contains(Modifier.STATIC) &&
        !executable.getModifiers().contains(Modifier.DEFAULT);
  }

  public boolean isObserverRegistrationParamsMethod(ExecutableElement executable,
                                               GUIComponentObserverPatternAdapter guiComponentAdapter) {
    return isObserverRegistrationParamsAnnotated(executable) &&
        !guiComponentAdapter.util().isInterface(guiComponentAdapter.getParentElement(TypeElement.class)) &&
        !executable.getModifiers().contains(Modifier.ABSTRACT) &&
        !executable.getModifiers().contains(Modifier.STATIC) &&
        !executable.getModifiers().contains(Modifier.DEFAULT);
  }

  public <T> T getAnnotationPropertyValue(TypeElement classElement, String annotationProperty,
      Class<T> propertyClass, T defaultValue) {
    if (hasComponentAnnotation(classElement) || hasDirectiveAnnotation(classElement)) {
      String annotation = hasDirectiveAnnotation(classElement) ?
          GUIAdapterUtils.DIRECTIVE_ANNOTATION :
          GUIAdapterUtils.COMPONENT_ANNOTATION;
      T value = context.getAnnotationValue(classElement, annotation, annotationProperty,
          propertyClass, null);
      if (value != null) {
        return value;
      }
    }
    if (isHWCofDirective(classElement)) {
      T value = getAnnotationPropertyValue(
          (TypeElement) types.asElement(classElement.getSuperclass()),
          annotationProperty, propertyClass, defaultValue);
      if (value != null) {
        return value;
      }
    }
    //    Log.error("0JSC002 Cannot find " + annotationProperty + " of classElement " + classElement.getSimpleNameOfFQN().toString());
    return defaultValue;
  }

  public boolean isInitMethod(ExecutableElement executable, TypeElement classElement) {
    return isDirectiveMethod(executable, classElement) &&
        executable.getSimpleName().toString().equals("init");
  }

  SunTypesHelper sunTypesHelper = new SunTypesHelper();
  public standalone.com.sun.tools.javac.tree.JCTree.JCVariableDecl getNewParameter(String variableName) {
    return sunTypesHelper.getNewParameter(variableName);
  }

  public standalone.com.sun.tools.javac.tree.JCTree.JCLiteral getNewLiteral(String variableName) {
    return sunTypesHelper.getNewLiteral(variableName);
  }

  public standalone.com.sun.tools.javac.tree.JCTree.JCIdent getNewIdent(String variableName) {
    return sunTypesHelper.getNewIdent(variableName);
  }

  // TODO this should be removed, since we have Annotations for it
  public boolean isStateVarGetter(MethodInvocationElement invocation,
      Element parentElement, GUIComponentObserverPatternAdapter guiComponentObserverPatternAdapter) {
    return invocation.getMethodName().startsWith("get") &&
        guiComponentObserverPatternAdapter.util().getAllMembers((TypeElement) guiComponentObserverPatternAdapter.types().asElement(parentElement.asType())).stream()
            .anyMatch(m -> m.getSimpleName().toString().equalsIgnoreCase(
                STATEVAR_PREFIX + invocation.getMethodName().substring(3))) ||
        invocation.getMethodName().startsWith("is") &&
            guiComponentObserverPatternAdapter.util().getAllMembers((TypeElement) guiComponentObserverPatternAdapter.types().asElement(parentElement.asType())).stream()
                .anyMatch(m -> m.getSimpleName().toString().equalsIgnoreCase(
                    STATEVAR_PREFIX + invocation.getMethodName().substring(2)));
  }
}
