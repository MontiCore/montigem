package aas;

import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import aas.decorator.NameTransformationDecorator;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static aas.AAS2UMLPGenerator.NAMETRANSFORMATIONS;

/**
 * Unit tests for NameTransformationDecorator.
 */
class NameTransformationDecoratorTest extends AbstractTest {

  private GlobalExtensionManagement glex;
  private NameTransformationDecorator decorator;

  @Override
  public void _setup() {
    glex = new GlobalExtensionManagement();
    decorator = new NameTransformationDecorator(glex);
  }

  @Test
  void decorate_withNullGlex() {
    Log.enableFailQuick(false);
    decorator = new NameTransformationDecorator(null);
    assertEquals("", decorator.decorate());
    assertEquals(1, Log.getErrorCount());
    assertEquals("glex is null.", Log.getFindings().getFirst().getMsg());
    Log.clearFindings();
  }

  @Test
  void decorate_withMissingGlobalVar() {
    Log.enableFailQuick(false);
    String result = decorator.decorate();
    assertEquals("", result);
    assertEquals(1, Log.getErrorCount());
    assertEquals("glex has no name transformation global variable.", Log.getFindings().getFirst().getMsg());
    Log.clearFindings();
  }

  @Test
  void decorate_withEmptyMap_shouldReturnEmptyString() {
    glex.defineGlobalVar(NAMETRANSFORMATIONS, new LinkedHashMap<>());
    String result = decorator.decorate();
    assertEquals("", result);
  }

  @Test
  void decorate_withSingleEntry_shouldReturnFormattedOutput() {
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
    map.put("key1", "value1");
    glex.defineGlobalVar(NAMETRANSFORMATIONS, map);
    String result = decorator.decorate();
    assertEquals("key1: value1", result);
  }

  @Test
  void decorate_withMultipleEntries() {
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
    map.put("foo", "bar");
    map.put("hello", "world");
    glex.defineGlobalVar(NAMETRANSFORMATIONS, map);
    String result = decorator.decorate();
    assertTrue(result.contains("foo: bar"));
    assertTrue(result.contains("hello: world"));
  }

  @Test
  void decorate_withMapAlteredAfterDecoratorCreation() {
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
    glex.defineGlobalVar(NAMETRANSFORMATIONS, map);
    String result1 = decorator.decorate();

    map.put("newKey", "newValue");
    String result2 = decorator.decorate();

    assertTrue(result2.contains("newKey: newValue"));
  }

  @Test
  void decorate_shouldBeDeterministicAndOrderPreserving() {
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
    map.put("b", "2");
    map.put("a", "1");
    glex.defineGlobalVar(NAMETRANSFORMATIONS, map);
    String result = decorator.decorate();
    assertEquals("b: 2\na: 1", result);
  }
}
