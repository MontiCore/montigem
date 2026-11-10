package chatmodule;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolExecution;
import jsweet.lang.Erased;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Retains the complete model conversation, including tool exchanges, for each chat. */
@Erased
public class ChatService {
  private static final Logger LOG = Logger.getLogger(ChatService.class.getName());
  private final ChatModel model;
  private final String systemMessage;

  /** The application supplies its model and the already composed system instructions. */
  public ChatService(ChatModel model, String systemMessage) {
    this.model = Objects.requireNonNull(model);
    this.systemMessage = Objects.requireNonNull(systemMessage);
  }

  /** Tools are scoped to this request, allowing the application to supply its current context. */
  public void send(MessageHistory chat, Object... tools) {
    synchronized (chat) {
      try {
        send(chat, new PersistedChatMemory(chat), tools);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Chat conversation persistence failed", e);
        addReply(chat, "Serverfehler. Bitte erneut versuchen.");
      }
    }
  }

  private void send(MessageHistory chat, PersistedChatMemory memory, Object... tools) {
    try {
      List<ChatMessage> messages = new ArrayList<>();
      for (Message message : chat.getMessagesList()) {
        messages.add(message.isUser()
            ? UserMessage.from(message.getContent())
            : AiMessage.from(message.getContent()));
      }
      if (messages.isEmpty() || !(messages.get(messages.size() - 1) instanceof UserMessage userMessage)) {
        throw new IllegalStateException("The conversation must end with a user message");
      }
      System.out.println("[ChatService] User message: " + userMessage.singleText());
      // Import only unseen visible history; AiServices appends the current user message
      // and all subsequent assistant/tool messages to this same unbounded memory.
      for (int i = memory.historySize; i < messages.size() - 1; i++) {
        memory.historySize = i + 1;
        memory.add(messages.get(i));
      }
      memory.historySize = messages.size();
      ChatAssistant assistant = AiServices.builder(ChatAssistant.class)
          .chatModel(model)
          .chatMemory(memory)
          .systemMessageProvider(context -> systemMessage)
          .tools(tools)
          .beforeToolExecution(execution ->
              System.out.println("[ChatService] Tool call: " + execution.request()))
          .afterToolExecution(execution ->
              System.out.println("[ChatService] Tool result (" + execution.request().name() + "): "
                  + execution.result()))
          .build();
      Result<String> result = assistant.chat(userMessage.singleText());
      String answer = result.content();
      if (answer == null && !result.toolExecutions().isEmpty()) {
        List<String> toolResults = new ArrayList<>();
        for (ToolExecution execution : result.toolExecutions()) {
          toolResults.add(execution.result());
        }
        answer = String.join("\n\n", toolResults);
      }
      if (answer == null || answer.isBlank()) {
        throw new IllegalStateException("The chat model returned no text");
      }
      if (result.content() == null) {
        memory.add(AiMessage.from(answer));
      }
      addReply(chat, answer);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Chat model request failed", e);
      String error = "Serverfehler. Bitte erneut versuchen.";
      memory.add(AiMessage.from(error));
      addReply(chat, error);
    } finally {
      memory.historySize = chat.getMessagesList().size();
      memory.saveHistoryPosition();
    }
  }

  private void addReply(MessageHistory chat, String content) {
    System.out.println("[ChatService] Assistant message: " + content);
    Message reply = ChatModuleManager.messageBuilder()
        .timestamp("") // set by the browser on arrival
        .user(false)
        .content(content)
        .build().get();
    chat.addMessages(reply);
  }
}
