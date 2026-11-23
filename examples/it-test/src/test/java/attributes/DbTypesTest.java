/* (c) https://github.com/MontiCore/monticore */
package attributes;

import attributes.test_infrastructure.AttributesNeo4JDBTestSetup;
import attributes.test_infrastructure.AttributesSQLDBTestSetup;
import attributes.test_infrastructure.AttributesTestSetup;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public interface DbTypesTest extends AttributesTestSetup {

  @DisplayName("Neo4J-DbTypes")
  class Neo4JTestSuite extends AttributesNeo4JDBTestSetup implements DbTypesTest {
    Neo4JTestSuite() {
      super(0);
    }
  }

  @DisplayName("SQL-DbTypes")
  class SQLTestSuite extends AttributesSQLDBTestSetup implements DbTypesTest {
    SQLTestSuite() {
      super(0);
    }
  }

  @Test
  default void testListOfPrimitivesIsWrittenToDb() {
    List<Integer> values = List.of(0, 0, 1, 2, 3, 4);
    var sut = AttributesManager.listOfPrimitivesBuilder().ints(values).build().get();

    var dao = AttributesDAOFactory.getListOfPrimitivesDAO();

    var clone = dao.selectListOfPrimitives(sut.gemId).get();
    assertEquals(values, clone.getIntsList());
  }

  @Test
  default void testAddToListOfPrimitivesInDb() {
    List<Integer> values = List.of(0, -1, -2, -3, -4);
    ListOfPrimitives sut = AttributesManager.listOfPrimitivesBuilder().ints(values).build().get();
    var dao = AttributesDAOFactory.getListOfPrimitivesDAO();
    var clone = dao.selectListOfPrimitives(sut.gemId).get();
    assertEquals(5, clone.getIntsList().size());

    // add element at the end
    sut.addInts(5, -5);
    assertEquals(6, sut.getIntsList().size());
    clone = dao.selectListOfPrimitives(sut.gemId).get();
    assertEquals(6, clone.getIntsList().size());

    // add element in the middle
    sut.addInts(1, 11);
    assertEquals(7, sut.getIntsList().size());
    assertEquals(11, sut.getInts(1));
    clone = dao.selectListOfPrimitives(sut.gemId).get();
    assertEquals(7, clone.getIntsList().size());
    assertEquals(11, clone.getInts(1));
  }

  @Test
  default void testDeleteInListOfPrimitivesInDb() {
    List<Integer> values = List.of(-1, -1, -2, -3, -4);
    var sut = AttributesManager.listOfPrimitivesBuilder().ints(values).build().get();

    var dao = AttributesDAOFactory.getListOfPrimitivesDAO();
    var clone = dao.selectListOfPrimitives(sut.gemId).get();
    assertEquals(5, clone.getIntsList().size());

    // delete at the beginning
    sut.removeInts(0);
    assertEquals(4, sut.getIntsList().size());
    clone = dao.selectListOfPrimitives(sut.gemId).get();
    assertEquals(4, clone.getIntsList().size());

    // delete at the end
    sut.removeInts(3);
    assertEquals(3, sut.getIntsList().size());
    clone = dao.selectListOfPrimitives(sut.gemId).get();
    assertEquals(3, clone.getIntsList().size());
  }

  @Test
  default void testOptionalOfprimitivesIsWritten() {
    Optional<Integer> value = Optional.of(-1);
    var sut = AttributesManager.optionalOfPrimitivesBuilder().iVal(value).build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfPrimitivesDAO();
    var clone = dao.selectOptionalOfPrimitives(sut.gemId).get();
    assertNotSame(sut, clone);

    assertTrue(clone.getIVal().isPresent());
    assertEquals(-1, clone.getIVal().get());
  }

  @Test
  default void testSetToOptionalOfPrimitive() {
    var sut = AttributesManager.optionalOfPrimitivesBuilder().build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfPrimitivesDAO();
    var clone = dao.selectOptionalOfPrimitives(sut.gemId).get();
    assertNotSame(sut, clone);
    assertFalse(clone.getIVal().isPresent());

    // set element
    sut.setIVal(Optional.of(-2));
    assertTrue(sut.getIVal().isPresent());

    clone = dao.selectOptionalOfPrimitives(sut.gemId).get();
    assertTrue(clone.getIVal().isPresent());
    assertEquals(-2, clone.getIVal().get());
  }

  @Test
  default void testDeleteInOptionalOfPrimitives() {
    Optional<Integer> value = Optional.of(-3);
    var sut = AttributesManager.optionalOfPrimitivesBuilder().iVal(value).build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfPrimitivesDAO();
    var clone = dao.selectOptionalOfPrimitives(sut.gemId).get();
    assertNotSame(sut, clone);
    assertTrue(clone.getIVal().isPresent());

    // delete element
    sut.setIVal(Optional.empty());
    clone = dao.selectOptionalOfPrimitives(sut.gemId).get();

    assertFalse(sut.getIVal().isPresent());
    assertFalse(clone.getIVal().isPresent());
  }

  @Test
  default void testListOfStringsIsWrittenToDb() {
    List<String> values = List.of("A", "B", "C");
    var sut = AttributesManager.listOfStringsBuilder().strings(values).build().get();

    var dao = AttributesDAOFactory.getListOfStringsDAO();

    var clone = dao.selectListOfStrings(sut.gemId).get();
    assertEquals(values, clone.getStringsList());
  }

  @Test
  default void testAddToListOfStringsInDb() {
    List<String> values = List.of("A", "B", "C");
    var sut = AttributesManager.listOfStringsBuilder().strings(values).build().get();
    var dao = AttributesDAOFactory.getListOfStringsDAO();
    var clone = dao.selectListOfStrings(sut.gemId).get();
    assertEquals(3, clone.getStringsList().size());

    // add element at the end
    sut.addStrings(3, "X");
    assertEquals(4, sut.getStringsList().size());
    clone = dao.selectListOfStrings(sut.gemId).get();
    assertEquals(4, clone.getStringsList().size());

    // add element in the middle
    sut.addStrings(1, "Y");
    assertEquals(5, sut.getStringsList().size());
    assertEquals("Y", sut.getStrings(1));
    clone = dao.selectListOfStrings(sut.gemId).get();
    assertEquals(5, clone.getStringsList().size());
    assertEquals("Y", clone.getStrings(1));
  }

  @Test
  default void testDeleteInListOfStringsInDb() {
    List<String> values = List.of("A", "B", "C");
    var sut = AttributesManager.listOfStringsBuilder().strings(values).build().get();
    var dao = AttributesDAOFactory.getListOfStringsDAO();
    var clone = dao.selectListOfStrings(sut.gemId).get();
    assertEquals(3, clone.getStringsList().size());

    // delete at the beginning
    sut.removeStrings(0);
    assertEquals(2, sut.getStringsList().size());
    clone = dao.selectListOfStrings(sut.gemId).get();
    assertEquals(2, clone.getStringsList().size());

    // delete at the end
    sut.removeStrings(1);
    assertEquals(1, sut.getStringsList().size());
    clone = dao.selectListOfStrings(sut.gemId).get();
    assertEquals(1, clone.getStringsList().size());
  }

  @Test
  default void testDeleteRemovesTheListEntries() {
    var sut = AttributesManager.listOfStringsBuilder().strings(List.of("A", "B")).build().get();
    long gemId = sut.gemId;

    assertTrue(AttributesManager.deleteListOfStrings(gemId));
    var dao = AttributesDAOFactory.getListOfStringsDAO();
    assertTrue(dao.selectStrings(gemId).isEmpty());
  }

  @Test
  default void testOptionalOfStringIsWritten() {
    Optional<String> value = Optional.of("A");
    var sut = AttributesManager.optionalOfStringBuilder().str(value).build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfStringDAO();
    var clone = dao.selectOptionalOfString(sut.gemId).get();
    assertNotSame(sut, clone);

    assertTrue(clone.getStr().isPresent());
    assertEquals("A", clone.getStr().get());
  }

  @Test
  default void testSetToOptionalOfString() {
    var sut = AttributesManager.optionalOfStringBuilder().build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfStringDAO();
    var clone = dao.selectOptionalOfString(sut.gemId).get();
    assertNotSame(sut, clone);
    assertFalse(clone.getStr().isPresent());

    sut.setStr(Optional.of("A"));
    clone = dao.selectOptionalOfString(sut.gemId).get();
    assertTrue(sut.getStr().isPresent());
    assertTrue(clone.getStr().isPresent());
  }

  @Test
  default void testDeleteInOptionalOfString() {
    Optional<String> value = Optional.of("A");
    var sut = AttributesManager.optionalOfStringBuilder().str(value).build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfStringDAO();
    var clone = dao.selectOptionalOfString(sut.gemId).get();
    assertNotSame(sut, clone);
    assertTrue(clone.getStr().isPresent());

    sut.setStr(Optional.empty());
    clone = dao.selectOptionalOfString(sut.gemId).get();
    assertFalse(sut.getStr().isPresent());
    assertFalse(clone.getStr().isPresent());
  }

  @Test
  default void testListOfDatesIsWritten() {
    List<LocalDate> values = Lists.newArrayList();
    values.add(LocalDate.now());
    values.add(LocalDate.of(2000, 1, 1));
    var sut = AttributesManager.listOfTimeBuilder().dates(values).build().get();

    // db
    var dao = AttributesDAOFactory.getListOfTimeDAO();
    var clone = dao.selectListOfTime(sut.gemId).get();
    assertNotSame(sut, clone);
    assertEquals(2, clone.getDatesList().size());
  }

  @Test
  default void testAddToListOfDates() {
    List<LocalDate> values = Lists.newArrayList();
    values.add(LocalDate.now());
    values.add(LocalDate.of(2000, 1, 1));
    var sut = AttributesManager.listOfTimeBuilder().dates(values).build().get();

    // db
    var dao = AttributesDAOFactory.getListOfTimeDAO();
    var clone = dao.selectListOfTime(sut.gemId).get();
    assertNotSame(sut, clone);
    assertNotSame(sut, clone);
    assertEquals(2, clone.getDatesList().size());

    // add element at the end
    sut.addDates(2, LocalDate.of(2000,2,2));
    assertEquals(3, sut.getDatesList().size());
    clone = dao.selectListOfTime(sut.gemId).get();
    assertEquals(3, clone.getDatesList().size());

    // add element in the middle
    sut.addDates(1, LocalDate.of(2000, 3, 3));
    assertEquals(4, sut.getDatesList().size());
    clone = dao.selectListOfTime(sut.gemId).get();
    assertEquals(4, clone.getDatesList().size());
  }

  @Test
  default void testDeleteInListOfDates() {
    List<LocalDate> values = List.of(LocalDate.now(), LocalDate.of(2000, 1, 1), LocalDate.of(2000, 2, 2));
    var sut = AttributesManager.listOfTimeBuilder().dates(values).build().get();

    // db
    var dao = AttributesDAOFactory.getListOfTimeDAO();
    var clone = dao.selectListOfTime(sut.gemId).get();
    assertNotSame(sut, clone);
    assertEquals(3, clone.getDatesList().size());

    // delete at the beginning
    sut.removeDates(0);
    assertEquals(2, sut.getDatesList().size());
    clone = dao.selectListOfTime(sut.gemId).get();
    assertEquals(2, clone.getDatesList().size());

    // delete at the end
    sut.removeDates(1);
    assertEquals(1, sut.getDatesList().size());
    clone = dao.selectListOfTime(sut.gemId).get();
    assertEquals(1, clone.getDatesList().size());
  }

  @Test
  default void testOptionalOfTimeIsWritten() {
    Optional<LocalDate> value = Optional.of(LocalDate.now());
    var sut = AttributesManager.optionalOfTimeBuilder().date(value).build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfTimeDAO();
    var clone = dao.selectOptionalOfTime(sut.gemId).get();
    assertNotSame(sut, clone);
    assertTrue(clone.getDate().isPresent());
  }

  @Test
  default void testSetToOptionalOfTime() {
    var sut = AttributesManager.optionalOfTimeBuilder().build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfTimeDAO();
    var clone = dao.selectOptionalOfTime(sut.gemId).get();
    assertNotSame(sut, clone);
    assertFalse(clone.getDate().isPresent());

    // set element
    sut.setDate(Optional.of(LocalDate.now()));
    assertTrue(sut.getDate().isPresent());
    clone = dao.selectOptionalOfTime(sut.gemId).get();
    assertTrue(clone.getDate().isPresent());
  }

  @Test
  default void testDeleteInOptionalOfTime() {
    Optional<LocalDate> value = Optional.of(LocalDate.now());
    var sut = AttributesManager.optionalOfTimeBuilder().date(value).build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfTimeDAO();
    var clone = dao.selectOptionalOfTime(sut.gemId).get();
    assertNotSame(sut, clone);
    assertTrue(clone.getDate().isPresent());

    // delete element
    sut.setDate(Optional.empty());
    assertFalse(sut.getDate().isPresent());
    clone = dao.selectOptionalOfTime(sut.gemId).get();
    assertFalse(clone.getDate().isPresent());
  }

  @Test
  default void testListOfBooleansIsWritten() {
    List<Boolean> values = List.of(true, false, true);
    var sut = AttributesManager.listOfBooleansBuilder().bVals(values).build().get();

    // db
    var dao = AttributesDAOFactory.getListOfBooleansDAO();
    var clone = dao.selectListOfBooleans(sut.gemId).get();
    assertNotSame(sut, clone);
    assertEquals(values, clone.getBValsList());
  }

  @Test
  default void testAddToListOfBooleans() {
    List<Boolean> values = List.of(true, false, true);
    var sut = AttributesManager.listOfBooleansBuilder().bVals(values).build().get();

    // db
    var dao = AttributesDAOFactory.getListOfBooleansDAO();
    var clone = dao.selectListOfBooleans(sut.gemId).get();
    assertNotSame(sut, clone);
    assertEquals(3, clone.getBValsList().size());

    // add element at the end
    sut.addBVals(3, false);
    assertEquals(4, sut.getBValsList().size());
    clone = dao.selectListOfBooleans(sut.gemId).get();
    assertEquals(4, clone.getBValsList().size());

    // add element in the middle
    sut.addBVals(1, true);
    assertEquals(5, sut.getBValsList().size());
    assertEquals(true, sut.getBVals(1));
    clone = dao.selectListOfBooleans(sut.gemId).get();
    assertEquals(5, clone.getBValsList().size());
    assertEquals(true, clone.getBVals(1));
  }

  @Test
  default void testDeleteInListOfBooleans() {
    List<Boolean> values = List.of(true, false, true);
    var sut = AttributesManager.listOfBooleansBuilder().bVals(values).build().get();

    // db
    var dao = AttributesDAOFactory.getListOfBooleansDAO();
    var clone = dao.selectListOfBooleans(sut.gemId).get();
    assertNotSame(sut, clone);
    assertEquals(3, clone.getBValsList().size());

    // delete at the beginning
    sut.removeBVals(0);
    assertEquals(2, sut.getBValsList().size());
    clone = dao.selectListOfBooleans(sut.gemId).get();
    assertEquals(2, clone.getBValsList().size());

    // delete at the end
    sut.removeBVals(1);
    assertEquals(1, sut.getBValsList().size());
    clone = dao.selectListOfBooleans(sut.gemId).get();
    assertEquals(1, clone.getBValsList().size());
  }

  @Test
  default void testOptionalOfBooleanIsWritten() {
    Optional<Boolean> value = Optional.of(true);
    var sut = AttributesManager.optionalOfBooleanBuilder().bVal(value).build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfBooleanDAO();
    var clone = dao.selectOptionalOfBoolean(sut.gemId).get();
    assertNotSame(sut, clone);

    assertTrue(clone.getBVal().isPresent());
    assertEquals(true, clone.getBVal().get());
  }

  @Test
  default void testSetToOptionalOfBoolean() {
    var sut = AttributesManager.optionalOfBooleanBuilder().build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfBooleanDAO();
    var clone = dao.selectOptionalOfBoolean(sut.gemId).get();
    assertNotSame(sut, clone);
    assertFalse(clone.getBVal().isPresent());

    // set element
    sut.setBVal(Optional.of(true));
    assertTrue(sut.getBVal().isPresent());
    clone = dao.selectOptionalOfBoolean(sut.gemId).get();
    assertTrue(clone.getBVal().isPresent());
    assertEquals(true, clone.getBVal().get());
  }

  @Test
  default void testDeleteInOptionalOfBoolean() {
    Optional<Boolean> value = Optional.of(true);
    var sut = AttributesManager.optionalOfBooleanBuilder().bVal(value).build().get();

    // db
    var dao = AttributesDAOFactory.getOptionalOfBooleanDAO();
    var clone = dao.selectOptionalOfBoolean(sut.gemId).get();
    assertNotSame(sut, clone);
    assertTrue(clone.getBVal().isPresent());

    // delete element
    sut.setBVal(Optional.empty());
    assertFalse(sut.getBVal().isPresent());
    clone = dao.selectOptionalOfBoolean(sut.gemId).get();
    assertFalse(clone.getBVal().isPresent());
  }

  @Test
  default void testMinimalJavaTimeDb(){
    MinimalJavaTime sut = AttributesManager.minimalJavaTimeBuilder()
        .bar_local_date(LocalDate.now())
        .bar_local_time(LocalTime.now())
        .bar_local_date_time(LocalDateTime.now())
        .bar_instant(Instant.parse("2026-11-17T09:30:15.123456Z"))
        .build().get();

    // db
    var dao = AttributesDAOFactory.getMinimalJavaTimeDAO();
    var clone = dao.selectMinimalJavaTime(sut.gemId).get();
    assertNotSame(sut, clone);

    assertEquals(sut.getBar_local_time().truncatedTo(ChronoUnit.MILLIS), clone.getBar_local_time().truncatedTo(ChronoUnit.MILLIS));
    assertEquals(sut.getBar_local_date_time().truncatedTo(ChronoUnit.MILLIS), clone.getBar_local_date_time().truncatedTo(ChronoUnit.MILLIS));
    assertEquals(sut.getBar_local_date(), clone.getBar_local_date());
    assertEquals(sut.getBar_instant(), clone.getBar_instant());
  }
}
