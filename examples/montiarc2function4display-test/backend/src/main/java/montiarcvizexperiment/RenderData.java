package montiarcvizexperiment;

public class RenderData extends RenderDataTOP {

  @Override
  public boolean isValid() {
    return !getMainFunctionName().isBlank() && !getSrc().isBlank();
  }
}
