package bin;

import de.monticore.cd2pojo.CD2PojoTool;
import jsweet.lang.Erased;
import ma2fustrumo.MA2FuStruMoTool;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;

@Erased
public class MontiArcVizExperiment {
  public static void main(String[] args) {
    System.out.println("Hello world!");
  }

  public static Function4Display createMainFunction() {
    CD2PojoTool.main(new String[]{
        "-i",
        "examples/montiarc2function4display-test/models/functional/Model",
        "-c2mc",
        "--symboltable",
        "examples/montiarc2function4display-test/backend/build/cd2pojo_symbols",
    });

    var tool = new MA2FuStruMoTool();
    tool.init();
    tool.run(new String[]{
        "-i",
        "examples/montiarc2function4display-test/models/functional/Model",
        "-path",
        "examples/montiarc2function4display-test/backend/build/cd2pojo_symbols",
        "-main",
        "summerschooltutorial.IMM",
        "-c2mc",
    });

    return tool.getResult4Display();
  }
}
