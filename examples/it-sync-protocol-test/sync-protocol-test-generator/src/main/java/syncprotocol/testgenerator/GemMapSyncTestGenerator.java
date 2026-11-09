package syncprotocol.testgenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Generates the sync protocol tests of qualified associations (GemMaps), attribute qualified ones ([[attr]], the key
 * is derived from the value) and type qualified ones ([T], the key is chosen on put), like
 * {@link SyncProtocolTestGenerator} does for list associations: one test interface per map of the model
 * (examples/it-sync-protocol-test/models/Associations.umlp, classes named <Key><Owner><Value>), with a test method per
 * build state, context pair, operation (the ones that apply to the map, see {@link Operation#appliesTo}) and load
 * state, and the support that is specific to the map. Bidirectional maps ({@link Owner}) check the other side of the
 * values as well.
 */
public final class GemMapSyncTestGenerator {
  private GemMapSyncTestGenerator() {
  }

  enum Key {
    STR("Str", "String", false, false),
    NUM("Num", "int", false, false),
    OBJ("Obj", "MapKey", true, false),
    ROLE("Role", "MapKey", true, false),
    // type qualified associations ([T]): the key is chosen on put
    TYPE_STR("TypeStr", "String", false, true),
    TYPE_NUM("TypeNum", "int", false, true),
    TYPE_OBJ("TypeObj", "MapKey", true, true),
    // long keys close to the largest integer a browser represents exactly; Plain values only
    LONG("Long", "long", false, false),
    TYPE_LONG("TypeLong", "long", false, true),
    // the declared key class MapKeyBase is abstract, the keys are instances of its subclass MapSubKey; Plain only
    TYPE_SUB_KEY("TypeSubKey", "MapKeyBase", true, true),
    // boxed long keys ([Long]) close to the largest integer a browser represents exactly; Plain only
    TYPE_BOXED("TypeBoxed", "Long", false, true);

    final String namePart;
    final String type;
    // the key is a domain object (GemEntryMap), stored by its gemId
    final boolean entry;
    // the key is chosen on put (type qualified association), not derived from the value
    final boolean chosen;

    Key(String namePart, String type, boolean entry, boolean chosen) {
      this.namePart = namePart;
      this.type = type;
      this.entry = entry;
      this.chosen = chosen;
    }

    // the value shapes of the model: all for most keys, Plain only for the keys added for their key type
    boolean hasValue(Value value) {
      return value == Value.PLAIN || (this != LONG && this != TYPE_LONG && this != TYPE_SUB_KEY && this != TYPE_BOXED);
    }

    // the class of the key objects (GemEntryMap): MapKey, or the subclass MapSubKey of the declared key class
    String keyClass() {
      return this == TYPE_SUB_KEY ? "MapSubKey" : "MapKey";
    }
  }

  /**
   * the owner side of the association: unidirectional, or bidirectional with the owner side's cardinality; the value
   * side of a bidirectional association is the map's other side (role owner or owners)
   */
  enum Owner {
    UNI(""),
    // [1]: a value is in one owner's map, a put into another owner's map moves it
    ONE("One"),
    // [0..1]: the same as [1]
    OPT("Opt"),
    // [*]: a value may be in several owners' maps
    MANY("Many");

    final String namePart;

    Owner(String namePart) {
      this.namePart = namePart;
    }

    boolean bidirectional() {
      return this != UNI;
    }

    boolean single() {
      return this == ONE || this == OPT;
    }
  }

  enum Value {
    PLAIN("Plain"),
    SUPER("Super"),
    SUB("Sub"),
    ABSTRACT("Abstract");

    final String namePart;

    Value(String namePart) {
      this.namePart = namePart;
    }
  }

  enum BuildState {
    DONE_TO_DONE("DoneToDone", false),
    BUILDER_TO_DONE("BuilderToDone", true),
    BUILDER_TO_BUILDER("BuilderToBuilder", true);

    final String namePart;
    final boolean hasBuilder;

    BuildState(String namePart, boolean hasBuilder) {
      this.namePart = namePart;
      this.hasBuilder = hasBuilder;
    }
  }

  enum ContextPair {
    SERVER_TO_SERVER("ServerToServer"),
    CLIENT_TO_SERVER("ClientToServer"),
    SERVER_TO_CLIENT("ServerToClient"),
    CLIENT_TO_CLIENT("ClientToClient");

    final String namePart;

    ContextPair(String namePart) {
      this.namePart = namePart;
    }
  }

  enum LoadState {
    FULL_OBJECT_LOADED("FullObjectLoaded"),
    // the owner is loaded, so its keys are known, the values are not
    OWNER_LOADED("OwnerLoaded"),
    // bidirectional: the values are loaded, the owner is not; the values' other side follows nevertheless
    VALUE_LOADED("ValueLoaded"),
    FULL_OBJECT_NOT_LOADED("FullObjectNotLoaded");

    final String namePart;

    LoadState(String namePart) {
      this.namePart = namePart;
    }
  }

  enum Operation {
    BUILD_OBJECT("BuildObject"),
    PUT("Put"),
    PUT_BY_MAP("PutByMap"),
    GET("Get"),
    // derived keys: the qualifying attribute of a value changes
    REKEY("Rekey"),
    // chosen keys: a value is put under another key, it is stored under one key only, so its entry moves
    MOVE("Move"),
    REMOVE("Remove"),
    REMOVE_BY_KEY_SET("RemoveByKeySet"),
    REPLACE("Replace"),
    DELETE_OBJECT("DeleteObject"),
    // deleting the owner removes its map, the values and the maps of other owners stay
    DELETE_OWNER("DeleteOwner"),
    // chosen keys that are objects: deleting the key object removes its entry, the value stays
    DELETE_KEY("DeleteKey"),
    // bidirectional: value 0 is put into the map of another owner; a single owner loses it
    MOVE_OWNER("MoveOwner"),
    // bidirectional, derived keys: the value side of value 0 is changed to (single) or extended by (several) another
    // owner, which puts it into that owner's map
    SET_OWNER("SetOwner");

    final String namePart;

    Operation(String namePart) {
      this.namePart = namePart;
    }

    boolean appliesTo(GemMap gemMap) {
      Key key = gemMap.key();
      return switch (this) {
        case REKEY -> !key.chosen;
        case MOVE -> key.chosen;
        // the entries of a derived key object are not removed on its deletion yet (known bug)
        case DELETE_KEY -> key.chosen && key.entry;
        case MOVE_OWNER -> gemMap.owner().bidirectional();
        // the value side of a type qualified association is read-only (there is no key to put it under)
        case SET_OWNER -> gemMap.owner().bidirectional() && !key.chosen;
        default -> true;
      };
    }
  }

  record GemMap(Key key, Owner owner, Value value) {
    String caseName() {
      return key.namePart + owner.namePart + value.namePart;
    }

    // the role of the map's other side on the value
    String otherSide() {
      return owner.single() ? "owner" : "owners";
    }

    String ownerType() {
      return caseName() + "Owner";
    }

    // the type of the objects put into the map
    String valueType() {
      return caseName() + "Value";
    }

    // the target type of the association: the value type, or its superclass for Sub and Abstract
    String mapType() {
      return value == Value.SUB || value == Value.ABSTRACT ? caseName() + "Base" : valueType();
    }

    String className() {
      return caseName() + "MapSyncProtocolTest";
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Usage: GemMapSyncTestGenerator <testOutputDir>");
    }

    Path outputDir = Path.of(args[0]);
    recreateDirectory(outputDir);
    generateGemMapSyncTests(outputDir, defaultGemMaps());
  }

  private static List<GemMap> defaultGemMaps() {
    List<GemMap> gemMaps = new ArrayList<>();
    for (Key key : Key.values()) {
      for (Value value : Value.values()) {
        if (!key.hasValue(value)) {
          continue;
        }
        gemMaps.add(new GemMap(key, Owner.UNI, value));
      }
    }
    // bidirectional, Plain only: [1] and [*] for the main keys, [0..1] (the same code as [1]) for one [[attr]] and one
    // [T] key
    for (Key key : List.of(Key.STR, Key.NUM, Key.OBJ, Key.ROLE, Key.TYPE_STR, Key.TYPE_NUM, Key.TYPE_OBJ)) {
      for (Owner owner : List.of(Owner.ONE, Owner.OPT, Owner.MANY)) {
        if (owner == Owner.OPT && key != Key.STR && key != Key.TYPE_OBJ) {
          continue;
        }
        gemMaps.add(new GemMap(key, owner, Value.PLAIN));
      }
    }
    return gemMaps;
  }

  private static void generateGemMapSyncTests(Path testOutputDir, List<GemMap> gemMaps) throws IOException {
    Path packageDir = testOutputDir.resolve("syncprotocol/generated/gemmap");
    Files.createDirectories(packageDir);

    for (GemMap gemMap : gemMaps) {
      Files.writeString(
          packageDir.resolve(gemMap.className() + ".java"),
          renderTestClass(gemMap),
          StandardCharsets.UTF_8);
    }
  }

  private static String renderTestClass(GemMap gemMap) {
    StringBuilder content = new StringBuilder();
    content.append("/* Generated by syncprotocol.testgenerator.GemMapSyncTestGenerator */\n\n");
    content.append("package syncprotocol.generated.gemmap;\n\n");
    content.append("import associations.*;\n");
    content.append("import associations.test_infrastructure.*;\n");
    content.append("import org.junit.jupiter.api.Test;\n\n");
    content.append("import org.junit.jupiter.api.Tag;\n\n");
    content.append("import util.GemMapOperation;\n\n");
    content.append("import util.GemMapSyncProtocolTestScenario;\n");
    content.append("import util.GemMapSyncProtocolTestSupport;\n\n");
    content.append("import static org.junit.jupiter.api.Assertions.assertEquals;\n");
    content.append("import static org.junit.jupiter.api.Assertions.assertFalse;\n");
    content.append("import static org.junit.jupiter.api.Assertions.assertNull;\n");
    content.append("import static org.junit.jupiter.api.Assertions.assertTrue;\n\n");

    content.append("""
    public interface <cn> extends GemMapSyncProtocolTestSupport {
      public class <cn>Mock extends AssociationsMockTestSetup implements <cn> {
        public <cn>Mock() {
          super(2);
        }
      }

      public class <cn>SQL extends AssociationsSQLDBTestSetup implements <cn> {
        public <cn>SQL() {
          super(2);
        }
      }

      @Tag("SlowTest")
      public class <cn>Neo4J extends AssociationsNeo4JDBTestSetup implements <cn> {
        public <cn>Neo4J() {
          super(2);
        }
      }
    """.replace("<cn>", gemMap.className()));
    appendTestMethods(content, gemMap);
    appendScenarioSupport(content, gemMap);
    content.append("}\n");
    return content.toString();
  }

  private static void appendTestMethods(StringBuilder content, GemMap gemMap) {
    for (BuildState buildState : BuildState.values()) {
      for (ContextPair contextPair : ContextPair.values()) {
        for (Operation operation : Operation.values()) {
          if (!operation.appliesTo(gemMap)) {
            continue;
          }
          for (LoadState loadState : LoadState.values()) {
            if (loadState == LoadState.VALUE_LOADED && !gemMap.owner().bidirectional()) {
              continue;
            }
            if (isSensible(buildState, contextPair, loadState)) {
              appendTestMethod(content, buildState, contextPair, operation, loadState, gemMap);
            }
            else {
              appendSkippedCaseComment(content, buildState, contextPair, operation, loadState);
            }
          }
        }
      }
    }
  }

  private static void appendTestMethod(
      StringBuilder content,
      BuildState buildState,
      ContextPair contextPair,
      Operation operation,
      LoadState loadState,
      GemMap gemMap) {
    content.append("  @Test\n");
    content.append("  default void test")
        .append(buildState.namePart)
        .append(contextPair.namePart)
        .append(operation.namePart)
        .append(loadState.namePart)
        .append("() {\n");
    appendActivation(content, sourceContext(contextPair));
    content.append("    GemMapSyncProtocolTestScenario scenario = ")
        .append(createInitialMethod(buildState))
        .append("(")
        .append(initialValueCount(operation))
        .append(");\n");
    appendPreloadExpectedContext(content, contextPair, loadState);
    appendActivation(content, sourceContext(contextPair));
    content.append("    applyOperation(GemMapOperation.").append(operation.name()).append(", scenario);\n\n");
    appendActivation(content, expectedContext(contextPair));
    if (loadState == LoadState.OWNER_LOADED) {
      content.append("    // the keys travel without the values\n");
      content.append("    assertValuesNotLoaded(scenario);\n");
    }
    // VALUE_LOADED: the values' other side follows without the owner (whether the owner stays unloaded is not
    // checked: changing a single other side by id loads its old value, known bug #18)
    // before the map's assertions, which load the owner
    appendOtherSideAssertion(content, operation, gemMap, "");
    appendExpectedAssertion(content, operation, gemMap, "");
    if (operation == Operation.REPLACE && !gemMap.key().chosen) {
      content.append("    // the replaced value derives the same key: the server reports it, the value is replaced\n");
      content.append("    assertLogged(\"").append(gemMap.key().entry ? "0xA7317" : "0xA7307").append("\");\n");
    }
    appendDatabaseAssertion(content, operation, gemMap);
    content.append("  }\n\n");
  }

  private static String sourceContext(ContextPair contextPair) {
    return switch (contextPair) {
      case SERVER_TO_SERVER, SERVER_TO_CLIENT -> "getBackend()";
      case CLIENT_TO_SERVER, CLIENT_TO_CLIENT -> "getClients(0)";
    };
  }

  private static String expectedContext(ContextPair contextPair) {
    return switch (contextPair) {
      case SERVER_TO_SERVER, CLIENT_TO_SERVER -> "getBackend()";
      case SERVER_TO_CLIENT, CLIENT_TO_CLIENT -> "getClients(1)";
    };
  }

  private static boolean expectedIsClient(ContextPair contextPair) {
    return contextPair == ContextPair.SERVER_TO_CLIENT || contextPair == ContextPair.CLIENT_TO_CLIENT;
  }

  private static void appendActivation(StringBuilder content, String contextExpression) {
    content.append("    ").append(contextExpression).append(".activate();\n");
  }

  private static String createInitialMethod(BuildState buildState) {
    return switch (buildState) {
      case DONE_TO_DONE -> "createDoneToDoneInitial";
      case BUILDER_TO_DONE -> "createBuilderToDoneInitial";
      case BUILDER_TO_BUILDER -> "createBuilderToBuilderInitial";
    };
  }

  // the initial values are stored under the keys 0, 1 and 2; key 3 is free (rekeying)
  private static int initialValueCount(Operation operation) {
    return switch (operation) {
      case BUILD_OBJECT, PUT, PUT_BY_MAP, REPLACE -> 1;
      case GET, REKEY, MOVE, REMOVE, REMOVE_BY_KEY_SET, DELETE_OBJECT, DELETE_OWNER, DELETE_KEY, MOVE_OWNER,
           SET_OWNER -> 2;
    };
  }

  private static void appendPreloadExpectedContext(
      StringBuilder content,
      ContextPair contextPair,
      LoadState loadState) {
    if (loadState == LoadState.FULL_OBJECT_NOT_LOADED) {
      content.append("    // Expected context intentionally remains unloaded.\n\n");
      return;
    }

    appendActivation(content, expectedContext(contextPair));
    if (loadState != LoadState.VALUE_LOADED) {
      content.append("    preloadOwner(scenario.ownerId);\n");
    }
    if (loadState == LoadState.FULL_OBJECT_LOADED || loadState == LoadState.VALUE_LOADED) {
      content.append("    preloadValue(scenario.value0Id);\n");
      content.append("    preloadValue(scenario.value1Id);\n");
      content.append("    preloadValue(scenario.value2Id);\n");
    }
    content.append("\n");
  }

  private static void appendExpectedAssertion(StringBuilder content, Operation operation, GemMap gemMap,
                                              String persisted) {
    String present = "    assert" + persisted + "EntryPresent(scenario, ";
    String absent = "    assert" + persisted + "EntryAbsent(scenario, ";
    String size = "    assert" + persisted + "MapSize(scenario, ";
    switch (operation) {
      case BUILD_OBJECT -> {
        content.append(present).append("0, scenario.value0Id);\n");
        content.append(size).append("1);\n");
      }
      case PUT, PUT_BY_MAP -> {
        content.append(present).append("1, scenario.targetValueId);\n");
        content.append(present).append("0, scenario.value0Id);\n");
        content.append(size).append("2);\n");
      }
      case GET -> {
        content.append(present).append("1, scenario.value1Id);\n");
        content.append(present).append("0, scenario.value0Id);\n");
      }
      case REKEY, MOVE -> {
        content.append(present).append("3, scenario.value0Id);\n");
        content.append(absent).append("0);\n");
        content.append(present).append("1, scenario.value1Id);\n");
        content.append(size).append("2);\n");
      }
      case REMOVE, REMOVE_BY_KEY_SET -> {
        content.append(absent).append("0);\n");
        content.append(present).append("1, scenario.value1Id);\n");
        content.append(size).append("1);\n");
      }
      case REPLACE -> {
        content.append(present).append("0, scenario.targetValueId);\n");
        content.append(size).append("1);\n");
      }
      case DELETE_OBJECT -> {
        content.append(absent).append("0);\n");
        content.append(present).append("1, scenario.value1Id);\n");
        content.append(size).append("1);\n");
        if (persisted.isEmpty()) {
          content.append("    assertFalse(isValueKnown(scenario.targetValueId));\n");
        }
        else {
          content.append("    assertPersistedValueDeleted(scenario.targetValueId);\n");
        }
      }
      case DELETE_OWNER -> {
        if (persisted.isEmpty()) {
          content.append("    assertFalse(isOwnerKnown(scenario.ownerId));\n");
          content.append("    assertTrue(isValueKnown(scenario.value0Id));\n");
          content.append("    assertTrue(isValueKnown(scenario.value1Id));\n");
        }
        else {
          content.append("    assertPersistedOwnerDeleted(scenario.ownerId);\n");
          content.append("    assertPersistedValuePresent(scenario.value0Id);\n");
          content.append("    assertPersistedValuePresent(scenario.value1Id);\n");
        }
        content.append("    assert").append(persisted).append("OtherOwnerEntryPresent(scenario, 0, scenario.value0Id);\n");
      }
      case MOVE_OWNER, SET_OWNER -> {
        if (gemMap.owner().single()) {
          content.append(absent).append("0);\n");
          content.append(size).append("1);\n");
        }
        else {
          content.append(present).append("0, scenario.value0Id);\n");
          content.append(size).append("2);\n");
        }
        content.append(present).append("1, scenario.value1Id);\n");
        content.append("    assert").append(persisted).append("OtherOwnerEntryPresent(scenario, 0, scenario.value0Id);\n");
      }
      case DELETE_KEY -> {
        // key 0 is deleted: its entry is checked by the gemId of the key object
        content.append("    assert").append(persisted).append("StoredKeyAbsent(scenario, 0);\n");
        content.append(present).append("1, scenario.value1Id);\n");
        content.append(size).append("1);\n");
        if (persisted.isEmpty()) {
          content.append("    assertTrue(isValueKnown(scenario.targetValueId));\n");
        }
        else {
          content.append("    assertPersistedValuePresent(scenario.targetValueId);\n");
        }
      }
    }
  }

  /**
   * bidirectional: the other side of the values names the owners whose maps hold them (o: the owner, oo: the other
   * owner of DELETE_OWNER, MOVE_OWNER and SET_OWNER)
   */
  private static void appendOtherSideAssertion(StringBuilder content, Operation operation, GemMap gemMap,
                                               String persisted) {
    if (!gemMap.owner().bidirectional()) {
      return;
    }
    String o = "scenario.ownerId";
    String oo = "scenario.otherOwnerId";
    String assertion = "    assert" + persisted + "OtherSide(";
    switch (operation) {
      case BUILD_OBJECT -> content.append(assertion).append("scenario.value0Id, ").append(o).append(");\n");
      case PUT, PUT_BY_MAP -> {
        content.append(assertion).append("scenario.targetValueId, ").append(o).append(");\n");
        content.append(assertion).append("scenario.value0Id, ").append(o).append(");\n");
      }
      case GET, REKEY, MOVE -> {
        content.append(assertion).append("scenario.value0Id, ").append(o).append(");\n");
        content.append(assertion).append("scenario.value1Id, ").append(o).append(");\n");
      }
      case REMOVE, REMOVE_BY_KEY_SET, DELETE_KEY -> {
        content.append(assertion).append("scenario.value0Id);\n");
        content.append(assertion).append("scenario.value1Id, ").append(o).append(");\n");
      }
      case REPLACE -> {
        content.append(assertion).append("scenario.targetValueId, ").append(o).append(");\n");
        content.append(assertion).append("scenario.oldValueId);\n");
      }
      case DELETE_OBJECT -> content.append(assertion).append("scenario.value1Id, ").append(o).append(");\n");
      case DELETE_OWNER -> {
        // value 0 was put into the other owner's map as well (a single owner moved it there)
        content.append(assertion).append("scenario.value0Id, ").append(oo).append(");\n");
        content.append(assertion).append("scenario.value1Id);\n");
      }
      case MOVE_OWNER, SET_OWNER -> {
        if (gemMap.owner().single()) {
          content.append(assertion).append("scenario.value0Id, ").append(oo).append(");\n");
        }
        else {
          content.append(assertion).append("scenario.value0Id, ").append(o).append(", ").append(oo).append(");\n");
        }
        content.append(assertion).append("scenario.value1Id, ").append(o).append(");\n");
      }
    }
  }

  private static void appendDatabaseAssertion(StringBuilder content, Operation operation, GemMap gemMap) {
    content.append("\n");
    content.append("    if (hasDatabase()) {\n");
    content.append("      getBackend().activate();\n");
    StringBuilder persisted = new StringBuilder();
    appendOtherSideAssertion(persisted, operation, gemMap, "Persisted");
    appendExpectedAssertion(persisted, operation, gemMap, "Persisted");
    content.append(persisted.toString().replace("\n    ", "\n      ").replaceFirst("^    ", "      "));
    content.append("    }\n");
  }

  private static void appendSkippedCaseComment(
      StringBuilder content,
      BuildState buildState,
      ContextPair contextPair,
      Operation operation,
      LoadState loadState) {
    content.append("  // Not generated: ")
        .append(buildState.namePart)
        .append(", ")
        .append(contextPair.namePart)
        .append(", ")
        .append(operation.namePart)
        .append(", ")
        .append(loadState.namePart)
        .append(" is not sensible: the server has loaded all objects.\n\n");
  }

  /**
   * builders are built on the source before the operation, so every build state is sensible for every context pair:
   * the other participants receive the built objects
   */
  private static boolean isSensible(BuildState buildState, ContextPair contextPair, LoadState loadState) {
    // the server has loaded everything it created or received
    if (loadState != LoadState.FULL_OBJECT_LOADED && !expectedIsClient(contextPair)) {
      return false;
    }
    return true;
  }

  private static void appendScenarioSupport(StringBuilder content, GemMap gemMap) {
    appendKey(content, gemMap);
    appendCreateInitial(content, gemMap);
    appendOperation(content, gemMap);
    appendPreloadAndAssertions(content, gemMap);
    appendObjectFactories(content, gemMap);
  }

  private static void appendKey(StringBuilder content, GemMap gemMap) {
    content.append("  // the key with index in the active context\n");
    content.append("  private ").append(gemMap.key().type).append(" key(GemMapSyncProtocolTestScenario scenario, int index) {\n");
    switch (gemMap.key()) {
      case STR, TYPE_STR -> content.append("    return \"key\" + index;\n");
      case NUM, TYPE_NUM -> content.append("    return 1000 + index;\n");
      // 2^53 - 1 - 10 + index: within the keys a browser represents exactly, see GemValueMap#isSafeKey
      case LONG, TYPE_LONG, TYPE_BOXED -> content.append("    return 9007199254740981L + index;\n");
      case OBJ, ROLE, TYPE_OBJ, TYPE_SUB_KEY -> content.append("    return AssociationsManager.get")
          .append(gemMap.key().keyClass()).append("Sure(scenario.keyIds[index]);\n");
    }
    content.append("  }\n\n");

    content.append("  // the key with index as stored in the map: the key itself, or the gemId of the key object\n");
    content.append("  private ").append(gemMap.key().entry ? "Long" : gemMap.key().type)
        .append(" storedKey(GemMapSyncProtocolTestScenario scenario, int index) {\n");
    if (gemMap.key().entry) {
      content.append("    return scenario.keyIds[index];\n");
    }
    else {
      content.append("    return key(scenario, index);\n");
    }
    content.append("  }\n\n");
  }

  private static void appendCreateInitial(StringBuilder content, GemMap gemMap) {
    String ownerType = gemMap.ownerType();
    String valueType = gemMap.valueType();
    String ownerBuilder = ownerType + "Builder";
    String valueBuilder = valueType + "Builder";

    content.append("  default GemMapSyncProtocolTestScenario createDoneToDoneInitial(int valueCount) {\n");
    content.append("    GemMapSyncProtocolTestScenario scenario = newScenario();\n");
    content.append("    ").append(ownerType).append(" owner = newBareOwner();\n");
    content.append("    scenario.ownerId = owner.getGemId();\n");
    for (int index = 0; index < 3; index++) {
      content.append("    if (valueCount > ").append(index).append(") {\n");
      content.append("      ").append(valueType).append(" value").append(index)
          .append(" = newValue(scenario, ").append(index).append(");\n");
      content.append("      scenario.value").append(index).append("Id = value").append(index).append(".getGemId();\n");
      content.append("      owner.putByKey(").append(putArgs(gemMap, index, "value" + index)).append(");\n");
      content.append("    }\n");
    }
    content.append("    return scenario;\n");
    content.append("  }\n\n");

    content.append("  default GemMapSyncProtocolTestScenario createBuilderToDoneInitial(int valueCount) {\n");
    content.append("    GemMapSyncProtocolTestScenario scenario = newScenario();\n");
    content.append("    ").append(ownerBuilder).append(" ownerBuilder = AssociationsManager.")
        .append(lowerFirst(ownerType)).append("Builder();\n");
    for (int index = 0; index < 3; index++) {
      content.append("    if (valueCount > ").append(index).append(") {\n");
      content.append("      ").append(valueType).append(" value").append(index)
          .append(" = newValue(scenario, ").append(index).append(");\n");
      content.append("      scenario.value").append(index).append("Id = value").append(index).append(".getGemId();\n");
      content.append("      ownerBuilder.byKeyPut(").append(putArgs(gemMap, index, "value" + index)).append(");\n");
      content.append("    }\n");
    }
    content.append("    scenario.ownerId = ownerBuilder.build().orElseThrow().getGemId();\n");
    content.append("    return scenario;\n");
    content.append("  }\n\n");

    content.append("  default GemMapSyncProtocolTestScenario createBuilderToBuilderInitial(int valueCount) {\n");
    content.append("    GemMapSyncProtocolTestScenario scenario = newScenario();\n");
    content.append("    ").append(ownerBuilder).append(" ownerBuilder = AssociationsManager.")
        .append(lowerFirst(ownerType)).append("Builder();\n");
    for (int index = 0; index < 3; index++) {
      content.append("    ").append(valueBuilder).append(" value").append(index).append("Builder = null;\n");
      content.append("    if (valueCount > ").append(index).append(") {\n");
      content.append("      value").append(index).append("Builder = newValueBuilder(scenario, ").append(index).append(");\n");
      content.append("      scenario.value").append(index).append("Id = value").append(index).append("Builder.getGemId();\n");
      content.append("      ownerBuilder.byKeyPut(").append(putArgs(gemMap, index, "value" + index + "Builder"))
          .append(");\n");
      content.append("    }\n");
    }
    content.append("    scenario.ownerId = ownerBuilder.build().orElseThrow().getGemId();\n");
    for (int index = 0; index < 3; index++) {
      content.append("    if (value").append(index).append("Builder != null) {\n");
      content.append("      scenario.value").append(index).append("Id = value").append(index)
          .append("Builder.build().orElseThrow().getGemId();\n");
      content.append("    }\n");
    }
    content.append("    return scenario;\n");
    content.append("  }\n\n");
  }

  private static void appendOperation(StringBuilder content, GemMap gemMap) {
    String ownerType = gemMap.ownerType();
    String valueType = gemMap.valueType();
    content.append("  default void applyOperation(GemMapOperation operation, GemMapSyncProtocolTestScenario scenario) {\n");
    content.append("    ").append(ownerType).append(" owner = AssociationsManager.get").append(ownerType)
        .append("Sure(scenario.ownerId);\n");
    content.append("    switch (operation) {\n");
    content.append("      case BUILD_OBJECT -> scenario.targetValueId = scenario.value0Id;\n");

    content.append("      case PUT -> {\n");
    content.append("        ").append(valueType).append(" value = newValue(scenario, 1);\n");
    content.append("        scenario.targetValueId = value.getGemId();\n");
    content.append("        owner.putByKey(").append(putArgs(gemMap, 1, "value")).append(");\n");
    content.append("      }\n");

    content.append("      case PUT_BY_MAP -> {\n");
    content.append("        ").append(valueType).append(" value = newValue(scenario, 1);\n");
    content.append("        scenario.targetValueId = value.getGemId();\n");
    content.append("        owner.getByKeyMap().put(key(scenario, 1), value);\n");
    content.append("      }\n");

    content.append("      case GET -> scenario.targetValueId = scenario.value1Id;\n");

    if (Operation.REKEY.appliesTo(gemMap)) {
      content.append("      case REKEY -> {\n");
      content.append("        scenario.targetValueId = scenario.value0Id;\n");
      content.append("        AssociationsManager.get").append(valueType).append("Sure(scenario.value0Id).setKey(key(scenario, 3));\n");
      content.append("      }\n");
    }

    if (Operation.MOVE.appliesTo(gemMap)) {
      content.append("      case MOVE -> {\n");
      content.append("        scenario.targetValueId = scenario.value0Id;\n");
      content.append("        owner.putByKey(key(scenario, 3), AssociationsManager.get").append(valueType)
          .append("Sure(scenario.value0Id));\n");
      content.append("      }\n");
    }

    content.append("      case REMOVE -> {\n");
    content.append("        scenario.targetValueId = scenario.value0Id;\n");
    content.append("        owner.removeByKey(key(scenario, 0));\n");
    content.append("      }\n");

    content.append("      case REMOVE_BY_KEY_SET -> {\n");
    content.append("        scenario.targetValueId = scenario.value0Id;\n");
    content.append("        assertTrue(owner.getByKeyMap().keySet().remove(key(scenario, 0)));\n");
    content.append("      }\n");

    content.append("      case REPLACE -> {\n");
    content.append("        ").append(valueType).append(" value = newValue(scenario, 0);\n");
    content.append("        scenario.oldValueId = scenario.value0Id;\n");
    content.append("        scenario.targetValueId = value.getGemId();\n");
    content.append("        owner.putByKey(").append(putArgs(gemMap, 0, "value")).append(");\n");
    content.append("      }\n");

    content.append("      case DELETE_OBJECT -> {\n");
    content.append("        scenario.targetValueId = scenario.value0Id;\n");
    content.append("        assertTrue(AssociationsManager.delete").append(valueType).append("(scenario.targetValueId));\n");
    content.append("      }\n");

    content.append("      case DELETE_OWNER -> {\n");
    content.append("        ").append(ownerType).append(" otherOwner = newBareOwner();\n");
    content.append("        scenario.otherOwnerId = otherOwner.getGemId();\n");
    content.append("        otherOwner.putByKey(")
        .append(putArgs(gemMap, 0, "AssociationsManager.get" + valueType + "Sure(scenario.value0Id)")).append(");\n");
    content.append("        assertTrue(AssociationsManager.delete").append(ownerType).append("(scenario.ownerId));\n");
    content.append("      }\n");

    if (Operation.DELETE_KEY.appliesTo(gemMap)) {
      content.append("      case DELETE_KEY -> {\n");
      content.append("        scenario.targetValueId = scenario.value0Id;\n");
      content.append("        assertTrue(AssociationsManager.delete").append(gemMap.key().keyClass())
          .append("(scenario.keyIds[0]));\n");
      content.append("      }\n");
    }

    if (Operation.MOVE_OWNER.appliesTo(gemMap)) {
      content.append("      case MOVE_OWNER -> {\n");
      content.append("        ").append(ownerType).append(" otherOwner = newBareOwner();\n");
      content.append("        scenario.otherOwnerId = otherOwner.getGemId();\n");
      content.append("        scenario.targetValueId = scenario.value0Id;\n");
      content.append("        otherOwner.putByKey(")
          .append(putArgs(gemMap, 0, "AssociationsManager.get" + valueType + "Sure(scenario.value0Id)")).append(");\n");
      content.append("      }\n");
    }

    if (Operation.SET_OWNER.appliesTo(gemMap)) {
      content.append("      case SET_OWNER -> {\n");
      content.append("        ").append(ownerType).append(" otherOwner = newBareOwner();\n");
      content.append("        scenario.otherOwnerId = otherOwner.getGemId();\n");
      content.append("        scenario.targetValueId = scenario.value0Id;\n");
      content.append("        AssociationsManager.get").append(valueType).append("Sure(scenario.value0Id).")
          .append(gemMap.owner().single() ? "setOwner" : "addOwners").append("(otherOwner);\n");
      content.append("      }\n");
    }
    content.append("    }\n");
    content.append("  }\n\n");
  }

  private static void appendPreloadAndAssertions(StringBuilder content, GemMap gemMap) {
    String ownerType = gemMap.ownerType();
    String valueType = gemMap.valueType();

    content.append("  default void preloadOwner(long ownerId) {\n");
    content.append("    if (ownerId >= 0) {\n");
    content.append("      AssociationsManager.get").append(ownerType).append("Sure(ownerId);\n");
    content.append("    }\n");
    content.append("  }\n\n");

    content.append("  default void preloadValue(long valueId) {\n");
    content.append("    if (valueId >= 0) {\n");
    content.append("      AssociationsManager.get").append(valueType).append("Sure(valueId);\n");
    content.append("    }\n");
    content.append("  }\n\n");

    content.append("  default void assertValuesNotLoaded(GemMapSyncProtocolTestScenario scenario) {\n");
    content.append("    for (long valueId : new long[] {scenario.value0Id, scenario.value1Id, scenario.value2Id, scenario.targetValueId}) {\n");
    content.append("      if (valueId >= 0) {\n");
    content.append("        assertFalse(AssociationsManager.isLoaded").append(valueType).append("(valueId));\n");
    content.append("      }\n");
    content.append("    }\n");
    content.append("  }\n\n");

    if (gemMap.owner().bidirectional()) {
      appendOtherSideHelpers(content, gemMap);
    }

    appendEntryAssertions(content, gemMap, "", "AssociationsManager.get" + ownerType + "Sure(scenario.ownerId)");
    appendEntryAssertions(content, gemMap, "Persisted", "selectPersistedOwner(scenario.ownerId)");
    appendEntryPresentAssertion(content, gemMap, "OtherOwner",
        "AssociationsManager.get" + ownerType + "Sure(scenario.otherOwnerId)");
    appendEntryPresentAssertion(content, gemMap, "PersistedOtherOwner", "selectPersistedOwner(scenario.otherOwnerId)");

    content.append("  default void assertPersistedValueDeleted(long valueId) {\n");
    content.append("    assertFalse(AssociationsDAOFactory.get").append(valueType).append("DAO().select")
        .append(valueType).append("(valueId).isPresent());\n");
    content.append("  }\n\n");

    content.append("  default void assertPersistedValuePresent(long valueId) {\n");
    content.append("    assertTrue(AssociationsDAOFactory.get").append(valueType).append("DAO().select")
        .append(valueType).append("(valueId).isPresent());\n");
    content.append("  }\n\n");

    content.append("  default void assertPersistedOwnerDeleted(long ownerId) {\n");
    content.append("    assertFalse(AssociationsDAOFactory.get").append(ownerType).append("DAO().select")
        .append(ownerType).append("(ownerId).isPresent());\n");
    content.append("  }\n\n");

    content.append("  default boolean isOwnerKnown(long ownerId) {\n");
    content.append("    for (").append(ownerType).append(" owner : AssociationsManager.get").append(ownerType).append("List()) {\n");
    content.append("      if (owner.getGemId() == ownerId) {\n");
    content.append("        return true;\n");
    content.append("      }\n");
    content.append("    }\n");
    content.append("    return false;\n");
    content.append("  }\n\n");

    content.append("  default boolean isValueKnown(long valueId) {\n");
    content.append("    for (").append(valueType).append(" value : AssociationsManager.get").append(valueType).append("List()) {\n");
    content.append("      if (value.getGemId() == valueId) {\n");
    content.append("        return true;\n");
    content.append("      }\n");
    content.append("    }\n");
    content.append("    return false;\n");
    content.append("  }\n\n");

    content.append("  private ").append(ownerType).append(" selectPersistedOwner(long ownerId) {\n");
    content.append("    return AssociationsDAOFactory.get").append(ownerType).append("DAO().select")
        .append(ownerType).append("(ownerId).orElseThrow();\n");
    content.append("  }\n\n");
  }

  /**
   * assert(Persisted)OtherSide(valueId, ownerIds...): the other side of the value names exactly the given owners
   * (none: -1 for a single owner, an empty list for several)
   */
  private static void appendOtherSideHelpers(StringBuilder content, GemMap gemMap) {
    String valueType = gemMap.valueType();

    for (String persisted : List.of("", "Persisted")) {
      String value = persisted.isEmpty()
          ? "AssociationsManager.get" + valueType + "Sure(valueId)"
          : "AssociationsDAOFactory.get" + valueType + "DAO().select" + valueType + "(valueId).orElseThrow()";
      content.append("  default void assert").append(persisted).append("OtherSide(long valueId, long... ownerIds) {\n");
      content.append("    ").append(valueType).append(" value = ").append(value).append(";\n");
      if (gemMap.owner().single()) {
        content.append("    assertEquals(ownerIds.length == 0 ? -1 : ownerIds[0], value.getOwnerGem().getGemId());\n");
      }
      else {
        content.append("    java.util.Set<Long> expected = new java.util.HashSet<>();\n");
        content.append("    for (long ownerId : ownerIds) {\n");
        content.append("      expected.add(ownerId);\n");
        content.append("    }\n");
        content.append("    java.util.List<Long> actual = value._internal_getOwnersGemList().getIDs();\n");
        content.append("    assertEquals(expected, new java.util.HashSet<>(actual));\n");
        content.append("    assertEquals(expected.size(), actual.size());\n");
      }
      content.append("  }\n\n");
    }
  }

  private static void appendEntryAssertions(StringBuilder content, GemMap gemMap, String persisted, String owner) {
    String ownerType = gemMap.ownerType();

    appendEntryPresentAssertion(content, gemMap, persisted, owner);

    content.append("  default void assert").append(persisted)
        .append("EntryAbsent(GemMapSyncProtocolTestScenario scenario, int keyIndex) {\n");
    content.append("    ").append(ownerType).append(" owner = ").append(owner).append(";\n");
    content.append("    assertFalse(owner.containsKeyByKey(key(scenario, keyIndex)));\n");
    content.append("  }\n\n");

    // by the stored key: the key object of a GemEntryMap may be deleted
    content.append("  default void assert").append(persisted)
        .append("StoredKeyAbsent(GemMapSyncProtocolTestScenario scenario, int keyIndex) {\n");
    content.append("    ").append(ownerType).append(" owner = ").append(owner).append(";\n");
    content.append("    assertNull(owner._internal_getByKeyGemMap().getID(storedKey(scenario, keyIndex)));\n");
    content.append("  }\n\n");

    content.append("  default void assert").append(persisted)
        .append("MapSize(GemMapSyncProtocolTestScenario scenario, int size) {\n");
    content.append("    ").append(ownerType).append(" owner = ").append(owner).append(";\n");
    content.append("    assertEquals(size, owner.sizeByKeys());\n");
    content.append("  }\n\n");
  }

  /**
   * assert<prefix>EntryPresent: the map of the owner holds the value under the key with index
   */
  private static void appendEntryPresentAssertion(StringBuilder content, GemMap gemMap, String prefix, String owner) {
    content.append("  default void assert").append(prefix)
        .append("EntryPresent(GemMapSyncProtocolTestScenario scenario, int keyIndex, long valueId) {\n");
    content.append("    ").append(gemMap.ownerType()).append(" owner = ").append(owner).append(";\n");
    content.append("    assertTrue(owner.containsKeyByKey(key(scenario, keyIndex)));\n");
    content.append("    assertEquals(Long.valueOf(valueId), owner._internal_getByKeyGemMap().getID(storedKey(scenario, keyIndex)));\n");
    content.append("    assertEquals(valueId, owner.getByKey(key(scenario, keyIndex)).getGemId());\n");
    content.append("  }\n\n");
  }

  private static void appendObjectFactories(StringBuilder content, GemMap gemMap) {
    String ownerType = gemMap.ownerType();
    String valueType = gemMap.valueType();

    content.append("  private GemMapSyncProtocolTestScenario newScenario() {\n");
    content.append("    GemMapSyncProtocolTestScenario scenario = new GemMapSyncProtocolTestScenario();\n");
    if (gemMap.key().entry) {
      content.append("    for (int index = 0; index < scenario.keyIds.length; index++) {\n");
      content.append("      scenario.keyIds[index] = AssociationsManager.").append(lowerFirst(gemMap.key().keyClass()))
          .append("Builder().name(\"key\" + index).build()\n");
      content.append("          .orElseThrow().getGemId();\n");
      content.append("    }\n");
    }
    content.append("    return scenario;\n");
    content.append("  }\n\n");

    content.append("  private ").append(ownerType).append(" newBareOwner() {\n");
    content.append("    return AssociationsManager.").append(lowerFirst(ownerType)).append("Builder().build().orElseThrow();\n");
    content.append("  }\n\n");

    content.append("  private ").append(valueType).append("Builder newValueBuilder(GemMapSyncProtocolTestScenario scenario, int keyIndex) {\n");
    if (gemMap.key().chosen) {
      // the key is chosen on put, the value has no key attribute
      content.append("    return AssociationsManager.").append(lowerFirst(valueType)).append("Builder();\n");
    }
    else {
      content.append("    return AssociationsManager.").append(lowerFirst(valueType)).append("Builder().key(key(scenario, keyIndex));\n");
    }
    content.append("  }\n\n");

    content.append("  private ").append(valueType).append(" newValue(GemMapSyncProtocolTestScenario scenario, int keyIndex) {\n");
    content.append("    return newValueBuilder(scenario, keyIndex).build().orElseThrow();\n");
    content.append("  }\n");
  }

  /**
   * the arguments of a put: the value, whose key is derived from it, or the chosen key with index and the value
   */
  private static String putArgs(GemMap gemMap, int keyIndex, String value) {
    return gemMap.key().chosen ? "key(scenario, " + keyIndex + "), " + value : value;
  }

  private static String lowerFirst(String value) {
    if (value.isEmpty()) {
      return value;
    }
    return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1);
  }

  private static void recreateDirectory(Path directory) throws IOException {
    if (Files.exists(directory)) {
      try (Stream<Path> stream = Files.walk(directory)) {
        List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
        for (Path path : paths) {
          Files.delete(path);
        }
      }
    }
    Files.createDirectories(directory);
  }
}
