/* (c) https://github.com/MontiCore/monticore */
package chatmodule;

import java.util.Objects;
import jsweet.lang.Erased;

/** Backend extension point for the application's model provider and context-specific tools. */
@Erased
public final class MessageSender {
  private static volatile Handler handler;

  private MessageSender() {}

  @FunctionalInterface
  public interface Handler {
    void send(Message message, MessageHistory history, long contextGemId);
  }

  /** Register once during application startup, before accepting chat commands. */
  public static void setHandler(Handler messageHandler) {
    handler = Objects.requireNonNull(messageHandler);
  }

  public static void send(Message message, MessageHistory history, long contextGemId) {
    Handler current = handler;
    if (current == null) {
      throw new IllegalStateException("Register a MessageSender handler before sending chat messages");
    }
    current.send(message, history, contextGemId);
  }
}
