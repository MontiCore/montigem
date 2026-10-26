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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
  public void testDerivedKeyMapPutChecksKey() {
    Node a = new Node().build().get();
    a.name = "a";
    NamedValueMap map = new NamedValueMap();
    map.keyDerived = true;
    map.build();

    // the key is derived from the value, another key is an error
    assertNull(map.put("b", a));
    assertEquals(1, Log.getErrorCount());
    assertTrue(map.isEmpty());

    assertNull(map.put("a", a));
    assertSame(a, map.get("a"));
    assertEquals(List.of(a.getGemId()), map.putValueIDs);
    assertTrue(map.containsValueID(a.getGemId()));
  }

  @Test
  public void testClearRemovesEveryValueWithoutLoading() {
    Node a = new Node().build().get();
    a.name = "a";
    Node b = new Node().build().get();
    b.name = "b";
    NamedValueMap map = new NamedValueMap();
    map.build();
    map._internal_putValue(a);
    map._internal_addValueIDs(List.of(b.getGemId()));
    int loads = loadCalls;

    map.clear();
    assertTrue(map.isEmpty());
    assertEquals(List.of(a.getGemId(), b.getGemId()), map.removedValueIDs);
    // b was removed by id, its key was not derived
    assertEquals(loads, loadCalls);
    assertNoErrors();
  }

  protected Node named(String name) {
    Node n = new Node().build().get();
    n.name = name;
    return n;
  }

  @Test
  public void testViewsAreLive() {
    Node a = named("a");
    Node b = named("b");
    NamedValueMap map = new NamedValueMap();
    map.build();
    Set<String> keys = map.keySet();
    Collection<Node> values = map.values();
    Set<Map.Entry<String, Node>> entries = map.entrySet();

    map._internal_putValue(a);
    assertEquals(Set.of("a"), keys);
    map._internal_putValue(b);
    assertTrue(keys.contains("b"));
    assertEquals(2, values.size());
    assertTrue(values.contains(b));
    assertTrue(entries.contains(new SimpleEntry<>("b", b)));
    assertFalse(entries.contains(new SimpleEntry<>("b", a)));

    // rekeyed values appear under their new key
    map._internal_rekey("a", "c", a.getGemId());
    assertEquals(Set.of("b", "c"), keys);
    assertNoErrors();
  }

  @Test
  public void testRemoveThroughViewsIsSynchronized() {
    Node a = named("a");
    Node b = named("b");
    Node c = named("c");
    NamedValueMap map = new NamedValueMap();
    map.build();
    map._internal_putValue(a);
    map._internal_putValue(b);
    map._internal_putValue(c);

    assertTrue(map.keySet().remove("a"));
    assertFalse(map.keySet().remove("a"));
    assertTrue(map.values().remove(b));
    assertFalse(map.entrySet().remove(new SimpleEntry<>("c", a)));
    assertTrue(map.entrySet().remove(new SimpleEntry<>("c", c)));
    // every removal went through the synchronized hook
    assertEquals(List.of(a.getGemId(), b.getGemId(), c.getGemId()), map.removedValueIDs);
    assertTrue(map.isEmpty());

    // keys are derived from the values: adding through views is not supported
    map._internal_putValue(a);
    assertThrows(UnsupportedOperationException.class, () -> map.keySet().add("x"));
    assertThrows(UnsupportedOperationException.class, () -> map.values().add(b));
    assertThrows(UnsupportedOperationException.class, () -> map.entrySet().iterator().next().setValue(b));
    assertNoErrors();
  }

  @Test
  public void testIteratorsAreWeaklyConsistentAndBatched() {
    Node a = named("a");
    Node b = named("b");
    Node c = named("c");
    NamedValueMap map = new NamedValueMap();
    map.build();
    map._internal_addValueIDs(List.of(a.getGemId(), b.getGemId(), c.getGemId()));

    Iterator<Node> it = map.values().iterator();
    // keys derived in one batch when the iterator was created, values in one batch on first next
    assertSame(a, it.next());
    assertEquals(2, loadCalls);

    // b is removed during the iteration: skipped, no ConcurrentModificationException
    map._internal_removeValueID(b.getGemId());
    // added during the iteration: not visited
    map._internal_putValue(named("d"));
    assertSame(c, it.next());
    assertFalse(it.hasNext());
    assertEquals(2, loadCalls);

    // removing through the iterator goes through the synchronized hook
    it.remove();
    assertThrows(IllegalStateException.class, it::remove);
    assertEquals(List.of(c.getGemId()), map.removedValueIDs);
    assertEquals(Set.of("a", "d"), map.keySet());
    // insertion order
    assertEquals(List.of("a", "d"), new ArrayList<>(map.keySet()));
    assertNoErrors();
  }

  @Test
  public void testBuilderViewsContainUnbuiltBuilders() {
    Node a = new Node();
    a.name = "a";
    NamedValueMap map = new NamedValueMap();
    map._internal_putValue(a);

    assertEquals(Set.of("a"), map.keySet());
    assertSame(a, map.values().iterator().next());
    assertEquals(0, loadCalls);
    assertNoErrors();
  }

  @Test
  public void testEntryMapViewsLoadKeyObjects() {
    Node keyA = new Node().build().get();
    Node a = new Node().build().get();
    a.ref = keyA;
    NodeEntryMap map = new NodeEntryMap(null);
    map.build();
    map._internal_putValue(a);

    Map.Entry<Node, Node> entry = map.entrySet().iterator().next();
    assertSame(keyA, entry.getKey());
    assertSame(a, entry.getValue());
    assertTrue(map.keySet().contains(keyA));
    assertSame(keyA, map.keySet().iterator().next());
    assertNoErrors();
  }

  @Test
  public void testPutIDsStoresKeysWithoutLoading() {
    Node a = named("a");
    Node b = named("b");
    Map<String, Long> ids = new java.util.LinkedHashMap<>();
    ids.put("b", b.getGemId());
    ids.put("a", a.getGemId());

    // before build: kept by the builder
    NamedValueMap map = new NamedValueMap();
    map._internal_putIDs(ids);
    map.build();
    assertEquals(0, loadCalls);
    // keys known without loading, in insertion order
    assertEquals(2, map.size());
    assertTrue(map.containsKey("a"));
    assertEquals(List.of("b", "a"), new ArrayList<>(map.keySet()));
    assertEquals(0, loadCalls);
    assertSame(a, map.get("a"));
    assertEquals(1, loadCalls);

    // after build
    NamedValueMap built = new NamedValueMap();
    built.build();
    built._internal_putIDs(ids);
    assertEquals(ids, built.getIDMap());
    assertEquals(1, loadCalls);

    // rekeying reports whether the value was moved
    assertTrue(built._internal_rekey("a", "c", a.getGemId()));
    assertFalse(built._internal_rekey("a", "c", a.getGemId()));
    assertEquals(a.getGemId(), built.getID("c"));
    assertNoErrors();
  }

  @Test
  public void testEntriesAreLazy() {
    Node a = named("a");
    Node b = named("b");
    NamedValueMap map = new NamedValueMap();
    map.build();
    map._internal_putValue(a);
    map._internal_putValue(b);
    int loads = loadCalls;

    // keys only: no value is loaded
    Iterator<Map.Entry<String, Node>> it = map.entrySet().iterator();
    Map.Entry<String, Node> entryA = it.next();
    assertEquals("a", entryA.getKey());
    assertEquals(List.of("a", "b"), new ArrayList<>(map.keySet()));
    assertEquals(loads, loadCalls);
    // an entry of the map is found by gemId, without loading
    assertTrue(map.entrySet().contains(entryA));
    assertEquals(loads, loadCalls);

    // the first value loads the values of the remaining entries in one batch
    assertSame(a, entryA.getValue());
    assertSame(b, it.next().getValue());
    assertEquals(loads + 1, loadCalls);
    // entries follow Map.Entry
    assertEquals(new SimpleEntry<>("a", a), entryA);
    assertEquals(new SimpleEntry<>("a", a).hashCode(), entryA.hashCode());

    // a map loaded by ids is not keyed to tell whether it is empty
    NamedValueMap loaded = new NamedValueMap();
    loaded.build();
    loaded._internal_addValueIDs(List.of(a.getGemId()));
    loads = loadCalls;
    assertFalse(loaded.entrySet().isEmpty());
    assertFalse(loaded.keySet().isEmpty());
    assertEquals(loads, loadCalls);
    assertNoErrors();
  }

  @Test
  public void testRemoveThroughProjectionsDoesNotLoad() {
    Node keyA = new Node().build().get();
    Node keyB = new Node().build().get();
    Node keyC = new Node().build().get();
    Node a = new Node().build().get();
    a.ref = keyA;
    Node b = new Node().build().get();
    b.ref = keyB;
    Node c = new Node().build().get();
    c.ref = keyC;
    NodeEntryMap map = new NodeEntryMap(null);
    map.build();
    map._internal_putValue(a);
    map._internal_putValue(b);
    map._internal_putValue(c);
    int loads = loadCalls;

    assertTrue(map.keySet().remove(keyA));
    assertFalse(map.keySet().remove(keyA));
    assertTrue(map.values().remove(b));
    assertFalse(map.values().remove(b));
    Iterator<Map.Entry<Node, Node>> it = map.entrySet().iterator();
    Map.Entry<Node, Node> entryC = it.next();
    assertTrue(map.entrySet().remove(entryC));
    assertFalse(map.entrySet().remove(entryC));
    assertTrue(map.isEmpty());
    // neither key objects nor values were loaded
    assertEquals(loads, loadCalls);
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

    boolean keyDerived = false;

    @Override
    public boolean isKeyDerived() {
      return keyDerived;
    }

    // records the synchronized modifications, like a generated GemMap delegating to its owner
    List<Long> putValueIDs = new java.util.ArrayList<>();

    List<Long> removedValueIDs = new java.util.ArrayList<>();

    @Override
    protected void _internal_putValueAndSync(Node value) {
      putValueIDs.add(value.getGemId());
      super._internal_putValueAndSync(value);
    }

    @Override
    protected void _internal_removeValueIDAndSync(long gemID) {
      removedValueIDs.add(gemID);
      super._internal_removeValueIDAndSync(gemID);
    }
  }
}
