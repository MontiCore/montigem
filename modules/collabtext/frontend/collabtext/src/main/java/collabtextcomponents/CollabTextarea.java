/* (c) https://github.com/MontiCore/monticore */
package collabtextcomponents;

import collabtextmodule.CollabText;
import collabtextmodule.CollabTextCursor;
import collabtextmodule.CollabTextModuleManager;
import umlp.backendrte.command.CommandManager;
import umlp.jsweet.extension.annotation.Component;

/** Creates and removes this editor's cursor. The text area works directly with the domain data. */
@Component
public class CollabTextarea extends CollabTextareaTOP {
  private boolean initialized = false;

  @Override
  protected void initOwnCursor() {
    setOwnCursor(null);
    super.initOwnCursor();
  }

  @Override
  public void init() {
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
    setOwnCursor(cursor);
    text.addCursors(cursor);
  }

  private void leave(CollabText text) {
    CollabTextCursor cursor = getOwnCursor();
    setOwnCursor(null);
    if (text != null && cursor != null) {
      text.removeCursors(cursor);
      CollabTextModuleManager.deleteCollabTextCursor(cursor.getGemId());
    }
  }

  private String defaultColor(long participant) {
    String colors = "#c2185b#1976d2#2e7d32#ef6c00#6a1b9a#00838f";
    int start = (int) (participant % 6) * 7;
    return colors.substring(start, start + 7);
  }

  @Override
  public Boolean isOtherUser(CollabTextCursor cursor) {
    return getOwnCursor() == null || cursor.getGemId() != getOwnCursor().getGemId();
  }
}
