/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension;

import org.jsweet.transpiler.extension.PrinterAdapter;
import standalone.com.sun.source.tree.CompilationUnitTree;
import umlp.jsweet.extension.cocos.JSweetCoCoChecker;
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

/**
 * This adapter combines the adapters for GUI transpilation
 */
public class GUIAdapter extends PrinterAdapter {
  public GUIAdapter(PrinterAdapter parentAdapter) {
    super(
        new GUIComponentObserverPatternAdapter(
        new OptionalAdapter(
        new FutureAdapter(
        new CollectionsAdapter(
        new JavaBasicsAdapter(
        new UMLPRTEImportAdapter(
        new JavaTimeAdapter(
        new BasicsAdapter(
        new AsyncConstructorAdapter(
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
  }

  @Override
  public void onTranspilationStarted() {
    propagateAsyncFromRTECalls();

    for (CompilationUnitTree compilationUnit : getContext().compilationUnits) {
      if(!getContext().isExcludedSourcePath(compilationUnit.getSourceFile().getName())) {
        new JSweetCoCoChecker(getContext(), compilationUnit).scan(compilationUnit, getContext().trees);
      }
    }
    // CD2Java classes are transpiled separately (see ModuleUMLPAdapter), they keep their types here
    UMLPAdapterUtils.excludeFromGeneration(getContext(),
        compilationUnit -> UMLPAdapterUtils.isCD2Java(getContext(), compilationUnit));
    super.onTranspilationStarted();
  }

  public void propagateAsyncFromRTECalls() {
    UMLPAdapterUtils.addAsyncAnnotationsForJREToContext(this.getContext());

    new UMLPAsyncAwaitPropagationScanner(context, true).process(getContext().compilationUnits, getContext().trees);
  }
}
