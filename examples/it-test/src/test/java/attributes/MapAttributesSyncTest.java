/* (c) https://github.com/MontiCore/monticore */
package attributes;

import attributes.commands.MapsOfSimpleTypes_putScores;
import attributes.commands.MapsOfSimpleTypes_putWeights;
import attributes.test_infrastructure.AttributesMockTestSetup;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.Test;
import umlp.backendrte.command.CommandManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class MapAttributesSyncTest extends AttributesMockTestSetup {
  public MapAttributesSyncTest() {
    super(2);
  }

  @Test
  public void testMapOfPrimitivesIsWritten() {
    // client
    getClients(0).activate();
    Map<String, Integer> values = new LinkedHashMap<>();
    values.put("math", 3);
    values.put("art", -1);
    Map<Long, Double> weights = new LinkedHashMap<>();
    weights.put(9007199254740991L, 0.5);
    weights.put(-9007199254740991L, -1.5);
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().scores(values).weights(weights).build().get();

    // check in backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(values, sutServer.getScoresMap());
    assertEquals(List.of("math", "art"), List.copyOf(sutServer.getScoresMap().keySet()));
    assertEquals(weights, sutServer.getWeightsMap());
  }

  @Test
  public void testPutToMapOfPrimitives() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().scoresPut("math", 3).scoresPut("art", 1).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(2, sutServer.sizeScoress());

    // client: put a new key, added at the end
    getClients(0).activate();
    sut.putScores("music", 2);
    assertEquals(3, sut.sizeScoress());
    assertEquals(List.of("math", "art", "music"), List.copyOf(sutServer.getScoresMap().keySet()));

    // put an existing key: the value is replaced in its position
    sut.putScores("math", 4);
    assertEquals(4, sut.getScores("math"));
    assertEquals(List.of("math", "art", "music"), List.copyOf(sutServer.getScoresMap().keySet()));
    assertEquals(4, sutServer.getScores("math"));
  }

  @Test
  public void testRemoveInMapOfPrimitives() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.mapsOfSimpleTypesBuilder()
        .scoresPut("math", 3).scoresPut("art", 1).scoresPut("music", 2).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(3, sutServer.sizeScoress());

    // client: remove the first key
    getClients(0).activate();
    sut.removeScores("math");
    assertEquals(2, sut.sizeScoress());
    assertEquals(2, sutServer.sizeScoress());
    assertFalse(sutServer.containsKeyScores("math"));

    // remove the last key
    sut.removeScores("music");
    assertEquals(1, sut.sizeScoress());
    assertEquals(Map.of("art", 1), sutServer.getScoresMap());
  }

  @Test
  public void testMapOfStringsIsWritten() {
    // client
    getClients(0).activate();
    Map<Byte, String> values = new LinkedHashMap<>();
    values.put((byte) 1, "A");
    values.put((byte) -128, "B");
    values.put((byte) 127, "C");
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().labels(values).build().get();

    // check in backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(values, sutServer.getLabelsMap());
    assertEquals(List.copyOf(values.keySet()), List.copyOf(sutServer.getLabelsMap().keySet()));
  }

  @Test
  public void testPutToMapOfStrings() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().labelsPut((byte) 1, "A").labelsPut((byte) 2, "B")
        .build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(2, sutServer.sizeLabelss());

    // client: put a new key, added at the end
    getClients(0).activate();
    sut.putLabels((byte) 3, "X");
    assertEquals(3, sut.sizeLabelss());
    assertEquals(List.of((byte) 1, (byte) 2, (byte) 3), List.copyOf(sutServer.getLabelsMap().keySet()));

    // put an existing key: the value is replaced in its position
    sut.putLabels((byte) 1, "Y");
    assertEquals("Y", sut.getLabels((byte) 1));
    assertEquals(List.of((byte) 1, (byte) 2, (byte) 3), List.copyOf(sutServer.getLabelsMap().keySet()));
    assertEquals("Y", sutServer.getLabels((byte) 1));
  }

  @Test
  public void testRemoveInMapOfStrings() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.mapsOfSimpleTypesBuilder()
        .labelsPut((byte) 1, "A").labelsPut((byte) 2, "B").labelsPut((byte) 3, "C").build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(3, sutServer.sizeLabelss());

    // client: remove the first key
    getClients(0).activate();
    sut.removeLabels((byte) 1);
    assertEquals(2, sut.sizeLabelss());
    assertEquals(2, sutServer.sizeLabelss());
    assertFalse(sutServer.containsKeyLabels((byte) 1));

    // remove the last key
    sut.removeLabels((byte) 3);
    assertEquals(1, sut.sizeLabelss());
    assertEquals(Map.of((byte) 2, "B"), sutServer.getLabelsMap());
  }

  @Test
  public void testMapOfBooleansIsWritten() {
    // client
    getClients(0).activate();
    Map<Short, Boolean> values = new LinkedHashMap<>();
    values.put((short) 2, true);
    values.put((short) -1, false);
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().flags(values).build().get();

    // check in backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(values, sutServer.getFlagsMap());
    assertEquals(List.of((short) 2, (short) -1), List.copyOf(sutServer.getFlagsMap().keySet()));
  }

  @Test
  public void testPutToMapOfBooleans() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().flagsPut((short) 1, true).flagsPut((short) 2, false)
        .build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(2, sutServer.sizeFlagss());

    // client: put a new key, added at the end
    getClients(0).activate();
    sut.putFlags((short) 3, true);
    assertEquals(3, sut.sizeFlagss());
    assertEquals(List.of((short) 1, (short) 2, (short) 3), List.copyOf(sutServer.getFlagsMap().keySet()));

    // put an existing key: the value is replaced in its position
    sut.putFlags((short) 1, false);
    assertFalse(sut.getFlags((short) 1));
    assertEquals(List.of((short) 1, (short) 2, (short) 3), List.copyOf(sutServer.getFlagsMap().keySet()));
    assertFalse(sutServer.getFlags((short) 1));
  }

  @Test
  public void testRemoveInMapOfBooleans() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.mapsOfSimpleTypesBuilder()
        .flagsPut((short) 1, true).flagsPut((short) 2, false).flagsPut((short) 3, true).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(3, sutServer.sizeFlagss());

    // client: remove the first key
    getClients(0).activate();
    sut.removeFlags((short) 1);
    assertEquals(2, sut.sizeFlagss());
    assertEquals(2, sutServer.sizeFlagss());
    assertFalse(sutServer.containsKeyFlags((short) 1));

    // remove the last key
    sut.removeFlags((short) 3);
    assertEquals(1, sut.sizeFlagss());
    assertEquals(Map.of((short) 2, false), sutServer.getFlagsMap());
  }

  @Test
  public void testMapOfEnumsIsWritten() {
    // client
    getClients(0).activate();
    Map<Integer, MaritalStatus> values = new LinkedHashMap<>();
    values.put(7, MaritalStatus.MARRIED);
    values.put(-3, MaritalStatus.SINGLE);
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().statusById(values).build().get();

    // check in backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(values, sutServer.getStatusByIdMap());
    assertEquals(List.of(7, -3), List.copyOf(sutServer.getStatusByIdMap().keySet()));
  }

  @Test
  public void testPutToMapOfEnums() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.mapsOfSimpleTypesBuilder()
        .statusByIdPut(1, MaritalStatus.SINGLE).statusByIdPut(2, MaritalStatus.MARRIED).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(2, sutServer.sizeStatusByIds());

    // client: put a new key, added at the end
    getClients(0).activate();
    sut.putStatusById(3, MaritalStatus.WIDOWED);
    assertEquals(3, sut.sizeStatusByIds());
    assertEquals(List.of(1, 2, 3), List.copyOf(sutServer.getStatusByIdMap().keySet()));

    // put an existing key: the value is replaced in its position
    sut.putStatusById(1, MaritalStatus.DIVORCED);
    assertEquals(MaritalStatus.DIVORCED, sut.getStatusById(1));
    assertEquals(List.of(1, 2, 3), List.copyOf(sutServer.getStatusByIdMap().keySet()));
    assertEquals(MaritalStatus.DIVORCED, sutServer.getStatusById(1));
  }

  @Test
  public void testRemoveInMapOfEnums() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().statusByIdPut(1, MaritalStatus.SINGLE)
        .statusByIdPut(2, MaritalStatus.MARRIED).statusByIdPut(3, MaritalStatus.WIDOWED).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    assertNotSame(sut, sutServer);
    assertEquals(3, sutServer.sizeStatusByIds());

    // client: remove the first key
    getClients(0).activate();
    sut.removeStatusById(1);
    assertEquals(2, sut.sizeStatusByIds());
    assertEquals(2, sutServer.sizeStatusByIds());
    assertFalse(sutServer.containsKeyStatusById(1));

    // remove the last key
    sut.removeStatusById(3);
    assertEquals(1, sut.sizeStatusByIds());
    assertEquals(Map.of(2, MaritalStatus.MARRIED), sutServer.getStatusByIdMap());
  }

  @Test
  public void testServerChangesReachClients() {
    // client
    getClients(0).activate();
    var sut = AttributesManager.mapsOfSimpleTypesBuilder().scoresPut("math", 3).build().get();

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    sutServer.putScores("art", 1);
    sutServer.removeScores("math");

    assertEquals(Map.of("art", 1), sut.getScoresMap());
  }

  @Test
  public void testInvalidKeysAreRejected() {
    long unsafe = 9007199254740992L;
    // client: owner and builder refuse null and unsafe long keys
    getClients(0).activate();
    var builder = AttributesManager.mapsOfSimpleTypesBuilder();
    assertLogs("0xA7332", () -> builder.weightsPut(unsafe, 1.0));
    var sut = builder.build().get();
    assertLogs("0xA7332", () -> sut.putWeights(unsafe, 1.0));
    assertLogs("0xA7331", () -> sut.putScores(null, 1));
    assertLogs("0xA7331", () -> sut.putScores("math", null));
    assertEquals(0, sut.sizeWeightss());
    assertEquals(0, sut.sizeScoress());

    // other client
    getClients(1).activate();
    var sutOther = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);

    // client: the server rejects such commands, they are neither executed nor sent on
    getClients(0).activate();
    assertLogs("0xA7335", () -> assertFalse(CommandManager.executeCommand(
        new MapsOfSimpleTypes_putWeights(sut.gemId, unsafe, 1.0)).isSuccessful()));
    assertLogs("0xA7334", () -> assertFalse(CommandManager.executeCommand(
        new MapsOfSimpleTypes_putScores(sut.gemId, null, 1)).isSuccessful()));
    // neither the server nor the other client ran put<Attr>, which would have logged these errors
    assertTrue(Log.getFindings().stream().noneMatch(f -> f.getMsg().startsWith("0xA7331")
        || f.getMsg().startsWith("0xA7332")));

    // backend
    getBackend().activate();
    var sutServer = AttributesManager.getMapsOfSimpleTypesSure(sut.gemId);
    for (var o : List.of(sutServer, sutOther)) {
      assertEquals(0, o.sizeWeightss());
      assertEquals(0, o.sizeScoress());
    }
  }

  /** runs an action that is expected to log a finding starting with message */
  protected void assertLogs(String message, Runnable action) {
    boolean failQuick = Log.isFailQuickEnabled();
    Log.enableFailQuick(false);
    try {
      action.run();
      // removed before fail quick is enabled again: enabling it terminates if errors were logged
      assertTrue(Log.getFindings().removeIf(f -> f.getMsg().startsWith(message)));
    } finally {
      Log.enableFailQuick(failQuick);
    }
  }
}
