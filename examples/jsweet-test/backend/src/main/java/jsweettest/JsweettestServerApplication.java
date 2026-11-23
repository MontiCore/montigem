/* (c) https://github.com/MontiCore/monticore */
package jsweettest;

import de.se_rwth.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;

@SpringBootApplication(
    exclude={ DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class},
    scanBasePackages={"umlp.backendrte.service.websocket", "umlp.backendrte.service.rest", "jsweettest.service"}
)
public class JsweettestServerApplication extends JsweettestServerApplicationTOP {

  public  static  void main (String[] args){
    SpringApplication.run(JsweettestServerApplication.class, args);
  }

  @PostConstruct
  @Override
  public void init() throws IOException {
    super.init();
    A a1 = JsweettestManager.aBuilder().value("Malte").build().get();
    A a2 = JsweettestManager.aBuilder().value("Alex").build().get();
    A a3 = JsweettestManager.aBuilder().value("Sebastian").build().get();

    JsweettestManager.eBuilder().value("e1").a(a1).build();
    JsweettestManager.eBuilder().value("e2").a(a2).build();
    JsweettestManager.eBuilder().value("e3").a(a3).build();

    JsweettestManager.getGlobalState().setState(JsweettestManager.stateBuilder().name("State A").build().get());
    JsweettestManager.typesBuilder().timestamp(LocalDateTime.now()).build();
    JsweettestManager.timeValuesBuilder().start(Instant.parse("2026-11-17T08:00:00.123456Z"))
        .length(Duration.ofMinutes(90)).reminder(LocalTime.of(9, 15)).build();
  }

  public static void resetTestData() {
    // TODO: THe following #clears to not work
    JsweettestManager.getC1List().clear();
    JsweettestManager.getC2List().clear();
    JsweettestManager.getC3List().clear();

    while (!JsweettestManager.getC1List().isEmpty()) {
      JsweettestManager.deleteC1(JsweettestManager.getC1List().iterator().next().getGemId());
    }
    while (!JsweettestManager.getC2List().isEmpty()) {
      JsweettestManager.deleteC1(JsweettestManager.getC2List().iterator().next().getGemId());
    }
    while (!JsweettestManager.getC3List().isEmpty()) {
      JsweettestManager.deleteC1(JsweettestManager.getC3List().iterator().next().getGemId());
    }

    C1 c1 = JsweettestManager.c1Builder()
        // strings
        .string("C1#1")
        // ints
        .int1(1)
        .int2(2)
        .hwGemId(-1L)
        .c2Req(randomC2(1))
        .build().get();
    c1.setHwGemId(c1.getGemId());

    c1.getC2Req().addC3mult(randomC3(1));
    c1.getC2Req().addC3mult(randomC3(2));

    c1.setC2opt(randomC2(2));

    c1.addC2mult(c1.getC2Req());
    c1.addC2mult(randomC2(3));
    c1.addC2mult(randomC2(4));
    c1.addC2mult(randomC2(5));


    // Reset Ns
    System.out.println("Reset Ns");
    for (N2 n2 : JsweettestManager.getN2List()) {
      JsweettestManager.deleteN2(n2.getGemId());
    }
    for (N1 n1 : JsweettestManager.getN1List()) {
      JsweettestManager.deleteN1(n1.getGemId());
    }
    for (N0 n0 : JsweettestManager.getN0List()) {
      JsweettestManager.deleteN0(n0.getGemId());
    }
    // Invent Ns
    System.out.println("Invent Ns");
    for (int i = 0; i < 3; i++) {
      N1Builder n1Builder = JsweettestManager.n1Builder();
      for (int j = 0; j < 4; j++) {
        n1Builder.n2Add(JsweettestManager.n2Builder()
            .name("N2#" + i + "#" + j).build().get());
      }
      JsweettestManager.n0Builder().n1(n1Builder.build().get()).build();
    }
  }

  protected static C3 randomC3(int i) {
    return JsweettestManager.c3Builder()
        // strings
        .string("C3#" + i)
        // ints
        .int1(100 + i)
        .int2(200 + i)
        .build().get();
  }

  protected static C2 randomC2(int i) {
    return JsweettestManager.c2Builder()
        // strings
        .string("C2#" + i)
        // ints
        .int1(10 + i)
        .int2(20 + i)
        .build().get();
  }
}
