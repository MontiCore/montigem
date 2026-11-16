/* (c) https://github.com/MontiCore/monticore */
package collabtextdemo;

import collabtextmodule.CollabText;
import collabtextmodule.CollabTextModuleManager;
import collabtextmodule.CollabTextSync;
import collabtextmodule.TextEdit;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import java.io.IOException;

@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class},
    scanBasePackages = {"umlp.backendrte.service.websocket", "umlp.backendrte.service.rest", "service"}
)
public class CollabTextDemoServerApplication extends CollabTextDemoServerApplicationTOP {

  public static void main(String[] args) {
    SpringApplication.run(CollabTextDemoServerApplication.class, args);
  }

  @PostConstruct
  @Override
  public void init() throws IOException {
    super.init();
    CollabText sharedNotes = CollabTextModuleManager.collabTextBuilder().build().get();
    CollabTextSync.apply(sharedNotes, new TextEdit(0, 0,
        "Open this page in a second window and type in both of them.\n"));
  }
}
