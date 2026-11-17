/* (c) https://github.com/MontiCore/monticore */
package collabtextmodule;

import java.util.ArrayList;
import java.util.List;

public class CollabText extends CollabTextTOP {
  public static final int MAX_CHUNK_LENGTH = 10;

  /** Joins the current chunk contents. */
  public String getContent() {
    StringBuilder content = new StringBuilder();
    for (CollabTextChunk chunk : getChunksList()) {
      content.append(chunk.getContent());
    }
    return content.toString();
  }

  public int getLength() {
    int length = 0;
    for (CollabTextChunk chunk : getChunksList()) {
      length += chunk.getContent().length();
    }
    return length;
  }

  /** Updates the changed range of the chunks through generated setters and association mutations. */
  public void setContent(String content) {
    String previous = getContent();
    int common = Math.min(previous.length(), content.length());
    int prefix = 0;
    while (prefix < common && previous.charAt(prefix) == content.charAt(prefix)) {
      prefix++;
    }
    int suffix = 0;
    while (suffix < common - prefix
        && previous.charAt(previous.length() - 1 - suffix) == content.charAt(content.length() - 1 - suffix)) {
      suffix++;
    }
    if (prefix != previous.length() || prefix != content.length()) {
      replace(prefix, previous.length() - prefix - suffix, content.substring(prefix, content.length() - suffix));
    }
  }

  /** Replaces a range without constructing or synchronizing an edit object. */
  public boolean replace(int position, int deleteCount, String inserted) {
    List<Integer> lengths = new ArrayList<>();
    int length = 0;
    for (CollabTextChunk chunk : getChunksList()) {
      lengths.add(chunk.getContent().length());
      length += chunk.getContent().length();
    }
    if (position < 0 || deleteCount < 0 || position > length - deleteCount) {
      return false;
    }
    int first = 0;
    int firstStart = 0;
    while (first < lengths.size() - 1 && firstStart + lengths.get(first) < position) {
      firstStart += lengths.get(first);
      first++;
    }
    int touchedCount = 0;
    if (!lengths.isEmpty()) {
      int last = first;
      int touchedEnd = firstStart + lengths.get(first);
      while (last < lengths.size() - 1 && touchedEnd < position + deleteCount) {
        last++;
        touchedEnd += lengths.get(last);
      }
      touchedCount = last - first + 1;
    }
    StringBuilder touched = new StringBuilder();
    for (int i = first; i < first + touchedCount; i++) {
      touched.append(getChunks(i).getContent());
    }
    String previous = touched.toString();
    int localPosition = position - firstStart;
    String changed = previous.substring(0, localPosition) + inserted + previous.substring(localPosition + deleteCount);
    // Leave room in newly split chunks for subsequent typing.
    int pieceLength = changed.length() <= MAX_CHUNK_LENGTH ? MAX_CHUNK_LENGTH : MAX_CHUNK_LENGTH / 2;
    int newCount = 0;
    for (int start = 0; start < changed.length(); start += pieceLength) {
      String content = changed.substring(start, Math.min(changed.length(), start + pieceLength));
      if (newCount >= touchedCount) {
        addChunks(first + newCount, CollabTextModuleManager.collabTextChunkBuilder().content(content).build().get());
      } else if (!getChunks(first + newCount).getContent().equals(content)) {
        getChunks(first + newCount).setContent(content);
      }
      newCount++;
    }
    for (int i = touchedCount - 1; i >= newCount; i--) {
      CollabTextChunk removed = removeChunks(first + i);
      CollabTextModuleManager.deleteCollabTextChunk(removed.getGemId());
    }
    return true;
  }
}
