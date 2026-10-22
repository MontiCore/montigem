// (c) https://github.com/MontiCore/monticore
package umlp.backendrte;

import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import umlp.backendrte.common.GemBuildable;
import umlp.backendrte.common.GemEntryMapBuilder;
import umlp.backendrte.common.GemIdentifiable;
import umlp.backendrte.common.GemValueMapBuilder;
import umlp.backendrte.common.LoadResult;
import umlp.backendrte.common.LoadingState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Builder phase of GemEntryMapBuilder and GemValueMapBuilder:
 * unbuilt builders have their gemId from construction, but are only known to
 * the "manager" (here: registry) after build()
 */
public class GemMapBuilderTest extends AbstractUMLPRTETest {

  // acts as manager: knows only built nodes
  Map<Long, Node> registry;

  long nextID;

  int loadCalls;

  @BeforeEach
  public void init() {
    registry = new HashMap<>();
    nextID = 0;
    loadCalls = 0;
  }

  @Test
  public void testEntryMapStoresUnbuiltBuilders() {
    NodeEntryMap map = new NodeEntryMap(null);
    Node key = new Node();
    Node value = new Node();

    assertNull(map.put(key, value));
    assertEquals(1, map.size());
    assertSame(value, map.get(key));
    assertTrue(map.containsKey(key));
    assertTrue(map.containsValue(value));

    map.build();

    assertTrue(key.isBuildFinished());
    assertTrue(value.isBuildFinished());
    assertSame(key, registry.get(key.getGemId()));
    assertSame(value, registry.get(value.getGemId()));
    assertEquals(Map.of(key.getGemId(), value.getGemId()), map.getIDMap());
    assertSame(value, map.get(key));
    assertNoErrors();
  }

  @Test
  public void testEntryMapKeyByIdAndByBuilderMatch() {
    Node key = new Node().build().get();
    Node oldValue = new Node().build().get();
    NodeEntryMap map = new NodeEntryMap(Map.of(key.getGemId(), oldValue.getGemId()));
    Node newValue = new Node();

    // key was added by id, is now put as object: same entry
    assertSame(oldValue, map.put(key, newValue));
    assertEquals(1, map.size());
    assertSame(newValue, map.get(key));
    assertNoErrors();
  }

  @Test
  public void testEntryMapMissingKeyAndRemove() {
    NodeEntryMap map = new NodeEntryMap(null);
    Node key = new Node();
    Node value = new Node();

    assertNull(map.get(key));
    assertNull(map.remove(key));

    map.put(key, value);
    assertSame(value, map.remove(key));
    assertTrue(map.isEmpty());
    assertFalse(map.containsKey(key));

    map.build();
    assertFalse(key.isBuildFinished());
    assertTrue(map.getIDMap().isEmpty());
    assertNoErrors();
  }

  @Test
  public void testValueMapStoresUnbuiltBuilders() {
    NodeValueMap map = new NodeValueMap(null);
    Node value = new Node();

    assertNull(map.put("a", value));
    assertSame(value, map.get("a"));
    assertNull(map.get("b"));
    assertNull(map.remove("b"));
    assertTrue(map.containsKey("a"));

    map.build();

    assertTrue(value.isBuildFinished());
    assertSame(value, registry.get(value.getGemId()));
    assertEquals(Map.of("a", value.getGemId()), map.getIDMap());
    assertNoErrors();
  }

  @Test
  public void testValueMapDerivedKeysAreLazyAndRekeyed() {
    Node a = new Node().build().get();
    a.name = "a";
    Node b = new Node().build().get();
    b.name = "b";
    NamedValueMap map = new NamedValueMap();
    map.build();
    map._internal_addValueIDs(List.of(a.getGemId(), b.getGemId()));

    // ids are known without loading the values
    assertEquals(2, map.getValueIDs().size());
    assertEquals(0, loadCalls);

    // keys are derived on first access, in one batch (plus loading b itself)
    assertSame(b, map.get("b"));
    assertEquals(2, loadCalls);
    assertEquals(Map.of("a", a.getGemId(), "b", b.getGemId()), map.getIDMap());

    map._internal_rekey("a", "c", a.getGemId());
    assertNull(map.get("a"));
    assertSame(a, map.get("c"));
    assertNoErrors();

    // rekeying onto an occupied key is an error and changes nothing
    map._internal_rekey("c", "b", a.getGemId());
    assertEquals(1, Log.getErrorCount());
    assertSame(a, map.get("c"));
  }

  @Test
  public void testValueMapBuilderPutValueKeepsBuilder() {
    Node a = new Node();
    a.name = "a";
    NamedValueMap map = new NamedValueMap();
    map._internal_putValue(a);
    assertSame(a, map.get("a"));

    map.build();
    assertTrue(a.isBuildFinished());
    assertEquals(Map.of("a", a.getGemId()), map.getIDMap());
    assertNoErrors();
  }

  @Test
  public void testValueMapNotifiesRemovedValues() {
    Node a = new Node();
    a.name = "a";
    Node b = new Node();
    b.name = "a";
    Node c = new Node();
    c.name = "c";
    NamedValueMap map = new NamedValueMap();
    // before build: the removed builders are at hand
    map._internal_putValue(a);
    map._internal_putValue(b);
    assertEquals(List.of(a.getGemId()), map.removedIDs);
    assertSame(a, map.removedValues.get(0));
    map._internal_putValue(c);
    map._internal_removeValueID(c.getGemId());
    assertEquals(List.of(a.getGemId(), c.getGemId()), map.removedIDs);

    // after build: only the ids are known
    map.build();
    // a replaces b, which holds key "a"
    a.build();
    map._internal_putValue(a);
    map.remove("a");
    assertEquals(List.of(a.getGemId(), c.getGemId(), b.getGemId(), a.getGemId()), map.removedIDs);
    assertNull(map.removedValues.get(2));
    assertNull(map.removedValues.get(3));
    assertNoErrors();
  }

  @Test
  public void testEntryMapDerivedKeysAreLazyAndRekeyed() {
    Node keyA = new Node().build().get();
    Node keyB = new Node().build().get();
    Node a = new Node().build().get();
    a.ref = keyA;
    NodeEntryMap map = new NodeEntryMap(null);
    map._internal_addValueIDs(List.of(a.getGemId()));

    assertSame(a, map.get(keyA));
    map._internal_rekey(keyA.getGemId(), keyB.getGemId(), a.getGemId());
    assertNull(map.get(keyA));
    assertSame(a, map.get(keyB));

    map.build();
    assertEquals(Map.of(keyB.getGemId(), a.getGemId()), map.getIDMap());
    assertNoErrors();
  }

  protected List<LoadResult<Node>> load(List<Long> gemIDs) {
    loadCalls++;
    return gemIDs.stream()
        .map(id -> registry.containsKey(id) ? new LoadResult<>(registry.get(id))
            : new LoadResult<Node>(LoadingState.FAILED_TO_LOAD))
        .collect(Collectors.toList());
  }

  /**
   * like a generated domain class: it is its own builder and has its gemId from construction
   */
  class Node implements GemIdentifiable, GemBuildable {

    protected final long gemId = nextID++;

    protected boolean buildFinished = false;

    // qualifying attribute (value map) and qualifying reference (entry map)
    protected String name;

    protected Node ref;

    @Override
    public long getGemId() {
      return gemId;
    }

    @Override
    public Optional<Node> build() {
      if (!buildFinished) {
        buildFinished = true;
        registry.put(gemId, this);
      }
      return Optional.of(this);
    }

    @Override
    public boolean isBuildFinished() {
      return buildFinished;
    }
  }

  class NodeEntryMap extends GemEntryMapBuilder<Node, Node> {

    NodeEntryMap(Map<Long, Long> initialIds) {
      super(initialIds);
    }

    @Override
    protected List<LoadResult<Node>> loadKeyObjects(List<Long> gemIDs) {
      return load(gemIDs);
    }

    @Override
    protected List<LoadResult<Node>> loadValueObjects(List<Long> gemIDs) {
      return load(gemIDs);
    }

    @Override
    public long deriveKeyID(Node value) {
      return value.ref.getGemId();
    }
  }

  class NodeValueMap extends GemValueMapBuilder<String, Node> {

    NodeValueMap(Map<String, Long> initialIds) {
      super(initialIds);
    }

    @Override
    protected List<LoadResult<Node>> loadValueObjects(List<Long> gemIDs) {
      return load(gemIDs);
    }
  }

  /**
   * like the map of an attribute qualified association [[name]]
   */
  class NamedValueMap extends GemValueMapBuilder<String, Node> {

    NamedValueMap() {
      super(null);
    }

    @Override
    protected List<LoadResult<Node>> loadValueObjects(List<Long> gemIDs) {
      return load(gemIDs);
    }

    @Override
    public String deriveKey(Node value) {
      return value.name;
    }

    List<Long> removedIDs = new java.util.ArrayList<>();

    List<Node> removedValues = new java.util.ArrayList<>();

    @Override
    protected void onValueRemoved(long valueGemID, Node value) {
      removedIDs.add(valueGemID);
      removedValues.add(value);
    }
  }
}
