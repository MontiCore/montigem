/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.db.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLRenameGeneratorTest {

  private static String repeat(String value, int times) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < times; i++) {
      builder.append(value);
    }
    return builder.toString();
  }

  @Test
  public void testNothingToRenameForShortNames() {
    SQLRenameGenerator generator = new SQLRenameGenerator();
    generator.addTable("person", "person");
    generator.addTable("assoc_person_person_address_address", "assoc_person_person_address_address");
    generator.addColumn("person", "first_name", "first_name");

    assertTrue(generator.isEmpty());
  }

  @Test
  public void testRenamesFromTheNameThePostgresTruncationLeftBehind() {
    String rawName = "assoc_battery_pack_battery_pack_all_components_demo_rec_component";
    String name = SQLSchemaGenerator.convertAssocName("BatteryPackBatteryPackAllComponentsDemoRecComponent");

    SQLRenameGenerator generator = new SQLRenameGenerator();
    generator.addTable(rawName, name);

    assertFalse(generator.isEmpty());
    String script = generator.render("DemoRec");
    assertTrue(script.contains("ALTER TABLE \"assoc_battery_pack_battery_pack_all_components_demo_rec_compone\""), script);
    assertTrue(script.contains("RENAME TO \"" + name + "\""), script);
  }

  @Test
  public void testColumnRenameTargetsTheAlreadyRenamedTable() {
    SQLRenameGenerator generator = new SQLRenameGenerator();
    generator.addColumn("assoc_short_42a1b7c9", repeat("verylongrollname_", 5) + "end_id", "shortened_id");

    String script = generator.render("Demo");
    assertTrue(script.contains("ALTER TABLE \"assoc_short_42a1b7c9\" RENAME COLUMN"), script);
    assertTrue(script.contains("TO \"shortened_id\""), script);
  }

  @Test
  public void testTablesThatUsedToCollideAreLeftOutOfTheScript() {
    String common = repeat("x", SQLNames.MAX_BYTES);
    String left = common + "_left";
    String right = common + "_right";

    SQLRenameGenerator generator = new SQLRenameGenerator();
    generator.addTable(left, SQLNames.shorten(left));
    generator.addTable(right, SQLNames.shorten(right));

    // both used to be the same table, so neither can be migrated - but they do get distinct names now
    assertTrue(generator.isEmpty());
    assertFalse(SQLNames.shorten(left).equals(SQLNames.shorten(right)));
  }

  @Test
  public void testATableThatIsNotShortenedIsNeverRenamedAway() {
    // a list attribute table always carries its class table name as a prefix, so a class name of
    // exactly the limit is what the longer name truncates to - renaming it would move a live table
    String classTable = repeat("c", SQLNames.MAX_BYTES);
    String listTable = classTable + "$values";

    SQLRenameGenerator generator = new SQLRenameGenerator();
    generator.addTable(classTable, SQLNames.shorten(classTable));
    generator.addTable(listTable, SQLNames.shorten(listTable));

    assertEquals(classTable, SQLNames.shorten(classTable), "the class table is at the limit and must stay");
    assertTrue(generator.isEmpty(), "neither table may be renamed: " + generator.render("Demo"));
  }

  @Test
  public void testRepeatedRegistrationOfTheSameTableIsHarmless() {
    String rawName = "assoc_" + repeat("segment_", 12) + "end";
    String name = SQLNames.shorten(rawName);

    SQLRenameGenerator generator = new SQLRenameGenerator();
    generator.addTable(rawName, name);
    generator.addTable(rawName, name);

    assertFalse(generator.isEmpty());
    assertEquals(2, generator.render("Demo").split("ALTER TABLE").length,
        "the same table must not be renamed twice");
  }
}
