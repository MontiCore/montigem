/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.db.sql;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shortens SQL identifiers deterministically to PostgreSQL's limit of 63 bytes (NAMEDATALEN - 1).
 * <p>
 * PostgreSQL does not reject overlong identifiers, it silently truncates them to 63 bytes. Two names
 * that only differ beyond that point therefore collapse onto the same table. This class shortens such
 * names itself and appends a hash over the complete original name, which keeps them distinct.
 * <p>
 * Below the limit every method is the identity, so existing schemas are unaffected.
 */
public final class SQLNames {

  public static final int MAX_BYTES = 63;

  private static final int HASH_LENGTH = 8;

  private static final int MIN_SEGMENT_LENGTH = 3;

  private static final String SEPARATOR = "_";

  /** Splits on both separators used in generated identifiers: {@code _} and the list table marker {@code $}. */
  private static final String SEGMENT_PATTERN = "[_$]+";

  private SQLNames() {
  }

  public static String shorten(String identifier) {
    return shorten("", identifier, "");
  }

  /**
   * Shortens {@code prefix + body + suffix} to at most {@link #MAX_BYTES} bytes. Prefix and suffix are
   * preserved verbatim, only the body is compressed. The hash is inserted between body and suffix so
   * that a trailing {@code _id} stays in place.
   */
  public static String shorten(String prefix, String body, String suffix) {
    String full = prefix + body + suffix;
    if (utf8Length(full) <= MAX_BYTES) {
      return full;
    }

    String hash = SEPARATOR + hash(full);
    int budget = MAX_BYTES - utf8Length(prefix) - utf8Length(suffix) - utf8Length(hash);
    if (budget < MIN_SEGMENT_LENGTH) {
      return hardCut(prefix + body, hash, suffix);
    }

    List<String> segments = new ArrayList<>(Arrays.asList(body.split(SEGMENT_PATTERN)));
    segments.removeIf(String::isEmpty);

    collapseRepeatedBlocks(segments);
    if (!fits(segments, budget)) {
      truncateFairly(segments, budget);
    }
    if (!fits(segments, budget)) {
      dropFromMiddle(segments, budget);
    }

    return prefix + String.join(SEPARATOR, segments) + hash + suffix;
  }

  /**
   * Shortens a body that the caller appends {@code suffix} to afterwards. Returns the shortened body
   * without the suffix, but reserves room for it.
   */
  public static String shortenReserving(String body, String suffix) {
    String shortened = shorten("", body, suffix);
    return shortened.substring(0, shortened.length() - suffix.length());
  }

  /** The name PostgreSQL has silently truncated this identifier to so far. Used to generate renames. */
  public static String legacyTruncation(String identifier) {
    String truncated = identifier;
    while (utf8Length(truncated) > MAX_BYTES) {
      truncated = truncated.substring(0, truncated.length() - 1);
    }
    return truncated;
  }

  /**
   * Collapses an adjacent repetition of a block of segments, longest block first. Derived association
   * names repeat the class name as the role name when no role is given, which is where most of the
   * overlength comes from.
   */
  private static void collapseRepeatedBlocks(List<String> segments) {
    for (int length = segments.size() / 2; length >= 1; length--) {
      for (int i = 0; i + 2 * length <= segments.size(); i++) {
        if (segments.subList(i, i + length).equals(segments.subList(i + length, i + 2 * length))) {
          segments.subList(i + length, i + 2 * length).clear();
          // deliberately no second pass at the same position: for names sharing a prefix
          // (joint_connection / joint_connection_kind) that would swallow a real name part
        }
      }
    }
  }

  /**
   * Caps every segment at the largest common length that fits the budget, so short segments survive
   * intact and only the long ones are cut.
   */
  private static void truncateFairly(List<String> segments, int budget) {
    int available = budget - (segments.size() - 1);
    int cap = MIN_SEGMENT_LENGTH;
    for (int candidate = MIN_SEGMENT_LENGTH; candidate <= MAX_BYTES; candidate++) {
      int used = 0;
      for (String segment : segments) {
        used += Math.min(segment.length(), candidate);
      }
      if (used > available) {
        break;
      }
      cap = candidate;
    }
    for (int i = 0; i < segments.size(); i++) {
      if (segments.get(i).length() > cap) {
        segments.set(i, segments.get(i).substring(0, cap));
      }
    }
  }

  /** Last resort: the first and last segments carry the most meaning, so drop from the middle. */
  private static void dropFromMiddle(List<String> segments, int budget) {
    while (segments.size() > 2 && !fits(segments, budget)) {
      segments.remove(segments.size() / 2);
    }
    while (!segments.isEmpty() && !fits(segments, budget)) {
      String last = segments.get(segments.size() - 1);
      if (last.length() > 1) {
        segments.set(segments.size() - 1, last.substring(0, last.length() - 1));
      }
      else {
        segments.remove(segments.size() - 1);
      }
    }
  }

  private static boolean fits(List<String> segments, int budget) {
    return utf8Length(String.join(SEPARATOR, segments)) <= budget;
  }

  /** Fallback for affixes that leave no room for segments. Keeps the suffix, so the result still ends with it. */
  private static String hardCut(String head, String hash, String suffix) {
    String cut = head;
    while (!cut.isEmpty() && utf8Length(cut + hash + suffix) > MAX_BYTES) {
      cut = cut.substring(0, cut.length() - 1);
    }
    return cut + hash + suffix;
  }

  private static int utf8Length(String value) {
    return value.getBytes(StandardCharsets.UTF_8).length;
  }

  private static String hash(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (int i = 0; hex.length() < HASH_LENGTH; i++) {
        hex.append(String.format("%02x", digest[i]));
      }
      return hex.substring(0, HASH_LENGTH);
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required to be available on every Java platform", e);
    }
  }
}
