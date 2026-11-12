/* (c) https://github.com/MontiCore/monticore */
package collabtextmodule;

public class CollabText extends CollabTextTOP {

  public static String encodeLastEdit(int revision, long authorGemId, int length, TextEdit edit) {
    return revision + "|" + authorGemId + "|" + length + "|" + edit.encode();
  }

  /**
   * The whole text. In the frontend, this loads all chunks that are not loaded yet. To follow
   * the changes of the text, apply the edits published as {@code lastEdit} instead of calling it again.
   */
  public String getContent() {
    CollabTextModuleManager.loadAllCollabTextChunkWithIDs(_internal_getChunksGemList().getIDs());
    StringBuilder content = new StringBuilder();
    for (CollabTextChunk chunk : getChunksList()) {
      content.append(chunk.getContent());
    }
    return content.toString();
  }

  /** Number of edits applied to the text so far. */
  public int getRevision() {
    if (getLastEdit().isEmpty()) {
      return 0;
    }
    return Integer.parseInt(getLastEdit().substring(0, separator(1)));
  }

  /** GemId of the cursor of the user who made the last edit, 0 if the backend made it itself. */
  public long getLastEditAuthor() {
    return Long.parseLong(getLastEdit().substring(separator(1) + 1, separator(2)));
  }

  /** Length of the text, without reading its chunks. */
  public int getLength() {
    if (getLastEdit().isEmpty()) {
      return 0;
    }
    return Integer.parseInt(getLastEdit().substring(separator(2) + 1, separator(3)));
  }

  public TextEdit getLastTextEdit() {
    return TextEdit.decode(getLastEdit().substring(separator(3) + 1));
  }

  // Index of the given separator in lastEdit, the edit behind the third one may contain more
  private int separator(int number) {
    int index = -1;
    for (int i = 0; i < number; i++) {
      index = getLastEdit().indexOf("|", index + 1);
    }
    return index;
  }
}
