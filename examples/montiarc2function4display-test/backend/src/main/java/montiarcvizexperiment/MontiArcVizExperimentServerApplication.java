package montiarcvizexperiment;

import de.monticore.cd2pojo.CD2PojoTool;
import de.se_rwth.commons.logging.Log;
import jakarta.annotation.PostConstruct;
import ma2fustrumo.MA2FuStruMoTool;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class MontiArcVizExperimentServerApplication
    extends MontiArcVizExperimentServerApplicationTOP {

  private static Function4Display mainFunction;

  public static void main(String[] args) {
    SpringApplication.run(MontiArcVizExperimentServerApplication.class, args);
  }

  public static Function4Display getMainFunction() {
    return Optional.ofNullable(mainFunction).orElseGet(() -> {
      var res = createMainFunction();
      mainFunction = res;
      return res;
    });
  }

  private static Function4Display createMainFunction() {
    CD2PojoTool.main(new String[]{
        "-i",
        "../models/functional/Model",
        "-c2mc",
        "--symboltable",
        "../backend/build/cd2pojo_symbols",
    });

    var tool = new MA2FuStruMoTool();
    tool.init();
    tool.run(new String[]{
        "-i",
        "../models/functional/Model",
        "-path",
        "build/cd2pojo_symbols",
        "-main",
        "summerschooltutorial.IMM",
        "-c2mc",
    });

    return tool.getResult4Display();
  }

  @PostConstruct
  @Override
  public void init() throws IOException {
    super.init();

    try {
      Server webServer = Server.createWebServer("-web", "-webAllowOthers").start();
      Log.info(String.format("H2 Web Server started at: http://localhost:%s", webServer.getPort()),
               MontiArcVizExperimentServerApplication.class.getName());
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
}
