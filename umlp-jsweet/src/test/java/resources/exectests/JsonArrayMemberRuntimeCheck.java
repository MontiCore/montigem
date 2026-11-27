package resources.exectests;

import de.monticore.symboltable.serialization.JsonParser;
import de.monticore.symboltable.serialization.json.JsonElement;
import de.monticore.symboltable.serialization.json.JsonObject;
import util.JSweetTest;

import java.util.List;
import java.util.Optional;

/**
 * JsonObject#getArrayMember, #getArrayMemberOpt and JsonArray#getValues (BasicsAdapter): the elements of the JSON array become the
 * elements of an AsyncList (Optional: or null if the member is no JSON array).
 */
public class JsonArrayMemberRuntimeCheck {

  private static final String JSON = "{\"numbers\": [1, 2, 3], \"empty\": [], \"text\": \"abc\"}";

  // the getter is the only async call of these methods, so they must become async (asyncjre.json)
  private static List<JsonElement> arrayMember(JsonObject json, String name) {
    return json.getArrayMember(name);
  }

  private static Optional<List<JsonElement>> arrayMemberOpt(JsonObject json, String name) {
    return json.getArrayMemberOpt(name);
  }

  private static List<JsonElement> values(JsonObject json, String name) {
    return json.getMember(name).getAsJsonArray().getValues();
  }

  private static int sum(List<JsonElement> elements) {
    int sum = 0;
    for (JsonElement e : elements) {
      sum += e.getAsJsonNumber().getNumberAsInteger();
    }
    return sum;
  }

  @JSweetTest
  public static String arrayMemberElements() {
    List<JsonElement> numbers = arrayMember(JsonParser.parseJsonObject(JSON), "numbers");
    return numbers.size() + ":" + sum(numbers);
  }

  @JSweetTest
  public static String arrayValues() {
    List<JsonElement> numbers = values(JsonParser.parseJsonObject(JSON), "numbers");
    return numbers.size() + ":" + sum(numbers);
  }

  @JSweetTest
  public static String emptyArrayMember() {
    return "" + arrayMember(JsonParser.parseJsonObject(JSON), "empty").size();
  }

  @JSweetTest
  public static String arrayMemberOptPresent() {
    Optional<List<JsonElement>> numbers = arrayMemberOpt(JsonParser.parseJsonObject(JSON), "numbers");
    return numbers.isPresent() + ":" + numbers.get().size() + ":" + sum(numbers.get());
  }

  @JSweetTest
  public static String arrayMemberOptMissing() {
    return "" + arrayMemberOpt(JsonParser.parseJsonObject(JSON), "missing").isPresent();
  }

  @JSweetTest
  public static String arrayMemberOptNoArray() {
    return "" + arrayMemberOpt(JsonParser.parseJsonObject(JSON), "text").isPresent();
  }

  @JSweetTest
  public static String arrayMemberOptOrElse() {
    return "" + arrayMemberOpt(JsonParser.parseJsonObject(JSON), "missing").orElse(List.of()).size();
  }
}
