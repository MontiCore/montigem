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
import java.util.List;
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
        .bar_list_instant(List.of(Instant.parse("1969-12-31T23:59:59.5Z"), Instant.ofEpochSecond(0, 1)))
        .bar_opt_instant(Optional.of(Instant.parse("2026-11-18T00:00:00Z")))
        .bar_duration(Duration.parse("PT1H30M0.000000001S"))
        .bar_list_duration(List.of(Duration.ofSeconds(-90, 250_000_000), Duration.ZERO))
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
    assertEquals(minimalJavaTime.getBar_list_instantList(), minimalJavaTime2.getBar_list_instantList());
    assertEquals(minimalJavaTime.getBar_opt_instant(), minimalJavaTime2.getBar_opt_instant());
    assertEquals(minimalJavaTime.getBar_list_durationList(), minimalJavaTime2.getBar_list_durationList());
    assertEquals(Optional.empty(), minimalJavaTime2.getBar_opt_duration());
  }

  @Test
  public void testDefaultValues() {
    Optional<ClassWithDefaults> clazz= AttributesManager.classWithDefaultsBuilder().build();
    assertTrue(clazz.isPresent());
  }

}
