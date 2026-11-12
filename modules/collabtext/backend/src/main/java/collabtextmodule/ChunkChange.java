/* (c) https://github.com/MontiCore/monticore */
package collabtextmodule;

import java.util.ArrayList;
import java.util.List;

/**
 * How an edit changes the chunks of a text: the chunks it touches are replaced by new ones. The
 * backend changes the chunks this way, and editors follow the lengths of the chunks with it,
 * without reading them again.
 */
public class ChunkChange {
  // Typing changes one chunk, which is stored and sent to all clients: small chunks keep that cheap
  public static final int MAX_CHUNK_LENGTH = 10;

  protected int first;
  // Position of the first touched chunk in the text
  protected int firstStart;
  protected int touchedCount;
  protected List<Integer> newLengths;

  public ChunkChange(int first, int firstStart, int touchedCount, List<Integer> newLengths) {
    this.first = first;
    this.firstStart = firstStart;
    this.touchedCount = touchedCount;
    this.newLengths = newLengths;
  }

  public static ChunkChange of(List<Integer> lengths, TextEdit edit) {
    int count = lengths.size();
    if (count == 0) {
      return new ChunkChange(0, 0, 0, split(edit.getInserted().length()));
    }
    // The chunks from first to last contain the replaced characters. An insertion between two
    // chunks goes to the end of the first one.
    int first = 0;
    int firstStart = 0;
    while (first < count - 1 && firstStart + lengths.get(first) < edit.getPosition()) {
      firstStart += lengths.get(first);
      first++;
    }
    int last = first;
    int touchedEnd = firstStart + lengths.get(first);
    while (last < count - 1 && touchedEnd < edit.end()) {
      last++;
      touchedEnd += lengths.get(last);
    }
    int changedLength = touchedEnd - firstStart + edit.getInserted().length() - edit.getDeleteCount();
    return new ChunkChange(first, firstStart, last - first + 1, split(changedLength));
  }

  private static List<Integer> split(int length) {
    // Half-full chunks, such that typing into one of them does not split it again at once
    int pieceLength = length <= MAX_CHUNK_LENGTH ? MAX_CHUNK_LENGTH : MAX_CHUNK_LENGTH / 2;
    List<Integer> lengths = new ArrayList<>();
    for (int start = 0; start < length; start += pieceLength) {
      lengths.add(Math.min(pieceLength, length - start));
    }
    return lengths;
  }

  public int getFirst() {
    return first;
  }

  public int getFirstStart() {
    return firstStart;
  }

  public int getTouchedCount() {
    return touchedCount;
  }

  public List<Integer> getNewLengths() {
    return newLengths;
  }

  /** The lengths of the chunks after the change. */
  public List<Integer> apply(List<Integer> lengths) {
    List<Integer> changed = new ArrayList<>();
    for (int i = 0; i < first; i++) {
      changed.add(lengths.get(i));
    }
    changed.addAll(newLengths);
    for (int i = first + touchedCount; i < lengths.size(); i++) {
      changed.add(lengths.get(i));
    }
    return changed;
  }
}
