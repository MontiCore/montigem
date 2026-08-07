/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension;

import org.jsweet.transpiler.AsyncAwaitPropagationScanner;
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
import umlp.jsweet.extension.gui.GUITypesImportAdapter;
import umlp.jsweet.extension.module.RelativeImportAdapter;
import umlp.jsweet.extension.umlp.BackendOnlyAdapter;
import umlp.jsweet.extension.util.UMLPAdapterUtils;

/**
 * This adapter combines the adapters for domain classes transpilation
 * with relative imports (see {@link RelativeImportAdapter})
 */
public class ModuleDomainCDAdapter extends PrinterAdapter {
  public ModuleDomainCDAdapter(PrinterAdapter parentAdapter) {
    super(
//        new PrintImportsAtBottomAdapter(
        //new JavaStreamAdapter(
        new RelativeImportAdapter(
        new GUITypesImportAdapter(
        new OptionalAdapter(
        new FutureAdapter(
        new UMLPRTEImportAdapter(
        new JavaTimeAdapter(
        new BasicsAdapter(
        new AsyncConstructorAdapter(
        new CollectionsAdapter(
        new JavaBasicsAdapter(
        new BackendOnlyAdapter(
        new MutexAdapter(
        new UMLPEnumAdapter(
        parentAdapter
    ))))))))))))));
  }

  @Override
  public void onTranspilationStarted() {
    propagateAsyncFromRTECalls();

    for (CompilationUnitTree compilationUnit : getContext().compilationUnits) {
      if (!getContext().isExcludedSourcePath(compilationUnit.getSourceFile().getName())) {
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
