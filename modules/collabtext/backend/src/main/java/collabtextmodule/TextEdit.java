/* (c) https://github.com/MontiCore/monticore */
package collabtextmodule;

/**
 * Replaces {@code deleteCount} characters at {@code position} of a text by {@code inserted}.
 * Used by the backend and, transpiled, by the frontend, so both adapt concurrent edits the same way.
 */
public class TextEdit {
  protected int position;
  protected int deleteCount;
  protected String inserted;

  public TextEdit(int position, int deleteCount, String inserted) {
    this.position = position;
    this.deleteCount = deleteCount;
    this.inserted = inserted;
  }

  public int getPosition() {
    return position;
  }

  public int getDeleteCount() {
    return deleteCount;
  }

  public String getInserted() {
    return inserted;
  }

  public int end() {
    return position + deleteCount;
  }

  public int insertedEnd() {
    return position + inserted.length();
  }

  /** The inverse of {@link #decode}: {@code <position>|<deleteCount>|<inserted>}. */
  public String encode() {
    return position + "|" + deleteCount + "|" + inserted;
  }

  public static TextEdit decode(String encoded) {
    int firstSeparator = encoded.indexOf("|");
    int secondSeparator = encoded.indexOf("|", firstSeparator + 1);
    return new TextEdit(Integer.parseInt(encoded.substring(0, firstSeparator)),
        Integer.parseInt(encoded.substring(firstSeparator + 1, secondSeparator)),
        encoded.substring(secondSeparator + 1));
  }

  public boolean fits(int textLength) {
    return position >= 0 && deleteCount >= 0 && end() <= textLength;
  }

  public String apply(String text) {
    return text.substring(0, position) + inserted + text.substring(end());
  }

  /**
   * This edit and {@code other} were made on the same text. Returns this edit for the text on which
   * {@code other} is already applied. Rebasing two edits on each other leads to the same text in
   * both orders, if exactly one of them is rebased with {@code otherInsertsFirst}, which decides
   * whose insertion comes first when both insert at the same place.
   * <p>
   * An edit within the text that the other one replaces is dropped, the surrounding edit wins.
   */
  public TextEdit rebase(TextEdit other, boolean otherInsertsFirst) {
    int otherGrowth = other.inserted.length() - other.deleteCount;
    boolean endsBeforeOther = end() <= other.position;
    boolean startsAfterOther = other.end() <= position;
    if (endsBeforeOther && startsAfterOther) {
      return new TextEdit(otherInsertsFirst ? position + otherGrowth : position, deleteCount, inserted);
    }
    if (startsAfterOther) {
      return new TextEdit(position + otherGrowth, deleteCount, inserted);
    }
    if (endsBeforeOther) {
      return this;
    }
    boolean withinOther = other.position <= position && end() <= other.end();
    boolean aroundOther = position <= other.position && other.end() <= end();
    if (withinOther && aroundOther) {
      // Both replace the same characters, which are gone already, the insertions are kept
      return new TextEdit(otherInsertsFirst ? other.insertedEnd() : other.position, 0, inserted);
    }
    if (withinOther) {
      return new TextEdit(other.position, 0, "");
    }
    if (aroundOther) {
      return new TextEdit(position, deleteCount + otherGrowth, inserted);
    }
    if (position < other.position) {
      return new TextEdit(position, other.position - position, inserted);
    }
    return new TextEdit(other.insertedEnd(), end() - other.end(), inserted);
  }

  /** Whether {@code next}, made on the result of this edit, changes or adjoins the text this edit inserted. */
  public boolean touches(TextEdit next) {
    return next.position <= insertedEnd() && next.end() >= position;
  }

  /** One edit with the effect of this edit followed by {@code next}, which {@link #touches} it. */
  public TextEdit merge(TextEdit next) {
    int deletedBefore = Math.max(0, position - next.position);
    int deletedAfter = Math.max(0, next.end() - insertedEnd());
    String keptStart = inserted.substring(0, Math.max(0, next.position - position));
    String keptEnd = inserted.substring(Math.min(inserted.length(), next.end() - position));
    return new TextEdit(Math.min(position, next.position), deletedBefore + deleteCount + deletedAfter,
        keptStart + next.inserted + keptEnd);
  }

  /** Where the character at {@code index}, or a cursor in front of it, is after this edit. */
  public int shift(int index) {
    if (index <= position) {
      return index;
    }
    if (index >= end()) {
      return index + inserted.length() - deleteCount;
    }
    return insertedEnd();
  }
}
