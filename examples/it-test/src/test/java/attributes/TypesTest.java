/* (c) https://github.com/MontiCore/monticore */
package attributes;

import attributes.test_infrastructure.AttributesSQLDBTestSetup;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TypesTest extends AttributesSQLDBTestSetup {

  public TypesTest() {
    super(0);
  }

  @Test
  public void testMinimalJavaTimeDb(){
    MinimalJavaTime minimalJavaTime = AttributesManager.minimalJavaTimeBuilder()
        .bar_local_date(LocalDate.now())
        .bar_local_time(LocalTime.now())
        .bar_local_date_time(LocalDateTime.now())
        .bar_instant(Instant.parse("2026-11-17T09:30:15.123456Z"))
        .bar_duration(Duration.parse("PT1H30M0.000000001S"))
        .build().get();

    MinimalJavaTimeDAO dao = new MinimalJavaTimeDAO();
    MinimalJavaTime minimalJavaTime2 = dao.selectMinimalJavaTime(minimalJavaTime.getGemId()).get();
    assertNotSame(minimalJavaTime2, minimalJavaTime);

    assertEquals(minimalJavaTime.getBar_local_time().truncatedTo(ChronoUnit.MILLIS), minimalJavaTime2.getBar_local_time().truncatedTo(ChronoUnit.MILLIS));
    assertEquals(minimalJavaTime.getBar_local_date_time().truncatedTo(ChronoUnit.MILLIS), minimalJavaTime2.getBar_local_date_time().truncatedTo(ChronoUnit.MILLIS));
    assertEquals(minimalJavaTime.getBar_local_date(), minimalJavaTime2.getBar_local_date());
    assertEquals(minimalJavaTime.getBar_instant(), minimalJavaTime2.getBar_instant());
    assertEquals(minimalJavaTime.getBar_duration(), minimalJavaTime2.getBar_duration());
  }

  @Test
  public void testMinimalJavaTimeDeSer(){
    MinimalJavaTime minimalJavaTime = AttributesManager.minimalJavaTimeBuilder()
        .bar_local_date(LocalDate.now())
        .bar_local_time(LocalTime.now())
        .bar_local_date_time(LocalDateTime.now())
        .bar_instant(Instant.parse("2026-11-17T09:30:15.123456Z"))
        .bar_duration(Duration.parse("PT1H30M0.000000001S"))
        .build().get();

    AttributesBaseClassObjectMapper.init();

    MinimalJavaTimeDeSer deSer = new MinimalJavaTimeDeSer();
    String s = deSer.serialize(minimalJavaTime);
    MinimalJavaTime minimalJavaTime2 = deSer.deserialize(s).get();
    assertNotSame(minimalJavaTime, minimalJavaTime2);

    assertEquals(minimalJavaTime.getBar_local_time().truncatedTo(ChronoUnit.MILLIS), minimalJavaTime2.getBar_local_time().truncatedTo(ChronoUnit.MILLIS));
    assertEquals(minimalJavaTime.getBar_local_date_time().truncatedTo(ChronoUnit.MILLIS), minimalJavaTime2.getBar_local_date_time().truncatedTo(ChronoUnit.MILLIS));
    assertEquals(minimalJavaTime.getBar_local_date(), minimalJavaTime2.getBar_local_date());
    assertEquals(minimalJavaTime.getBar_instant(), minimalJavaTime2.getBar_instant());
    assertEquals(minimalJavaTime.getBar_duration(), minimalJavaTime2.getBar_duration());
  }

  @Test
  public void testDefaultValues() {
    Optional<ClassWithDefaults> clazz= AttributesManager.classWithDefaultsBuilder().build();
    assertTrue(clazz.isPresent());
  }

}
