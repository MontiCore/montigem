/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension;

import org.jsweet.transpiler.AsyncAwaitPropagationScanner;
import org.jsweet.transpiler.extension.AnnotationManager;
import org.jsweet.transpiler.extension.PrinterAdapter;
import standalone.com.sun.source.tree.CompilationUnitTree;
import umlp.jsweet.extension.cocos.JSweetCoCoChecker;
import umlp.jsweet.extension.common.AdditionalAsyncAwaitPropagationScanner;
import umlp.jsweet.extension.common.AsyncConstructorAdapter;
import umlp.jsweet.extension.common.BasicsAdapter;
import umlp.jsweet.extension.common.CollectionsAdapter;
import umlp.jsweet.extension.common.FutureAdapter;
import umlp.jsweet.extension.common.JavaBasicsAdapter;
import umlp.jsweet.extension.common.JavaTimeAdapter;
import umlp.jsweet.extension.common.MutexAdapter;
import umlp.jsweet.extension.common.OptionalAdapter;
import umlp.jsweet.extension.common.UMLPAsyncAwaitPropagationScanner;
import umlp.jsweet.extension.common.UMLPEnumAdapter;
import umlp.jsweet.extension.common.UMLPRTEImportAdapter;
import umlp.jsweet.extension.gui.AngularSpecificsAdapter;
import umlp.jsweet.extension.gui.AnnotationToDecoratorAdapter;
import umlp.jsweet.extension.gui.GUIComponentObserverPatternAdapter;
import umlp.jsweet.extension.gui.GUITypesImportAdapter;
import umlp.jsweet.extension.gui.GUIUtilAdapter;
import umlp.jsweet.extension.gui.TSTypesAdapter;
import umlp.jsweet.extension.umlp.BackendOnlyAdapter;
import umlp.jsweet.extension.util.UMLPAdapterUtils;

import javax.lang.model.element.Element;

import static org.jsweet.JSweetConfig.ANNOTATION_ERASED;
import static umlp.jsweet.extension.util.GUIAdapterUtils.CD2JAVA_ANNOTATION;

/**
 * This adapter combines the adapters for GUI transpilation
 */
public class GUIAdapter extends PrinterAdapter {
  public GUIAdapter(PrinterAdapter parentAdapter) {
    super(
       //new JavaStreamAdapter(
        new OptionalAdapter(
        new FutureAdapter(
        new CollectionsAdapter(
        new JavaBasicsAdapter(
        new UMLPRTEImportAdapter(
        new JavaTimeAdapter(
        new BasicsAdapter(
        new AsyncConstructorAdapter(
        new GUIComponentObserverPatternAdapter(
        new AngularSpecificsAdapter(
        new GUITypesImportAdapter(
        new TSTypesAdapter(
        new GUIUtilAdapter(
        new BackendOnlyAdapter(
        new MutexAdapter(
        new UMLPEnumAdapter(
        new AnnotationToDecoratorAdapter(
        parentAdapter
    ))))))))))))))))));
    addAnnotationManager(getSynchronizedAnnotationManager());
  }

  private AnnotationManager getSynchronizedAnnotationManager() {
    return new AnnotationManager() {
      // Specifies if the `annotation` should be added to, removed from, be left alone
      // for the `element` depending on the return value
      @Override
      public Action manageAnnotation(Element element, String annotation) {
        if (element.getKind().toString().equals("CLASS") && ANNOTATION_ERASED.equals(annotation)) {
          System.out.println("GUIAdapter");
          System.out.println(element.getSimpleName() + (isAnnotatedWithCD2Java(element) ? " (CD2JAVA)" : ""));
        }
        return ANNOTATION_ERASED.equals(annotation) &&
            element.getKind().toString().equals("CLASS") &&
            isAnnotatedWithCD2Java(element) ?
            Action.ADD :
            Action.VOID;
      }

      private boolean isAnnotatedWithCD2Java(Element element) {
        return getContext().elementHasAnnotationType(element, CD2JAVA_ANNOTATION);
      }
    };
  }

  @Override
  public void onTranspilationStarted() {
    propagateAsyncFromRTECalls();

    for (CompilationUnitTree compilationUnit : getContext().compilationUnits) {
      if(!getContext().isExcludedSourcePath(compilationUnit.getSourceFile().getName())) {
        new JSweetCoCoChecker(getContext()).scan(compilationUnit, getContext().trees);
      }
    }
    super.onTranspilationStarted();
  }

  public void propagateAsyncFromRTECalls() {
    UMLPAdapterUtils.addAsyncAnnotationsForJREToContext(this.getContext());

    new UMLPAsyncAwaitPropagationScanner(context, true).process(getContext().compilationUnits, getContext().trees);
    new AsyncAwaitPropagationScanner(context).process(getContext().compilationUnits, getContext().trees);
    new UMLPAsyncAwaitPropagationScanner(context, true).process(
        getContext().compilationUnits, getContext().trees);
    new AsyncAwaitPropagationScanner(context).process(getContext().compilationUnits, getContext().trees);
    new AdditionalAsyncAwaitPropagationScanner(context).process(
        getContext().compilationUnits, getContext().trees);
    new AsyncAwaitPropagationScanner(context).process(getContext().compilationUnits, getContext().trees);
  }
}
