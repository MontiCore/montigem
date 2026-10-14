/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.gui;

import de.se_rwth.commons.logging.Log;
import org.jsweet.transpiler.JSweetContext;
import org.jsweet.transpiler.util.AbstractTreeScanner;
import org.jsweet.transpiler.util.Util;
import standalone.com.sun.source.tree.CompilationUnitTree;
import standalone.com.sun.source.tree.MethodInvocationTree;
import standalone.com.sun.source.tree.MethodTree;
import standalone.com.sun.source.util.Trees;
import umlp.jsweet.extension.util.GUIAdapterUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Propagates the @Serialized annotation from a handwritten binding method to the
 * generated <method>ForAngular pendant in the TOP superclass, where the update
 * machinery is printed by {@link GUIComponentObserverPatternAdapter}. For bindings
 * with a custom function (data = computeX(...)) no direct pendant exists; the
 * annotation is then propagated to the ForAngular pendant of the generated arg
 * getter that calls the annotated method.
 */
public class GUISerializedBindingPropagationScanner extends AbstractTreeScanner {

  private final List<ExecutableElement> unresolved = new ArrayList<>();

  protected GUISerializedBindingPropagationScanner(JSweetContext context) {
    super(null, context, null);
  }

  public void process(List<CompilationUnitTree> compilationUnits, Trees trees) {
    for (CompilationUnitTree compilationUnit : compilationUnits) {
      scan(compilationUnit, trees);
    }
    if (!unresolved.isEmpty()) {
      TransitiveScanner transitiveScanner = new TransitiveScanner(context);
      for (CompilationUnitTree compilationUnit : compilationUnits) {
        transitiveScanner.scan(compilationUnit, trees);
      }
      unresolved.removeAll(transitiveScanner.resolved);
    }
    for (ExecutableElement method : unresolved) {
      Log.warn("0xEE011: @Serialized on " + method.getEnclosingElement() + "." + method.getSimpleName()
          + " could not be propagated: no " + method.getSimpleName() + "ForAngular pendant exists and no "
          + "generated binding getter calling it was found.");
    }
  }

  @Override
  public Void visitMethod(MethodTree node, Trees trees) {
    try {
      ExecutableElement method = Util.getElement(node);
      if (method == null || method.getKind() == ElementKind.CONSTRUCTOR ||
          !context.hasAnnotationType(method, GUIAdapterUtils.SERIALIZED_ANNOTATION)) {
        return super.visitMethod(node, trees);
      }
      String bindingName = method.getSimpleName().toString() + "ForAngular";
      Element enclosing = method.getEnclosingElement();
      TypeMirror superclass = enclosing instanceof TypeElement ?
          ((TypeElement) enclosing).getSuperclass() : null;
      boolean found = false;
      while (superclass != null && types().asElement(superclass) instanceof TypeElement) {
        TypeElement superType = (TypeElement) types().asElement(superclass);
        for (ExecutableElement candidate : ElementFilter.methodsIn(superType.getEnclosedElements())) {
          if (candidate.getSimpleName().contentEquals(bindingName)) {
            context.addExtraAnnotationType(candidate, GUIAdapterUtils.SERIALIZED_ANNOTATION);
            found = true;
          }
        }
        if (found) {
          break;
        }
        superclass = superType.getSuperclass();
      }
      if (!found) {
        unresolved.add(method);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return super.visitMethod(node, trees);
  }

  private boolean isInSuperclassChain(ExecutableElement method, TypeElement type) {
    Element enclosing = method.getEnclosingElement();
    TypeMirror superclass = enclosing instanceof TypeElement ?
        ((TypeElement) enclosing).getSuperclass() : null;
    while (superclass != null && types().asElement(superclass) instanceof TypeElement) {
      TypeElement superType = (TypeElement) types().asElement(superclass);
      if (superType.equals(type)) {
        return true;
      }
      superclass = superType.getSuperclass();
    }
    return false;
  }

  /**
   * Second pass: finds methods in the superclass chain of an annotated method whose
   * body calls the annotated method and that have a ForAngular pendant in their own
   * class, and propagates the annotation to that pendant (one level of transitivity).
   */
  private class TransitiveScanner extends AbstractTreeScanner {

    private final Set<ExecutableElement> resolved = new LinkedHashSet<>();

    private TypeElement callerType;

    private ExecutableElement callerPendant;

    // the members of a type are listed once, not once per method
    private final Map<TypeElement, Map<String, ExecutableElement>> pendants = new HashMap<>();

    protected TransitiveScanner(JSweetContext context) {
      super(null, context, null);
    }

    /** the last method with each name, like the former loop over the members */
    private Map<String, ExecutableElement> pendantsByName(TypeElement type) {
      Map<String, ExecutableElement> byName = new HashMap<>();
      for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
        byName.put(method.getSimpleName().toString(), method);
      }
      return byName;
    }

    @Override
    public Void visitMethod(MethodTree node, Trees trees) {
      ExecutableElement caller = Util.getElement(node);
      callerType = null;
      callerPendant = null;
      if (caller != null && caller.getKind() != ElementKind.CONSTRUCTOR
          && caller.getEnclosingElement() instanceof TypeElement) {
        callerType = (TypeElement) caller.getEnclosingElement();
        callerPendant = pendants.computeIfAbsent(callerType, this::pendantsByName)
            .get(caller.getSimpleName().toString() + "ForAngular");
      }
      Void result = callerPendant != null ? super.visitMethod(node, trees) : null;
      callerType = null;
      callerPendant = null;
      return result;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree invocation, Trees trees) {
      try {
        if (callerPendant != null) {
          ExecutableElement invoked = Util.getElement(invocation.getMethodSelect());
          if (invoked != null) {
            for (ExecutableElement annotated : unresolved) {
              if (invoked.getSimpleName().contentEquals(annotated.getSimpleName())
                  && invoked.getParameters().size() == annotated.getParameters().size()
                  && isInSuperclassChain(annotated, callerType)) {
                context.addExtraAnnotationType(callerPendant, GUIAdapterUtils.SERIALIZED_ANNOTATION);
                resolved.add(annotated);
              }
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return super.visitMethodInvocation(invocation, trees);
    }
  }
}
