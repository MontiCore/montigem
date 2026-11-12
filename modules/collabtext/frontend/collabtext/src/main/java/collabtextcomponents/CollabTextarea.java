/* (c) https://github.com/MontiCore/monticore */
package collabtextcomponents;

import collabtextmodule.ChunkChange;
import collabtextmodule.CollabText;
import collabtextmodule.CollabTextChunk;
import collabtextmodule.CollabTextCursor;
import collabtextmodule.CollabTextModuleManager;
import collabtextmodule.TextEdit;
import collabtextmodule.commands.ApplyEdit;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.command.CommandResult;
import umlp.backendrte.common.NotificationScope;
import umlp.jsweet.extension.annotation.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sends the edits of the user to the backend one at a time. The backend publishes the edits of all
 * users in one order as {@code CollabText.lastEdit}. Until the own edit comes back from there, it
 * and the edits made in the meantime are kept rebased on the edits of the other users.
 * <p>
 * The chunks of the text are only read when it is opened. Afterwards, the published edits are
 * applied to a copy of the text.
 * <p>
 * The transpiled methods are asynchronous, another edit may be handled wherever one of them waits
 * for the backend. Hence, each handler first updates the state below and only then calls the backend.
 */
@Component
public class CollabTextarea extends CollabTextareaTOP {
  private boolean initialized = false;
  private CollabTextCursor ownCursor;
  // The text all users agree on after this number of edits
  private String confirmed = "";
  // Replaced as a whole, never changed
  private List<Integer> confirmedChunkLengths = new ArrayList<>();
  private int revision = 0;
  // Counts the attempts to read the text, an attempt is outdated when another one started
  private int loads = 0;
  // Counts the updates of the displayed chunks, only the most recent one is displayed
  private int chunkUpdates = 0;
  // The edit sent to the backend that has not been published yet
  private TextEdit inFlight;
  // The edits made since, in the order they were made
  private List<TextEdit> unsent = new ArrayList<>();

  @Override
  public void init() {
    // Set first: if the text is not there yet, the generated initialization fails at its end,
    // after it has set the defaults, and setText joins later
    initialized = true;
    super.init();
    join();
  }

  @Override
  public void destroy() {
    leave(getText());
    super.destroy();
  }

  @Override
  public void setText(CollabText text) {
    CollabText previous = getText();
    super.setText(text);
    if (initialized && previous != text) {
      leave(previous);
      if (text != null && !text.containsObserver(this)) {
        text.addObserver(this);
      }
      join();
    }
  }

  private void join() {
    CollabText text = getText();
    if (text == null) {
      return;
    }
    long participant = CommandManager.getParticipantId();
    CollabTextCursor cursor = CollabTextModuleManager.collabTextCursorBuilder()
        .userName(getUserName())
        .color(getUserColor().isEmpty() ? defaultColor(participant) : getUserColor())
        .participant(participant)
        .build().get();
    ownCursor = cursor;
    text.addCursors(cursor);
    load();
  }

  private void leave(CollabText text) {
    CollabTextCursor cursor = ownCursor;
    ownCursor = null;
    if (text != null && cursor != null) {
      text.removeObserver(this);
      text.removeCursors(cursor);
      CollabTextModuleManager.deleteCollabTextCursor(cursor.getGemId());
    }
  }

  private String defaultColor(long participant) {
    String colors = "#c2185b#1976d2#2e7d32#ef6c00#6a1b9a#00838f";
    int start = (int) (participant % 6) * 7;
    return colors.substring(start, start + 7);
  }

  /**
   * Reads the text from its chunks. Drops the edits of the user that are not published, they
   * cannot be rebased when edits of others were missed.
   */
  private void load() {
    loads++;
    int load = loads;
    CollabText text = getText();
    setLoading(true);
    inFlight = null;
    unsent = new ArrayList<>();
    int readRevision = text.getRevision();
    String content = text.getContent();
    List<Integer> chunkLengths = new ArrayList<>();
    int chunksLength = 0;
    for (CollabTextChunk chunk : text.getChunksList()) {
      chunkLengths.add(chunk.getContent().length());
      chunksLength += chunk.getContent().length();
    }
    // An edit that is applied while the chunks are read is published afterwards, which starts
    // the next attempt. The same holds if only a part of its chunks has arrived yet.
    if (load == loads && text == getText() && readRevision == text.getRevision()
        && content.length() == text.getLength() && chunksLength == content.length()) {
      confirmed = content;
      confirmedChunkLengths = chunkLengths;
      revision = readRevision;
      setValue(content);
      showChunkLengths();
      setLoading(false);
    }
  }

  @Override
  public Boolean isLocked(Boolean disabled, Boolean loading) {
    return disabled || loading;
  }

  @Override
  public String shownChunks(Boolean showChunks, String chunkLengths) {
    return showChunks ? chunkLengths : "";
  }

  @Override
  public Boolean isOtherUser(CollabTextCursor cursor) {
    return ownCursor == null || cursor.getGemId() != ownCursor.getGemId();
  }

  @Override
  public Consumer<String> applyEdit() {
    return encoded -> edit(TextEdit.decode(encoded));
  }

  private void edit(TextEdit edit) {
    if (ownCursor == null || isLoading() || !edit.fits(getValue().length())) {
      return;
    }
    setValue(edit.apply(getValue()));
    int last = unsent.size() - 1;
    if (last >= 0 && unsent.get(last).touches(edit)) {
      // E.g. typed characters, otherwise each of them had to wait for the previous one
      unsent.set(last, unsent.get(last).merge(edit));
    } else {
      unsent.add(edit);
    }
    boolean send = takeNextEdit();
    showChunkLengths();
    moveCursors(edit, ownCursor.getGemId());
    if (send) {
      sendEdit();
    }
  }

  private boolean takeNextEdit() {
    if (inFlight != null || unsent.isEmpty()) {
      return false;
    }
    inFlight = unsent.get(0);
    unsent.remove(0);
    return true;
  }

  private void sendEdit() {
    CollabText text = getText();
    CommandResult<Void> result = CommandManager.executeCommand(
        new ApplyEdit(text.getGemId(), ownCursor.getGemId(), revision, inFlight.encode()));
    if (!result.isSuccessful() && text == getText()) {
      load();
    }
  }

  @Override
  public void notifySetLastEdit(CollabText text, String oldValue, String lastEdit) {
    if (text != getText() || ownCursor == null) {
      return;
    }
    TextEdit published = text.getLastTextEdit();
    // Computed first: the state must not change between the checks and the update below
    List<Integer> previousChunkLengths = confirmedChunkLengths;
    List<Integer> chunkLengths = ChunkChange.of(previousChunkLengths, published).apply(previousChunkLengths);
    if (isLoading() || previousChunkLengths != confirmedChunkLengths || text.getRevision() != revision + 1
        || !published.fits(confirmed.length()) || confirmed.length() + published.getInserted().length() - published.getDeleteCount() != text.getLength()) {
      // Edits were missed, e.g. while the connection was interrupted
      load();
      return;
    }
    confirmed = published.apply(confirmed);
    confirmedChunkLengths = chunkLengths;
    revision++;
    long author = text.getLastEditAuthor();
    if (author == ownCursor.getGemId()) {
      inFlight = null;
      boolean send = takeNextEdit();
      showChunkLengths();
      if (send) {
        sendEdit();
      }
      return;
    }

    // The published edit as it applies to the displayed value, which contains the own edits already
    TextEdit remote = published;
    String value = confirmed;
    TextEdit rebasedInFlight = inFlight;
    if (inFlight != null) {
      rebasedInFlight = inFlight.rebase(remote, true);
      remote = remote.rebase(inFlight, false);
      if (!rebasedInFlight.fits(value.length())) {
        load();
        return;
      }
      value = rebasedInFlight.apply(value);
    }
    List<TextEdit> rebasedUnsent = new ArrayList<>();
    for (TextEdit edit : unsent) {
      TextEdit rebased = edit.rebase(remote, true);
      remote = remote.rebase(edit, false);
      if (!rebased.fits(value.length())) {
        load();
        return;
      }
      value = rebased.apply(value);
      rebasedUnsent.add(rebased);
    }
    inFlight = rebasedInFlight;
    unsent = rebasedUnsent;
    setValue(value);
    showChunkLengths();
    moveCursors(remote, author);
  }

  /** The chunks of the displayed value, which contains the edits that are not published yet. */
  private void showChunkLengths() {
    chunkUpdates++;
    int update = chunkUpdates;
    List<Integer> lengths = confirmedChunkLengths;
    if (inFlight != null) {
      lengths = ChunkChange.of(lengths, inFlight).apply(lengths);
    }
    for (TextEdit edit : unsent) {
      lengths = ChunkChange.of(lengths, edit).apply(lengths);
    }
    StringBuilder encoded = new StringBuilder();
    for (int i = 0; i < lengths.size(); i++) {
      if (i > 0) {
        encoded.append(",");
      }
      encoded.append(lengths.get(i));
    }
    if (update == chunkUpdates) {
      setChunkLengths(encoded.toString());
    }
  }

  /**
   * Only for this user, as all users move the cursors with the edits they see. The cursor of the
   * author is behind the inserted text afterwards.
   */
  private void moveCursors(TextEdit edit, long authorGemId) {
    for (CollabTextCursor cursor : getText().getCursorsList()) {
      boolean author = cursor.getGemId() == authorGemId;
      int anchor = author ? edit.insertedEnd() : edit.shift(cursor.getAnchor());
      int head = author ? edit.insertedEnd() : edit.shift(cursor.getHead());
      if (cursor.getAnchor() != anchor) {
        cursor.setAnchor(anchor, NotificationScope.LOCAL);
      }
      if (cursor.getHead() != head) {
        cursor.setHead(head, NotificationScope.LOCAL);
      }
    }
  }

  @Override
  public Consumer<String> moveCursor() {
    return selection -> {
      CollabTextCursor cursor = ownCursor;
      if (cursor != null) {
        int separator = selection.indexOf("|");
        cursor.setAnchor(Integer.parseInt(selection.substring(0, separator)));
        cursor.setHead(Integer.parseInt(selection.substring(separator + 1)));
      }
    };
  }
}
