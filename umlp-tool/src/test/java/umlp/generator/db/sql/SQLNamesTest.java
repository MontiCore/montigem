/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.db.sql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLNamesTest {

  private static int bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8).length;
  }

  private static String repeat(String value, int times) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < times; i++) {
      builder.append(value);
    }
    return builder.toString();
  }

  @Test
  public void testShortNamesAreUnchanged() {
    assertEquals("person", SQLNames.shorten("person"));
    assertEquals("assoc_person_person_address_address", SQLNames.shorten("assoc_person_person_address_address"));
    assertEquals("person$nicknames", SQLNames.shorten("person$nicknames"));
  }

  @Test
  public void testNameAtTheLimitIsUnchanged() {
    String atLimit = repeat("a", SQLNames.MAX_BYTES);
    assertEquals(atLimit, SQLNames.shorten(atLimit));

    String overLimit = repeat("a", SQLNames.MAX_BYTES + 1);
    assertNotEquals(overLimit, SQLNames.shorten(overLimit));
  }

  @Test
  public void testResultNeverExceedsTheLimit() {
    for (int length = 60; length < 400; length++) {
      String name = repeat("some_segment_", length / 13 + 1) + "tail" + length;
      assertTrue(bytes(SQLNames.shorten(name)) <= SQLNames.MAX_BYTES,
          "too long for input of length " + length);
    }
  }

  @Test
  public void testIsDeterministicAndIdempotent() {
    String name = "assoc_" + repeat("segment_", 12) + "end";

    assertEquals(SQLNames.shorten(name), SQLNames.shorten(name));
    assertEquals(SQLNames.shorten(name), SQLNames.shorten(SQLNames.shorten(name)));
  }

  @Test
  public void testNamesDifferingBeyondTheLimitStayDistinct() {
    String common = "aaaa_bbbb_cccc_dddd_eeee_ffff_gggg_hhhh_iiii_jjjj_kkkk_llll_mmmm";
    String left = SQLNames.shorten(common + "_left");
    String right = SQLNames.shorten(common + "_right");

    assertNotEquals(left, right);
    assertTrue(bytes(left) <= SQLNames.MAX_BYTES);
    assertTrue(bytes(right) <= SQLNames.MAX_BYTES);
  }

  @Test
  public void testRepeatedBlocksAreCollapsed() {
    // derived association name of DemoRec: battery_pack occurs as class and as role name
    String shortened = SQLNames.shorten("assoc_", "battery_pack_battery_pack_all_components_demo_rec_component", "");

    assertTrue(shortened.startsWith("assoc_battery_pack_all_components_demo_rec_component_"), shortened);
    assertEquals(61, bytes(shortened));
  }

  @Test
  public void testRepeatedBlocksOfSeveralSegmentsAreCollapsed() {
    String shortened = SQLNames.shorten("assoc_",
        "joint_connection_joint_connection_joint_connection_kind_joint_connection_kind", "");

    assertTrue(shortened.startsWith("assoc_joint_connection_joint_connection_kind_"), shortened);
    assertEquals(53, bytes(shortened));
  }

  @Test
  public void testDistinctSegmentsAreTruncatedFairly() {
    String shortened = SQLNames.shorten("alpha_bravo_charlie_delta_echo_foxtrot_golf_hotel_india_juliett_kilo");

    // every segment is capped at the same length, short ones are not padded away
    assertTrue(shortened.startsWith("alph_brav_char_delt_echo_foxt_golf_hote_indi_juli_kilo_"), shortened);
    assertEquals(SQLNames.MAX_BYTES, bytes(shortened));
  }

  @Test
  public void testPrefixAndSuffixSurvive() {
    String shortened = SQLNames.shorten("assoc_", repeat("segment_", 12) + "end", "_id");

    assertTrue(shortened.startsWith("assoc_"), shortened);
    assertTrue(shortened.endsWith("_id"), shortened);
    assertTrue(bytes(shortened) <= SQLNames.MAX_BYTES);
  }

  @Test
  public void testShortenReservingLeavesRoomForTheSuffix() {
    String body = repeat("segment_", 12) + "end";
    String reserved = SQLNames.shortenReserving(body, "_id");

    assertNotEquals(body, reserved);
    assertTrue(bytes(reserved + "_id") <= SQLNames.MAX_BYTES);
  }

  @Test
  public void testSuffixSurvivesEvenWhenTheAffixesLeaveNoRoom() {
    String shortened = SQLNames.shorten(repeat("p", 30), repeat("b", 40), repeat("s", 25));

    assertTrue(shortened.endsWith(repeat("s", 25)), shortened);
    assertEquals(SQLNames.MAX_BYTES, bytes(shortened));
  }

  @Test
  public void testShortenReservingIsIdentityForShortNames() {
    assertEquals("owner", SQLNames.shortenReserving("owner", "_id"));
  }

  @Test
  public void testMultiByteCharactersAreCountedAsBytes() {
    String umlauts = repeat("ä", 70);

    assertTrue(bytes(SQLNames.shorten(umlauts)) <= SQLNames.MAX_BYTES);
  }

  @Test
  public void testLegacyTruncationMatchesPostgres() {
    String name = "assoc_battery_pack_battery_pack_all_components_demo_rec_component";

    assertEquals("assoc_battery_pack_battery_pack_all_components_demo_rec_compone",
        SQLNames.legacyTruncation(name));
    assertEquals(SQLNames.MAX_BYTES, bytes(SQLNames.legacyTruncation(name)));
    assertEquals("person", SQLNames.legacyTruncation("person"));
  }
}
