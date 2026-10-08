package montiarcvizexperiment;

import de.se_rwth.commons.logging.Log;
import jakarta.annotation.PostConstruct;
import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;

import java.io.IOException;
import java.sql.SQLException;

public class MontiArcVizExperimentServerApplication
    extends MontiArcVizExperimentServerApplicationTOP {

  public static void main(String[] args) {
    SpringApplication.run(MontiArcVizExperimentServerApplication.class, args);
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
