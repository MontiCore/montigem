package ocl2java.commonExpressions;

import com.microsoft.playwright.junit.UsePlaywright;
import mydomain.MydomainManager;
import mydomain.TrafficLight;
import mydomain.TrafficLightColor;
import mydomain.enumExpressions.EnumEquals;
import mydomain.enumExpressions.EnumNotEquals;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import util.AbstractOCL2CypherTest;
import util.AbstractOCL2JavaE2ETest;
import util.AbstractOCL2JavaGenTest;
import util.IOCL2XTest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UsePlaywright
public interface EnumExpressionsTest extends IOCL2XTest<TrafficLight> {
  @Tag("Cypher")
  @Disabled
  class EnumExpressionsCypherTest extends AbstractOCL2CypherTest<TrafficLight> implements EnumExpressionsTest {
  }

  @Override
  default Map<String, Predicate<TrafficLight>> getConstraintToChecker() {
    Map<String, Predicate<TrafficLight>> res = new HashMap<>();
    res.put("EnumEquals", EnumEquals::checkInv0);
    res.put("EnumNotEquals", EnumNotEquals::checkInv0);
    return res;
  }

  @Tag("JavaGen")
  class EnumExpressionsJavaGenTest extends AbstractOCL2JavaGenTest<TrafficLight> implements EnumExpressionsTest {
    @Override
    protected List<TrafficLight> getContextObjects() {
      return MydomainManager.getTrafficLightList();
    }
  }

  @UsePlaywright
  @Tag("Playwright")
  class EnumExpressionsPlaywrightTest extends AbstractOCL2JavaE2ETest<TrafficLight> implements EnumExpressionsTest {
    @Override
    protected void _initConstraints() {
      registerGeneratedOclPages(getConstraintToChecker().keySet());
    }
  }

  default String getResourceFolder() {
    return "enumExpressions";
  }

  @Test
  default void testEnumEquals() throws IOException, SQLException {
    String constraintName = "EnumEquals";

    TrafficLight red = MydomainManager.trafficLightBuilder().color(TrafficLightColor.RED).build().get();
    TrafficLight yellow = MydomainManager.trafficLightBuilder().color(TrafficLightColor.YELLOW).build().get();
    TrafficLight green = MydomainManager.trafficLightBuilder().color(TrafficLightColor.GREEN).build().get();

    List<Long> violations = translateAndExecQueryFromPath(constraintName);

    assertTrue(violations.contains(red.getGemId()));
    assertTrue(violations.contains(yellow.getGemId()));
    assertFalse(violations.contains(green.getGemId()));
  }

  @Test
  default void testEnumNotEquals() throws IOException, SQLException {
    String constraintName = "EnumNotEquals";

    TrafficLight red = MydomainManager.trafficLightBuilder().color(TrafficLightColor.RED).build().get();
    TrafficLight yellow = MydomainManager.trafficLightBuilder().color(TrafficLightColor.YELLOW).build().get();
    TrafficLight green = MydomainManager.trafficLightBuilder().color(TrafficLightColor.GREEN).build().get();

    List<Long> violations = translateAndExecQueryFromPath(constraintName);

    assertTrue(violations.contains(red.getGemId()));
    assertFalse(violations.contains(yellow.getGemId()));
    assertFalse(violations.contains(green.getGemId()));
  }
}
