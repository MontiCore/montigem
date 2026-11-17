/* (c) https://github.com/MontiCore/monticore */
package attributes;

import attributes.test_infrastructure.AttributesMockTestSetup;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class TypesSyncTest extends AttributesMockTestSetup {
  public TypesSyncTest() {
    super(1);
  }

  @Test
  public void testJavaTimeIsWritten() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.minimalJavaTimeBuilder()
        .bar_local_date(LocalDate.of(2026, 11, 17))
        .bar_local_time(LocalTime.of(9, 30, 15, 123_456_789))
        .bar_local_date_time(LocalDateTime.of(2026, 11, 17, 9, 30, 15, 123_456_789))
        .bar_instant(Instant.parse("2026-11-17T09:30:15.123456789Z"))
        .build().get();

    // check in backend: exact to the nanosecond
    getBackend().activate();
    var sutServer = AttributesManager.getMinimalJavaTimeSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(sut.getBar_local_date(), sutServer.getBar_local_date());
    assertEquals(sut.getBar_local_time(), sutServer.getBar_local_time());
    assertEquals(sut.getBar_local_date_time(), sutServer.getBar_local_date_time());
    assertEquals(sut.getBar_instant(), sutServer.getBar_instant());
  }

  @Test
  public void testListOfPrimitivesIsWritten() {
    // client
    getClients(0).activate();
    List<Integer> values = List.of(0, 0, 1, 2, 3, 4);
    var sut = AttributesManager.listOfPrimitivesBuilder().ints(values).build().get();

    // check in backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfPrimitivesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(values, sutServer.getIntsList());
  }

  @Test
  public void testAddToListOfPrimitives() {
    // client
    getClients(0).activate();
    List<Integer> values = List.of(0, -1, -2, -3, -4);
    ListOfPrimitives sut = AttributesManager.listOfPrimitivesBuilder().ints(values).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfPrimitivesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(5, sutServer.getIntsList().size());

    // client: add element at the end
    getClients(0).activate();

    sut.addInts(5, -5);
    assertEquals(6, sut.getIntsList().size());
    assertEquals(6, sutServer.getIntsList().size());

    // add element in the middle
    sut.addInts(1, 11);
    assertEquals(7, sut.getIntsList().size());
    assertEquals(11, sut.getInts(1));
    assertEquals(7, sutServer.getIntsList().size());
    assertEquals(11, sutServer.getInts(1));
  }

  @Test
  public void testDeleteInListOfPrimitives() {
    // client
    getClients(0).activate();
    List<Integer> values = List.of(-1, -1, -2, -3, -4);
    var sut = AttributesManager.listOfPrimitivesBuilder().ints(values).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfPrimitivesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(5, sutServer.getIntsList().size());

    // client: delete at the beginning
    getClients(0).activate();
    sut.removeInts(0);
    assertEquals(4, sut.getIntsList().size());
    assertEquals(4, sutServer.getIntsList().size());

    // delete at the end
    sut.removeInts(3);
    assertEquals(3, sut.getIntsList().size());
    assertEquals(3, sutServer.getIntsList().size());
  }

  @Test
  public void testOptionalOfPrimitivesIsWritten() {
    // client
    getClients(0).activate();
    Optional<Integer> value = Optional.of(-1);
    var sut = AttributesManager.optionalOfPrimitivesBuilder().iVal(value).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfPrimitivesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertTrue(sutServer.getIVal().isPresent());
    assertEquals(-1, sutServer.getIVal().get());
  }

  @Test
  public void testSetToOptionalOfPrimitive() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.optionalOfPrimitivesBuilder().build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfPrimitivesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertFalse(sutServer.getIVal().isPresent());

    // client: set element
    getClients(0).activate();
    sut.setIVal(Optional.of(-2));
    assertTrue(sut.getIVal().isPresent());
    assertTrue(sutServer.getIVal().isPresent());
    assertEquals(-2, sutServer.getIVal().get());
  }

  @Test
  public void testDeleteInOptionalOfPrimitives() {
    // client
    getClients(0).activate();
    Optional<Integer> value = Optional.of(-3);
    var sut = AttributesManager.optionalOfPrimitivesBuilder().iVal(value).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfPrimitivesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertTrue(sutServer.getIVal().isPresent());

    // client: delete element
    getClients(0).activate();
    sut.setIVal(Optional.empty());
    assertFalse(sut.getIVal().isPresent());
    assertFalse(sutServer.getIVal().isPresent());
  }

  @Test
  public void testListOfStringsIsWritten() {
    // client
    getClients(0).activate();
    List<String> values = List.of("A", "B", "C");
    var sut = AttributesManager.listOfStringsBuilder().strings(values).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfStringsSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(3, sutServer.getStringsList().size());
    assertEquals(values, sutServer.getStringsList());
  }

  @Test
  public void testAddToListOfStrings() {
    // client
    getClients(0).activate();
    List<String> values = List.of("A", "B", "C");
    var sut = AttributesManager.listOfStringsBuilder().strings(values).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfStringsSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(3, sutServer.getStringsList().size());

    // client: add element at the end
    getClients(0).activate();
    sut.addStrings(3, "X");
    assertEquals(4, sut.getStringsList().size());
    assertEquals(4, sutServer.getStringsList().size());

    // add element in the middle
    sut.addStrings(1, "Y");
    assertEquals(5, sut.getStringsList().size());
    assertEquals("Y", sut.getStrings(1));
    assertEquals(5, sutServer.getStringsList().size());
    assertEquals("Y", sutServer.getStrings(1));
  }

  @Test
  public void testDeleteInListOfStrings() {
    // client
    getClients(0).activate();
    List<String> values = List.of("A", "B", "C");
    var sut = AttributesManager.listOfStringsBuilder().strings(values).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfStringsSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(3, sutServer.getStringsList().size());

    // client: delete at the beginning
    getClients(0).activate();
    sut.removeStrings(0);
    assertEquals(2, sut.getStringsList().size());
    assertEquals(2, sutServer.getStringsList().size());

    // delete at the end
    sut.removeStrings(1);
    assertEquals(1, sut.getStringsList().size());
    assertEquals(1, sutServer.getStringsList().size());
  }

  @Test
  public void testOptionalOfStringIsWritten() {
    // client
    getClients(0).activate();
    Optional<String> value = Optional.of("A");
    var sut = AttributesManager.optionalOfStringBuilder().str(value).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfStringSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertTrue(sutServer.getStr().isPresent());
    assertEquals("A", sutServer.getStr().get());
  }

  @Test
  public void testSetToOptionalOfString() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.optionalOfStringBuilder().build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfStringSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertFalse(sutServer.getStr().isPresent());

    // client: set element
    getClients(0).activate();
    sut.setStr(Optional.of("A"));
    assertTrue(sut.getStr().isPresent());
    assertTrue(sutServer.getStr().isPresent());

  }

  @Test
  public void testDeleteInOptionalOfString() {
    // client
    getClients(0).activate();
    Optional<String> value = Optional.of("A");
    var sut = AttributesManager.optionalOfStringBuilder().str(value).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfStringSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertTrue(sutServer.getStr().isPresent());

    // client: delete element
    getClients(0).activate();
    sut.setStr(Optional.empty());
    assertFalse(sut.getStr().isPresent());
    assertFalse(sutServer.getStr().isPresent());
 }

  @Test
  public void testListOfTimesIsWritten() {
    // client
    getClients(0).activate();
    List<LocalDate> values = Lists.newArrayList();
    values.add(LocalDate.now());
    values.add(LocalDate.of(2000, 1, 1));
    var sut = AttributesManager.listOfTimeBuilder().dates(values).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfTimeSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(2, sutServer.getDatesList().size());
  }

  @Test
  public void testAddToListOfTimes() {
    // client
    getClients(0).activate();
    List<LocalDate> values = Lists.newArrayList();
    values.add(LocalDate.now());
    values.add(LocalDate.of(2000, 1, 1));
    var sut = AttributesManager.listOfTimeBuilder().dates(values).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfTimeSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(2, sutServer.getDatesList().size());

    // client: add element at the end
    getClients(0).activate();
    sut.addDates(2, LocalDate.of(2000,2,2));
    assertEquals(3, sut.getDatesList().size());
    assertEquals(3, sutServer.getDatesList().size());

    // add element in the middle
    sut.addDates(1, LocalDate.of(2000, 3, 3));
    assertEquals(4, sut.getDatesList().size());
    assertEquals(4, sutServer.getDatesList().size());
  }

  @Test
  public void testDeleteInListOfTimes() {
    // client
    getClients(0).activate();
    List<LocalDate> values = List.of(LocalDate.now(), LocalDate.of(2000, 1, 1), LocalDate.of(2000, 2, 2));
    var sut = AttributesManager.listOfTimeBuilder().dates(values).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfTimeSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(3, sutServer.getDatesList().size());

    // client: delete at the beginning
    getClients(0).activate();
    sut.removeDates(0);
    assertEquals(2, sut.getDatesList().size());
    assertEquals(2, sutServer.getDatesList().size());

    // delete at the end
    sut.removeDates(1);
    assertEquals(1, sut.getDatesList().size());
    assertEquals(1, sutServer.getDatesList().size());
  }

  @Test
  public void testOptionalOfTimeIsWritten() {
    // client
    getClients(0).activate();
    Optional<LocalDate> value = Optional.of(LocalDate.now());
    var sut = AttributesManager.optionalOfTimeBuilder().date(value).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfTimeSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertTrue(sutServer.getDate().isPresent());
  }

  @Test
  public void testSetToOptionalOfTime() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.optionalOfTimeBuilder().build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfTimeSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertFalse(sutServer.getDate().isPresent());

    // client: set element
    getClients(0).activate();
    sut.setDate(Optional.of(LocalDate.now()));
    assertTrue(sut.getDate().isPresent());
    assertTrue(sutServer.getDate().isPresent());

  }

  @Test
  public void testDeleteInOptionalOfTime() {
    // client
    getClients(0).activate();
    Optional<LocalDate> value = Optional.of(LocalDate.now());
    var sut = AttributesManager.optionalOfTimeBuilder().date(value).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfTimeSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertTrue(sutServer.getDate().isPresent());

    // client: delete element
    getClients(0).activate();
    sut.setDate(Optional.empty());
    assertFalse(sut.getDate().isPresent());
    assertFalse(sutServer.getDate().isPresent());
  }

  @Test
  public void testListOfBooleansIsWritten() {
    // client
    getClients(0).activate();
    List<Boolean> values = List.of(true, false, true);
    var sut = AttributesManager.listOfBooleansBuilder().bVals(values).build().get();

    // check in backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfBooleansSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(values, sutServer.getBValsList());
  }

  @Test
  public void testAddToListOfBooleans() {
    // client
    getClients(0).activate();
    List<Boolean> values = List.of(true, false, true);
    var sut = AttributesManager.listOfBooleansBuilder().bVals(values).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfBooleansSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(3, sutServer.getBValsList().size());

    // client: add element at the end
    getClients(0).activate();

    sut.addBVals(3, false);
    assertEquals(4, sut.getBValsList().size());
    assertEquals(4, sutServer.getBValsList().size());

    // add element in the middle
    sut.addBVals(1, true);
    assertEquals(5, sut.getBValsList().size());
    assertEquals(true, sut.getBVals(1));
    assertEquals(5, sutServer.getBValsList().size());
    assertEquals(true, sutServer.getBVals(1));
  }

  @Test
  public void testDeleteInListOfBooleans() {
    // client
    getClients(0).activate();
    List<Boolean> values = List.of(true, false, true);
    var sut = AttributesManager.listOfBooleansBuilder().bVals(values).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getListOfBooleansSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(3, sutServer.getBValsList().size());

    // client: delete at the beginning
    getClients(0).activate();
    sut.removeBVals(0);
    assertEquals(2, sut.getBValsList().size());
    assertEquals(2, sutServer.getBValsList().size());

    // delete at the end
    sut.removeBVals(1);
    assertEquals(1, sut.getBValsList().size());
    assertEquals(1, sutServer.getBValsList().size());
  }

  @Test
  public void testOptionalOfBooleanIsWritten() {
    // client
    getClients(0).activate();
    Optional<Boolean> value = Optional.of(true);
    var sut = AttributesManager.optionalOfBooleanBuilder().bVal(value).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfBooleanSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertTrue(sutServer.getBVal().isPresent());
    assertEquals(true, sutServer.getBVal().get());
  }

  @Test
  public void testSetToOptionalOfBoolean() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.optionalOfBooleanBuilder().build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfBooleanSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertFalse(sutServer.getBVal().isPresent());

    // client: set element
    getClients(0).activate();
    sut.setBVal(Optional.of(true));
    assertTrue(sut.getBVal().isPresent());
    assertTrue(sutServer.getBVal().isPresent());
    assertEquals(true, sutServer.getBVal().get());
  }

  @Test
  public void testDeleteInOptionalOfBoolean() {
    // client
    getClients(0).activate();
    Optional<Boolean> value = Optional.of(true);
    var sut = AttributesManager.optionalOfBooleanBuilder().bVal(value).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getOptionalOfBooleanSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertTrue(sutServer.getBVal().isPresent());

    // client: delete element
    getClients(0).activate();
    sut.setBVal(Optional.empty());
    assertFalse(sut.getBVal().isPresent());
    assertFalse(sutServer.getBVal().isPresent());
  }

}
