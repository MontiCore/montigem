/* (c) https://github.com/MontiCore/monticore */
package collabtextmodule;

import collabtextmodule.commands.CollabTextChunk_setContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import umlp.backendrte.command.Command;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.command.CommandObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollabTextTest {
  CollabText text;

  @BeforeEach
  void init() {
    CollabTextModuleMock.init();
    text = CollabTextModuleManager.collabTextBuilder().build().get();
    text.setContent("hello");
  }

  @Test
  void contentFollowsAnOrdinarySerializedChunkMutation() {
    String content = "|\\\"\n";
    String json = CommandObjectMapper.serializeCommand(
        new CollabTextChunk_setContent(text.getChunks(0).getGemId(), content));
    Command<?> received = CommandObjectMapper.deserializeCommand(json).orElseThrow();
    assertTrue(CommandManager.executeCommand(received).isSuccessful());
    assertEquals(content, text.getContent());
    assertEquals(content.length(), text.getLength());
  }

  @Test
  void typingChangesOnlyTheChunkItHappensIn() {
    String longText = "0123456789".repeat(300);
    text.setContent(longText);
    List<String> before = chunkContents();
    List<CollabTextChunk> chunksBefore = new ArrayList<>(text.getChunksList());

    text.setContent(longText.substring(0, 1500) + "X" + longText.substring(1500));

    List<String> after = chunkContents();
    assertEquals(before.size(), after.size());
    int changed = 0;
    for (int i = 0; i < before.size(); i++) {
      assertEquals(chunksBefore.get(i).getGemId(), text.getChunks(i).getGemId());
      if (!before.get(i).equals(after.get(i))) {
        changed++;
      }
    }
    assertEquals(1, changed);
    assertEquals(longText.substring(0, 1500) + "X" + longText.substring(1500), text.getContent());
  }

  @Test
  void rejectsReplacementsOutsideTheCurrentChunks() {
    assertFalse(text.replace(-1, 0, "x"));
    assertFalse(text.replace(4, 3, "x"));
    assertEquals("hello", text.getContent());
  }

  @Test
  void replacementsSplitAndRemoveChunksWithoutLeavingOrphans() {
    Random random = new Random(4);
    String expected = text.getContent();
    for (int i = 0; i < 3000; i++) {
      int position = random.nextInt(expected.length() + 1);
      int deleteCount = random.nextInt(Math.min(expected.length() - position, i % 50 == 0 ? 2000 : 30) + 1);
      String inserted = String.valueOf((char) ('a' + i % 26)).repeat(random.nextInt(i % 40 == 0 ? 1500 : 40));
      expected = expected.substring(0, position) + inserted + expected.substring(position + deleteCount);

      assertTrue(text.replace(position, deleteCount, inserted));
      assertEquals(expected.length(), text.getLength());
      if (i % 20 == 0) {
        assertEquals(expected, text.getContent());
        assertEquals(text.sizeChunkss(), CollabTextModuleManager.getCollabTextChunkList().size());
        for (String content : chunkContents()) {
          assertFalse(content.isEmpty());
          assertTrue(content.length() <= CollabText.MAX_CHUNK_LENGTH);
        }
      }
    }
    assertTrue(text.replace(0, expected.length(), ""));
    assertEquals(0, text.sizeChunkss());
    assertEquals(0, CollabTextModuleManager.getCollabTextChunkList().size());
    assertEquals("", text.getContent());
  }

  List<String> chunkContents() {
    List<String> contents = new ArrayList<>();
    for (CollabTextChunk chunk : text.getChunksList()) {
      contents.add(chunk.getContent());
    }
    return contents;
  }
}
