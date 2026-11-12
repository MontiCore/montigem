/* (c) https://github.com/MontiCore/monticore */
package collabtextmodule;

import collabtextmodule.commands.ApplyEdit;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollabTextSyncTest {
  CollabText text;

  @BeforeEach
  void init() {
    CollabTextModuleMock.init();
    text = CollabTextModuleManager.collabTextBuilder().build().get();
    assertTrue(CollabTextSync.apply(text, new TextEdit(0, 0, "hello")));
  }

  CollabTextCursor cursor(String userName) {
    CollabTextCursor cursor = CollabTextModuleManager.collabTextCursorBuilder()
        .userName(userName).color("#000000").participant(0L).build().get();
    text.addCursors(cursor);
    return cursor;
  }

  boolean send(CollabTextCursor author, int baseRevision, TextEdit edit) {
    String json = CommandObjectMapper.serializeCommand(
        new ApplyEdit(text.getGemId(), author.getGemId(), baseRevision, edit.encode()));
    Command<?> command = CommandObjectMapper.deserializeCommand(json).orElseThrow();
    assertInstanceOf(ApplyEdit.class, command);
    return CommandManager.executeCommand(command).isSuccessful();
  }

  List<String> chunkContents() {
    List<String> contents = new ArrayList<>();
    for (CollabTextChunk chunk : text.getChunksList()) {
      contents.add(chunk.getContent());
    }
    return contents;
  }

  @Test
  void appliesSerializedEditAndPublishesIt() {
    CollabTextCursor author = cursor("Ada");
    assertEquals(1, text.getRevision());
    assertEquals(0, text.getLastEditAuthor());

    assertTrue(send(author, 1, new TextEdit(5, 0, " \"world\"|\n")));

    assertEquals("hello \"world\"|\n", text.getContent());
    assertEquals(2, text.getRevision());
    assertEquals(author.getGemId(), text.getLastEditAuthor());
    assertEquals(15, text.getLength());
    assertEquals("5|0| \"world\"|\n", text.getLastTextEdit().encode());
  }

  @Test
  void rebasesEditOnTheEditsItsAuthorHasNotSeen() {
    CollabTextCursor ada = cursor("Ada");
    CollabTextCursor bob = cursor("Bob");
    assertTrue(send(ada, 1, new TextEdit(0, 0, ">> ")));
    assertTrue(send(bob, 1, new TextEdit(5, 0, "!")));

    assertEquals(">> hello!", text.getContent());
    assertEquals("8|0|!", text.getLastTextEdit().encode());
  }

  @Test
  void rejectsEditsThatCannotBeRebased() {
    CollabTextCursor author = cursor("Ada");
    assertFalse(send(author, 2, new TextEdit(0, 0, "x")));
    assertFalse(send(author, 1, new TextEdit(4, 3, "x")));
    assertEquals("hello", text.getContent());
    assertEquals(1, text.getRevision());
  }

  @Test
  void keepsCursorsOfTheBackendAndRemovesThoseOfDisconnectedUsers() {
    CollabTextCursor author = cursor("Ada");
    CollabTextCursor gone = cursor("Bob");
    gone.setParticipant(4711L);

    assertTrue(send(author, 1, new TextEdit(0, 0, "x")));

    assertEquals(1, text.getCursorsList().size());
    assertEquals(author.getGemId(), text.getCursors(0).getGemId());
  }

  @Test
  void typingChangesOnlyTheChunkItHappensIn() {
    String longText = "0123456789".repeat(300);
    assertTrue(CollabTextSync.apply(text, new TextEdit(0, 5, longText)));
    List<String> before = chunkContents();
    assertTrue(before.size() >= 3000 / ChunkChange.MAX_CHUNK_LENGTH);
    List<CollabTextChunk> chunksBefore = new ArrayList<>(text.getChunksList());

    assertTrue(CollabTextSync.apply(text, new TextEdit(1500, 0, "X")));

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
  void chunksStaySmallAndNonEmptyUnderRandomEdits() {
    Random random = new Random(4);
    String expected = text.getContent();
    for (int i = 0; i < 3000; i++) {
      int position = random.nextInt(expected.length() + 1);
      int deleteCount = random.nextInt(Math.min(expected.length() - position, i % 50 == 0 ? 2000 : 30) + 1);
      String inserted = String.valueOf((char) ('a' + i % 26)).repeat(random.nextInt(i % 40 == 0 ? 1500 : 40));
      TextEdit edit = new TextEdit(position, deleteCount, inserted);
      expected = edit.apply(expected);

      assertTrue(CollabTextSync.apply(text, edit));

      assertEquals(expected.length(), text.getLength());
      if (i % 20 == 0) {
        assertEquals(expected, text.getContent());
        for (String content : chunkContents()) {
          assertFalse(content.isEmpty());
          assertTrue(content.length() <= ChunkChange.MAX_CHUNK_LENGTH);
        }
      }
    }
    assertTrue(CollabTextSync.apply(text, new TextEdit(0, expected.length(), "")));
    assertEquals(0, text.sizeChunkss());
    assertEquals("", text.getContent());
  }

  @Test
  void editorsFollowTheLengthsOfTheChunks() {
    Random random = new Random(5);
    List<Integer> followed = chunkLengths();
    for (int i = 0; i < 2000; i++) {
      int length = text.getLength();
      int position = random.nextInt(length + 1);
      int deleteCount = random.nextInt(Math.min(length - position, i % 50 == 0 ? 2000 : 30) + 1);
      String inserted = "x".repeat(random.nextInt(i % 40 == 0 ? 1500 : 40));
      TextEdit edit = new TextEdit(position, deleteCount, inserted);
      followed = ChunkChange.of(followed, edit).apply(followed);

      assertTrue(CollabTextSync.apply(text, edit));

      assertEquals(chunkLengths(), followed);
    }
  }

  List<Integer> chunkLengths() {
    List<Integer> lengths = new ArrayList<>();
    for (String content : chunkContents()) {
      lengths.add(content.length());
    }
    return lengths;
  }

  /** What an editor in the frontend does, see CollabTextarea. */
  class Client {
    final CollabTextCursor cursor;
    // The text after `revision` edits, kept up to date with the published edits only
    String confirmed = text.getContent();
    int revision = text.getRevision();
    String view = confirmed;
    TextEdit inFlight;
    final List<TextEdit> unsent = new ArrayList<>();
    // The backend already applied the edit, its publication has not arrived here yet
    boolean inFlightApplied;

    Client(String userName) {
      cursor = cursor(userName);
    }

    void type(TextEdit edit) {
      view = edit.apply(view);
      if (!unsent.isEmpty() && unsent.get(unsent.size() - 1).touches(edit)) {
        unsent.set(unsent.size() - 1, unsent.get(unsent.size() - 1).merge(edit));
      } else {
        unsent.add(edit);
      }
      sendNext();
    }

    void sendNext() {
      if (inFlight == null && !unsent.isEmpty()) {
        inFlight = unsent.remove(0);
        inFlightApplied = false;
      }
    }

    void receive(String lastEdit) {
      CollabText published = CollabTextModuleManager.collabTextBuilder().lastEdit(lastEdit).build().get();
      TextEdit remote = published.getLastTextEdit();
      confirmed = remote.apply(confirmed);
      assertEquals(published.getLength(), confirmed.length());
      revision++;
      assertEquals(published.getRevision(), revision);
      if (published.getLastEditAuthor() == cursor.getGemId()) {
        inFlight = null;
        sendNext();
        return;
      }
      view = confirmed;
      if (inFlight != null) {
        TextEdit rebased = inFlight.rebase(remote, true);
        remote = remote.rebase(inFlight, false);
        inFlight = rebased;
        view = inFlight.apply(view);
      }
      for (int i = 0; i < unsent.size(); i++) {
        TextEdit rebased = unsent.get(i).rebase(remote, true);
        remote = remote.rebase(unsent.get(i), false);
        unsent.set(i, rebased);
        view = rebased.apply(view);
      }
    }
  }

  @Test
  void clientsTypingAtTheSameTimeEndUpWithTheSameText() {
    Random random = new Random(3);
    for (int round = 0; round < 300; round++) {
      init();
      List<Client> clients = List.of(new Client("Ada"), new Client("Bob"), new Client("Cy"));
      int firstRevision = text.getRevision();
      // lastEdit of each revision published since
      List<String> published = new ArrayList<>();

      for (int step = 0; step < 400 || clients.stream().anyMatch(
          c -> c.inFlight != null || c.revision < firstRevision + published.size()); step++) {
        Client client = clients.get(random.nextInt(clients.size()));
        int action = random.nextInt(3);
        if (action == 0 && step < 400) {
          client.type(TextEditTest.randomEdit(random, client.view));
        } else if (action == 1 && client.inFlight != null && !client.inFlightApplied) {
          // The client keeps its edit rebased on what it received, so it does not matter for the
          // result whether the edit was sent earlier or is sent now
          assertTrue(send(client.cursor, client.revision, client.inFlight));
          client.inFlightApplied = true;
          published.add(text.getLastEdit());
        } else if (client.revision < firstRevision + published.size()) {
          client.receive(published.get(client.revision - firstRevision));
        }
      }
      for (Client client : clients) {
        assertEquals(text.getContent(), client.view, "round " + round);
        assertEquals(text.getContent(), client.confirmed, "round " + round);
        assertTrue(client.unsent.isEmpty());
      }
    }
  }
}
