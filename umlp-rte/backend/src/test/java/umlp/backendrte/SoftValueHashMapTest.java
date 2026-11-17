// (c) https://github.com/MontiCore/monticore
package umlp.backendrte;

import org.junit.jupiter.api.Test;
import umlp.backendrte.common.SoftValueHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SoftValueHashMap follows the Map contract: put and remove return the previous value, or null if there was none
 * (the managers' caches remove objects they may not have loaded)
 */
public class SoftValueHashMapTest {

  @Test
  public void testRemoveAbsentKeyReturnsNull() {
    SoftValueHashMap<Long, String> map = new SoftValueHashMap<>();
    assertNull(map.remove(1L));

    map.put(1L, "a");
    assertEquals("a", map.remove(1L));
    assertNull(map.remove(1L));
    assertTrue(map.isEmpty());
  }

  @Test
  public void testPutReturnsThePreviousValue() {
    SoftValueHashMap<Long, String> map = new SoftValueHashMap<>();
    assertNull(map.put(1L, "a"));
    assertEquals("a", map.put(1L, "b"));
    assertEquals("b", map.get(1L));
  }

  @Test
  public void testKeyMappedToNull() {
    SoftValueHashMap<Long, String> map = new SoftValueHashMap<>();
    assertNull(map.put(1L, null));
    assertTrue(map.containsKey(1L));
    assertNull(map.get(1L));
    assertNull(map.put(1L, "a"));
    assertEquals("a", map.get(1L));
    assertEquals("a", map.put(1L, null));
    assertNull(map.remove(1L));
    assertFalse(map.containsKey(1L));
  }
}
