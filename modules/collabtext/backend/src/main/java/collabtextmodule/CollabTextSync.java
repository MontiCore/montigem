/* (c) https://github.com/MontiCore/monticore */
package collabtextmodule;

import jsweet.lang.Erased;
import umlp.backendrte.command.CommandManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Applies the edits of all users to a text one after another, which gives them their revision. */
@Erased
public final class CollabTextSync {
  // An edit is rebased on the edits applied while it was on its way to the backend, these are few
  private static final int HISTORY_SIZE = 1000;

  // The most recent edits of each text by its gemId, the last one led to the current revision
  private static final Map<Long, List<TextEdit>> histories = new HashMap<>();

  private CollabTextSync() {}

  /** An edit of the backend itself on the current text, e.g. to set the initial content. */
  public static synchronized boolean apply(CollabText text, TextEdit edit) {
    return apply(text, 0, text.getRevision(), edit);
  }

  /**
   * @param authorGemId cursor of the user who made the edit
   * @param baseRevision revision of the text the edit was made on
   * @return false if the edit cannot be applied, because the edits since {@code baseRevision} are
   *     not known anymore (e.g. after a restart) or it does not fit the text
   */
  public static synchronized boolean apply(CollabText text, long authorGemId, int baseRevision, TextEdit edit) {
    List<TextEdit> history = histories.computeIfAbsent(text.getGemId(), gemId -> new ArrayList<>());
    int revision = text.getRevision();
    int missedEdits = revision - baseRevision;
    if (missedEdits < 0 || missedEdits > history.size()) {
      return false;
    }
    TextEdit rebased = edit;
    for (TextEdit applied : history.subList(history.size() - missedEdits, history.size())) {
      rebased = rebased.rebase(applied, true);
    }
    if (!rebased.fits(text.getLength())) {
      return false;
    }
    history.add(rebased);
    if (history.size() > HISTORY_SIZE) {
      history.remove(0);
    }
    int length = text.getLength() + rebased.getInserted().length() - rebased.getDeleteCount();
    // Clients that open the text read the chunks and then follow the edits, hence this order
    replaceInChunks(text, rebased);
    text.setLastEdit(CollabText.encodeLastEdit(revision + 1, authorGemId, length, rebased));
    removeCursorsOfDisconnectedUsers(text);
    return true;
  }

  private static void replaceInChunks(CollabText text, TextEdit edit) {
    List<Integer> lengths = new ArrayList<>();
    for (CollabTextChunk chunk : text.getChunksList()) {
      lengths.add(chunk.getContent().length());
    }
    ChunkChange change = ChunkChange.of(lengths, edit);
    int first = change.getFirst();
    int touchedCount = change.getTouchedCount();
    StringBuilder touched = new StringBuilder();
    for (int i = first; i < first + touchedCount; i++) {
      touched.append(text.getChunks(i).getContent());
    }
    String changed = new TextEdit(edit.getPosition() - change.getFirstStart(), edit.getDeleteCount(),
        edit.getInserted()).apply(touched.toString());

    List<Integer> newLengths = change.getNewLengths();
    int start = 0;
    for (int i = 0; i < newLengths.size(); i++) {
      String content = changed.substring(start, start + newLengths.get(i));
      start += newLengths.get(i);
      if (i >= touchedCount) {
        text.addChunks(first + i, CollabTextModuleManager.collabTextChunkBuilder().content(content).build().get());
      } else if (!text.getChunks(first + i).getContent().equals(content)) {
        text.getChunks(first + i).setContent(content);
      }
    }
    for (int i = touchedCount - 1; i >= newLengths.size(); i--) {
      CollabTextChunk removed = text.removeChunks(first + i);
      CollabTextModuleManager.deleteCollabTextChunk(removed.getGemId());
    }
  }

  private static void removeCursorsOfDisconnectedUsers(CollabText text) {
    for (CollabTextCursor cursor : new ArrayList<>(text.getCursorsList())) {
      // Participant 0 is the backend itself
      if (cursor.getParticipant() != 0
          && CommandManager.getInstance().getParticipant(cursor.getParticipant()) == null) {
        text.removeCursors(cursor);
        CollabTextModuleManager.deleteCollabTextCursor(cursor.getGemId());
      }
    }
  }
}
