package aas.decorator.utils;

public class StringUtils {

  public static String capitalize(String word) {
    if (word == null || word.isEmpty()) {
      return word;
    }
    return word.substring(0, 1).toUpperCase() + word.substring(1);
  }

  public static String uncapitalize(String word) {
    if (word == null || word.isEmpty()) {
      return word;
    }
    return word.substring(0, 1).toLowerCase() + word.substring(1);
  }

}
