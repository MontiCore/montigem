package chatmodule;

import java.time.LocalDateTime;

public class Message extends MessageTOP {

  public static String getCurrentTime() {
    LocalDateTime now = LocalDateTime.now();
    return formatTime(String.valueOf(now.getHour())) + ":" + formatTime(String.valueOf(now.getMinute()))
            + ":" + formatTime(String.valueOf(now.getSecond()));
  }

  /**
   * AI replies are created by the backend without a timestamp. Call this in the frontend when a reply
   * arrives, so it gets the arrival time in the browser's local time zone.
   */
  public static void setArrivalTimeIfMissing(Message msg) {
    if (!msg.isUser() && msg.getTimestamp().isEmpty()) {
      msg.setTimestamp(getCurrentTime());
    }
  }

  private static String formatTime(String time) {
    if (time.length() == 1) {
      return "0" + time;
    }
    return time;
  }

  public static boolean hasLetterOrDigit(String input) {
    if (input == null) return false;
    for (int i = 0; i < input.length(); i++) {
      if (Character.isLetterOrDigit(input.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  public static boolean lastMessageIsFromUser(MessageHistory chat) {
    return chat.getMessagesList().get(chat.getMessagesList().size()-1).isUser();
  }
}
