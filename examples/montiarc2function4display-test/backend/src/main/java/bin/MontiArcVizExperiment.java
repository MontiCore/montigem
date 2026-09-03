package bin;

import de.monticore.cd2pojo.CD2PojoTool;
import ma2fustrumo.MA2FuStruMoTool;

public class MontiArcVizExperiment {
  public static void main(String[] args) {
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
        //no idea what this does, but it seems important
    });

    var result = tool.getResult4Display();
    System.out.println(result);
  }
}
