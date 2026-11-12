/* (c) https://github.com/MontiCore/monticore */
package collabtextmodule;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextEditTest {
  static String randomText(Random random, int maxLength) {
    StringBuilder text = new StringBuilder();
    int length = random.nextInt(maxLength + 1);
    for (int i = 0; i < length; i++) {
      text.append("ab|\n\"".charAt(random.nextInt(5)));
    }
    return text.toString();
  }

  static TextEdit randomEdit(Random random, String text) {
    int position = random.nextInt(text.length() + 1);
    int deleteCount = random.nextInt(Math.min(4, text.length() - position + 1));
    return new TextEdit(position, deleteCount, randomText(random, 3));
  }

  @Test
  void encodingKeepsSeparatorsAndLineBreaksOfTheInsertedText() {
    TextEdit decoded = TextEdit.decode(new TextEdit(12, 3, "a|b\n\"c\"|").encode());
    assertEquals(12, decoded.getPosition());
    assertEquals(3, decoded.getDeleteCount());
    assertEquals("a|b\n\"c\"|", decoded.getInserted());
  }

  @Test
  void rebasedEditsLeadToTheSameTextInBothOrders() {
    Random random = new Random(1);
    for (int i = 0; i < 200000; i++) {
      String text = randomText(random, 8);
      TextEdit first = randomEdit(random, text);
      TextEdit second = randomEdit(random, text);
      String firstThenSecond = second.rebase(first, true).apply(first.apply(text));
      String secondThenFirst = first.rebase(second, false).apply(second.apply(text));
      assertEquals(firstThenSecond, secondThenFirst,
          () -> "'" + text + "' " + first.encode() + " / " + second.encode());
    }
  }

  @Test
  void insertionsAtTheSamePlaceAreBothKept() {
    TextEdit first = new TextEdit(1, 0, "X");
    TextEdit second = new TextEdit(1, 0, "Y");
    assertEquals("aXYb", second.rebase(first, true).apply(first.apply("ab")));
    assertEquals("aXYb", first.rebase(second, false).apply(second.apply("ab")));
  }

  @Test
  void editWithinReplacedTextIsDropped() {
    TextEdit replaceAll = new TextEdit(0, 5, "new");
    TextEdit typeInside = new TextEdit(2, 0, "X");
    assertEquals("new", typeInside.rebase(replaceAll, true).apply(replaceAll.apply("hello")));
    assertEquals("new", replaceAll.rebase(typeInside, false).apply(typeInside.apply("hello")));
  }

  @Test
  void mergedEditHasTheEffectOfBothEdits() {
    Random random = new Random(2);
    int merged = 0;
    for (int i = 0; i < 200000; i++) {
      String text = randomText(random, 8);
      TextEdit first = randomEdit(random, text);
      String afterFirst = first.apply(text);
      TextEdit second = randomEdit(random, afterFirst);
      if (first.touches(second)) {
        merged++;
        assertEquals(second.apply(afterFirst), first.merge(second).apply(text),
            () -> "'" + text + "' " + first.encode() + " / " + second.encode());
      }
    }
    assertTrue(merged > 10000);
  }

  @Test
  void typingAndDeletingBackwardsMergeIntoOneEdit() {
    TextEdit typed = new TextEdit(3, 0, "a").merge(new TextEdit(4, 0, "b"));
    assertEquals("3|0|ab", typed.encode());
    assertEquals("3|0|a", typed.merge(new TextEdit(4, 1, "")).encode());
    assertEquals("2|2|", new TextEdit(3, 1, "").merge(new TextEdit(2, 1, "")).encode());
    assertFalse(new TextEdit(3, 0, "a").touches(new TextEdit(9, 0, "b")));
  }

  @Test
  void shiftMovesIndicesBehindTheEdit() {
    TextEdit edit = new TextEdit(4, 2, "xyz");
    assertEquals(4, edit.shift(4));
    assertEquals(7, edit.shift(5));
    assertEquals(7, edit.shift(6));
    assertEquals(11, edit.shift(10));
  }
}
