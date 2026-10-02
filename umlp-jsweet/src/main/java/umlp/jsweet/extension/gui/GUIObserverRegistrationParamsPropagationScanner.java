/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.gui;

import org.jsweet.transpiler.JSweetContext;
import org.jsweet.transpiler.util.AbstractTreeScanner;
import org.jsweet.transpiler.util.Util;
import standalone.com.sun.source.tree.CompilationUnitTree;
import standalone.com.sun.source.tree.MethodTree;
import standalone.com.sun.source.util.TreePath;
import standalone.com.sun.source.util.Trees;
import umlp.jsweet.extension.util.GUIAdapterUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Propagates the observer registration parameters annotation from a method to the methods it overrides, so a
 * method and its overridden methods get the same TypeScript parameters. The other direction, from a method to
 * the methods that override it, is checked when printing, see {@link GUIAdapterUtils#elementOrRelatedHasAnnotation}.
 * <p>
 * Only methods that this transpilation prints are annotated: a library method like {@code Object.toString} or a
 * method of an excluded source would otherwise get the parameters at every call.
 */
public class GUIObserverRegistrationParamsPropagationScanner extends AbstractTreeScanner {

  private final Deque<ExecutableElement> annotated = new ArrayDeque<>();

  protected GUIObserverRegistrationParamsPropagationScanner(JSweetContext context) {
    super(null, context, null);
  }

  public void process(List<CompilationUnitTree> compilationUnits, Trees trees) {
    for (CompilationUnitTree compilationUnit : compilationUnits) {
      scan(compilationUnit, trees);
    }
    while (!annotated.isEmpty()) {
      ExecutableElement method = annotated.pop();
      List<ExecutableElement> overridden = new ArrayList<>();
      util().collectMatchingMethodDeclarationsInType((TypeElement) method.getEnclosingElement(),
          method.getSimpleName().toString(), (ExecutableType) method.asType(), true, overridden);
      for (ExecutableElement candidate : overridden) {
        if (candidate.getKind() != ElementKind.CONSTRUCTOR && isPrinted(candidate, trees)
            && !hasAnnotation(candidate)) {
          context.addExtraAnnotationType(candidate, GUIAdapterUtils.NEEDS_OBSERVER_REGISTRATION_PARAMS_ANNOTATION);
          annotated.push(candidate);
        }
      }
    }
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree compilationUnit, Trees trees) {
    // an excluded source is not printed, and the methods it overrides are not printed either
    if (context.isExcludedSourcePath(compilationUnit.getSourceFile().getName())) {
      return null;
    }
    return super.visitCompilationUnit(compilationUnit, trees);
  }

  @Override
  public Void visitMethod(MethodTree node, Trees trees) {
    ExecutableElement method = Util.getElement(node);
    if (method != null && method.getKind() != ElementKind.CONSTRUCTOR && hasAnnotation(method)) {
      annotated.add(method);
    }
    return super.visitMethod(node, trees);
  }

  private boolean hasAnnotation(ExecutableElement method) {
    return context.hasAnnotationType(method, GUIAdapterUtils.NEEDS_OBSERVER_REGISTRATION_PARAMS_ANNOTATION);
  }

  private boolean isPrinted(ExecutableElement method, Trees trees) {
    TreePath path = trees.getPath(method);
    return path != null
        && !context.isExcludedSourcePath(path.getCompilationUnit().getSourceFile().getName());
  }
}
