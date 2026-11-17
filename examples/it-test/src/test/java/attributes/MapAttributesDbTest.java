/* (c) https://github.com/MontiCore/monticore */
package attributes;

import attributes.test_infrastructure.AttributesNeo4JDBTestSetup;
import attributes.test_infrastructure.AttributesSQLDBTestSetup;
import attributes.test_infrastructure.AttributesTestSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public interface MapAttributesDbTest extends AttributesTestSetup {

  @DisplayName("Neo4J-MapAttributes")
  class Neo4JTestSuite extends AttributesNeo4JDBTestSetup implements MapAttributesDbTest {
    Neo4JTestSuite() {
      super(0);
    }
  }

  @DisplayName("SQL-MapAttributes")
  class SQLTestSuite extends AttributesSQLDBTestSetup implements MapAttributesDbTest {
    SQLTestSuite() {
      super(0);
    }
  }

  @Test
  default void testMapOfPrimitivesIsWrittenToDb() {
    Map<String, Integer> values = new LinkedHashMap<>();
    values.put("math", 3);
    values.put("art", -1);
    Map<Long, Double> weights = new LinkedHashMap<>();
    weights.put(9007199254740991L, 0.5);
    weights.put(-9007199254740991L, -1.5);
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().scores(values).weights(weights).build().get();

    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();

    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertNotSame(sut, clone);
    assertEquals(values, clone.getScoresMap());
    assertEquals(List.of("math", "art"), List.copyOf(clone.getScoresMap().keySet()));
    assertEquals(weights, clone.getWeightsMap());
  }

  @Test
  default void testPutToMapOfPrimitivesInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().scoresPut("math", 3).scoresPut("art", 1).build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeScoress());

    // put a new key: added at the end
    sut.putScores("music", 2);
    assertEquals(3, sut.sizeScoress());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of("math", "art", "music"), List.copyOf(clone.getScoresMap().keySet()));

    // put an existing key: the value is replaced in its position
    sut.putScores("math", 4);
    assertEquals(4, sut.getScores("math"));
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of("math", "art", "music"), List.copyOf(clone.getScoresMap().keySet()));
    assertEquals(4, clone.getScores("math"));
  }

  @Test
  default void testRemoveInMapOfPrimitivesInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder()
        .scoresPut("math", 3).scoresPut("art", 1).scoresPut("music", 2).build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(3, clone.sizeScoress());

    // remove the first key
    sut.removeScores("math");
    assertEquals(2, sut.sizeScoress());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeScoress());
    assertFalse(clone.containsKeyScores("math"));

    // remove the last key
    sut.removeScores("music");
    assertEquals(1, sut.sizeScoress());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(Map.of("art", 1), clone.getScoresMap());
  }

  @Test
  default void testMapOfStringsIsWrittenToDb() {
    Map<Byte, String> values = new LinkedHashMap<>();
    values.put((byte) 1, "A");
    values.put((byte) -128, "B");
    values.put((byte) 127, "C");
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().labels(values).build().get();

    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();

    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(values, clone.getLabelsMap());
    assertEquals(List.copyOf(values.keySet()), List.copyOf(clone.getLabelsMap().keySet()));
  }

  @Test
  default void testPutToMapOfStringsInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().labelsPut((byte) 1, "A").labelsPut((byte) 2, "B")
        .build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeLabelss());

    // put a new key: added at the end
    sut.putLabels((byte) 3, "X");
    assertEquals(3, sut.sizeLabelss());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of((byte) 1, (byte) 2, (byte) 3), List.copyOf(clone.getLabelsMap().keySet()));

    // put an existing key: the value is replaced in its position
    sut.putLabels((byte) 1, "Y");
    assertEquals("Y", sut.getLabels((byte) 1));
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of((byte) 1, (byte) 2, (byte) 3), List.copyOf(clone.getLabelsMap().keySet()));
    assertEquals("Y", clone.getLabels((byte) 1));
  }

  @Test
  default void testRemoveInMapOfStringsInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder()
        .labelsPut((byte) 1, "A").labelsPut((byte) 2, "B").labelsPut((byte) 3, "C").build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(3, clone.sizeLabelss());

    // remove the first key
    sut.removeLabels((byte) 1);
    assertEquals(2, sut.sizeLabelss());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeLabelss());
    assertFalse(clone.containsKeyLabels((byte) 1));

    // remove the last key
    sut.removeLabels((byte) 3);
    assertEquals(1, sut.sizeLabelss());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(Map.of((byte) 2, "B"), clone.getLabelsMap());
  }

  @Test
  default void testMapOfBooleansIsWrittenToDb() {
    Map<Short, Boolean> values = new LinkedHashMap<>();
    values.put((short) 2, true);
    values.put((short) -1, false);
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().flags(values).build().get();

    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();

    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(values, clone.getFlagsMap());
    assertEquals(List.of((short) 2, (short) -1), List.copyOf(clone.getFlagsMap().keySet()));
  }

  @Test
  default void testPutToMapOfBooleansInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().flagsPut((short) 1, true).flagsPut((short) 2, false)
        .build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeFlagss());

    // put a new key: added at the end
    sut.putFlags((short) 3, true);
    assertEquals(3, sut.sizeFlagss());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of((short) 1, (short) 2, (short) 3), List.copyOf(clone.getFlagsMap().keySet()));

    // put an existing key: the value is replaced in its position
    sut.putFlags((short) 1, false);
    assertFalse(sut.getFlags((short) 1));
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of((short) 1, (short) 2, (short) 3), List.copyOf(clone.getFlagsMap().keySet()));
    assertFalse(clone.getFlags((short) 1));
  }

  @Test
  default void testRemoveInMapOfBooleansInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder()
        .flagsPut((short) 1, true).flagsPut((short) 2, false).flagsPut((short) 3, true).build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(3, clone.sizeFlagss());

    // remove the first key
    sut.removeFlags((short) 1);
    assertEquals(2, sut.sizeFlagss());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeFlagss());
    assertFalse(clone.containsKeyFlags((short) 1));

    // remove the last key
    sut.removeFlags((short) 3);
    assertEquals(1, sut.sizeFlagss());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(Map.of((short) 2, false), clone.getFlagsMap());
  }

  @Test
  default void testMapOfEnumsIsWrittenToDb() {
    Map<Integer, MaritalStatus> values = new LinkedHashMap<>();
    values.put(7, MaritalStatus.MARRIED);
    values.put(-3, MaritalStatus.SINGLE);
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().statusById(values).build().get();

    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();

    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(values, clone.getStatusByIdMap());
    assertEquals(List.of(7, -3), List.copyOf(clone.getStatusByIdMap().keySet()));
  }

  @Test
  default void testPutToMapOfEnumsInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder()
        .statusByIdPut(1, MaritalStatus.SINGLE).statusByIdPut(2, MaritalStatus.MARRIED).build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeStatusByIds());

    // put a new key: added at the end
    sut.putStatusById(3, MaritalStatus.WIDOWED);
    assertEquals(3, sut.sizeStatusByIds());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of(1, 2, 3), List.copyOf(clone.getStatusByIdMap().keySet()));

    // put an existing key: the value is replaced in its position
    sut.putStatusById(1, MaritalStatus.DIVORCED);
    assertEquals(MaritalStatus.DIVORCED, sut.getStatusById(1));
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of(1, 2, 3), List.copyOf(clone.getStatusByIdMap().keySet()));
    assertEquals(MaritalStatus.DIVORCED, clone.getStatusById(1));
  }

  @Test
  default void testRemoveInMapOfEnumsInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().statusByIdPut(1, MaritalStatus.SINGLE)
        .statusByIdPut(2, MaritalStatus.MARRIED).statusByIdPut(3, MaritalStatus.WIDOWED).build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(3, clone.sizeStatusByIds());

    // remove the first key
    sut.removeStatusById(1);
    assertEquals(2, sut.sizeStatusByIds());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeStatusByIds());
    assertFalse(clone.containsKeyStatusById(1));

    // remove the last key
    sut.removeStatusById(3);
    assertEquals(1, sut.sizeStatusByIds());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(Map.of(2, MaritalStatus.MARRIED), clone.getStatusByIdMap());
  }

  @Test
  default void testMapOfDatesIsWrittenToDb() {
    Map<String, LocalDate> dates = new LinkedHashMap<>();
    dates.put("start", LocalDate.of(2026, 11, 17));
    dates.put("end", LocalDate.of(2027, 2, 28));
    Map<Integer, LocalDateTime> timestamps = new LinkedHashMap<>();
    timestamps.put(1, LocalDateTime.of(2026, 11, 17, 10, 30, 15));
    Map<String, LocalTime> times = new LinkedHashMap<>();
    times.put("lunch", LocalTime.of(12, 15));
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().dates(dates).timestamps(timestamps).times(times)
        .build().get();

    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();

    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(dates, clone.getDatesMap());
    assertEquals(List.of("start", "end"), List.copyOf(clone.getDatesMap().keySet()));
    assertEquals(timestamps, clone.getTimestampsMap());
    assertEquals(times, clone.getTimesMap());
  }

  @Test
  default void testPutToMapOfDatesInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().datesPut("start", LocalDate.of(2026, 11, 17))
        .datesPut("end", LocalDate.of(2027, 2, 28)).build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeDatess());

    // put a new key: added at the end
    sut.putDates("exam", LocalDate.of(2027, 2, 10));
    assertEquals(3, sut.sizeDatess());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of("start", "end", "exam"), List.copyOf(clone.getDatesMap().keySet()));

    // put an existing key: the value is replaced in its position
    sut.putDates("start", LocalDate.of(2026, 11, 18));
    assertEquals(LocalDate.of(2026, 11, 18), sut.getDates("start"));
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of("start", "end", "exam"), List.copyOf(clone.getDatesMap().keySet()));
    assertEquals(LocalDate.of(2026, 11, 18), clone.getDates("start"));
  }

  @Test
  default void testRemoveInMapOfDatesInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().datesPut("start", LocalDate.of(2026, 11, 17))
        .datesPut("exam", LocalDate.of(2027, 2, 10)).datesPut("end", LocalDate.of(2027, 2, 28)).build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(3, clone.sizeDatess());

    // remove the first key
    sut.removeDates("start");
    assertEquals(2, sut.sizeDatess());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeDatess());
    assertFalse(clone.containsKeyDates("start"));

    // remove the last key
    sut.removeDates("end");
    assertEquals(1, sut.sizeDatess());
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(Map.of("exam", LocalDate.of(2027, 2, 10)), clone.getDatesMap());
  }

  @Test
  default void testMapsWithDateKeysAreWrittenToDb() {
    Map<LocalDate, Integer> countsByDay = new LinkedHashMap<>();
    countsByDay.put(LocalDate.of(2026, 11, 17), 3);
    countsByDay.put(LocalDate.of(2026, 11, 16), 1);
    Map<LocalDateTime, String> notesByTime = new LinkedHashMap<>();
    notesByTime.put(LocalDateTime.of(2026, 11, 17, 10, 30, 15, 123_000_000), "start");
    Map<LocalTime, String> slots = new LinkedHashMap<>();
    slots.put(LocalTime.of(12, 15), "lunch");
    // Instant: microseconds, the precision of TIMESTAMP(6) WITH TIME ZONE
    Map<Instant, Double> readings = new LinkedHashMap<>();
    readings.put(Instant.parse("2026-11-17T09:30:15.123456Z"), 21.5);
    Map<String, Instant> lastSeen = new LinkedHashMap<>();
    lastSeen.put("sensor", Instant.parse("2026-11-17T09:30:15.123456Z"));
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().countsByDay(countsByDay).notesByTime(notesByTime)
        .slots(slots).readings(readings).lastSeen(lastSeen).build().get();

    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();

    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(countsByDay, clone.getCountsByDayMap());
    assertEquals(List.of(LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 16)),
        List.copyOf(clone.getCountsByDayMap().keySet()));
    assertEquals(notesByTime, clone.getNotesByTimeMap());
    assertEquals(slots, clone.getSlotsMap());
    assertEquals(readings, clone.getReadingsMap());
    assertEquals(lastSeen, clone.getLastSeenMap());
  }

  @Test
  default void testPutAndRemoveWithDateKeyInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().countsByDayPut(LocalDate.of(2026, 11, 17), 1)
        .countsByDayPut(LocalDate.of(2026, 11, 18), 1).build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();

    // put an existing key: the value is replaced in its position
    sut.putCountsByDay(LocalDate.of(2026, 11, 17), 2);
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(2, clone.sizeCountsByDays());
    assertEquals(List.of(LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 18)),
        List.copyOf(clone.getCountsByDayMap().keySet()));
    assertEquals(2, clone.getCountsByDay(LocalDate.of(2026, 11, 17)));

    // remove a key
    sut.removeCountsByDay(LocalDate.of(2026, 11, 17));
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(Map.of(LocalDate.of(2026, 11, 18), 1), clone.getCountsByDayMap());
  }

  @Test
  default void testPutAndRemoveWithInstantKeyInDb() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().readingsPut(Instant.parse("2026-11-17T09:30:15.123456Z"), 1.0)
        .readingsPut(Instant.parse("2026-11-17T10:00:00Z"), 2.0).build().get();
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();

    // put an existing key, as a new object: the value is replaced in its position
    sut.putReadings(Instant.parse("2026-11-17T09:30:15.123456Z"), 3.0);
    var clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(List.of(Instant.parse("2026-11-17T09:30:15.123456Z"), Instant.parse("2026-11-17T10:00:00Z")),
        List.copyOf(clone.getReadingsMap().keySet()));
    assertEquals(3.0, clone.getReadings(Instant.parse("2026-11-17T09:30:15.123456Z")));

    // remove a key
    sut.removeReadings(Instant.parse("2026-11-17T09:30:15.123456Z"));
    clone = dao.selectMapsOfSimpleTypes(sut.gemId).get();
    assertEquals(Map.of(Instant.parse("2026-11-17T10:00:00Z"), 2.0), clone.getReadingsMap());
  }

  @Test
  default void testDeleteRemovesTheEntries() {
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().scoresPut("math", 3).weightsPut(1L, 2.0).build().get();
    long gemId = sut.gemId;

    assertTrue(AttributesManager.deleteMapsOfSimpleTypes(gemId));
    var dao = AttributesDAOFactory.getMapsOfSimpleTypesDAO();
    assertTrue(dao.selectScores(gemId).isEmpty());
    assertTrue(dao.selectWeights(gemId).isEmpty());
  }
}
