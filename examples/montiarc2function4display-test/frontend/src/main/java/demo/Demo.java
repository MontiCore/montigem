package demo;

import montiarcvizexperiment.DemoApp;
import montiarcvizexperiment.MontiArcVizExperimentManager;
import montiarcvizexperiment.RenderData;
import umlp.jsweet.extension.annotation.Component;

import java.util.function.Consumer;

@Component
public class Demo extends DemoTOP {
  @Override
  public Consumer<Void> renderSrc() {
    return $ -> {
      DemoApp app = getApp();
      RenderData renderData = MontiArcVizExperimentManager
          .renderDataBuilder()
          .mainFunctionName(app.getMainFunctionNameInput())
          .src(app.getSrcInput())
          .build()
          .orElseThrow();
      app.setRenderedFunction(renderData);
    };
  }
}
