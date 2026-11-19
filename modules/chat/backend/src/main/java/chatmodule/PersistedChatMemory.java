package chatmodule;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import jsweet.lang.Erased;
import umlp.backendrte.common.NotificationScope;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Maps the provider's text/tool conversation to the chat module's persisted records. */
@Erased
public final class PersistedChatMemory implements ChatMemory {
  private final MessageHistory chat;
  private final List<ChatMessage> messages = new ArrayList<>();
  private ModelMessage systemMessage;
  private int nextSequence = 1;
  int historySize;

  public PersistedChatMemory(MessageHistory chat) {
    this.chat = chat;
    historySize = chat.getSynchronizedMessageCount();
    if (historySize < 0) {
      throw new IllegalStateException("Invalid synchronized message count");
    }
    for (ModelMessage message : chat.getModelMessagesList().stream()
        .sorted(Comparator.comparingInt(ModelMessage::getSequence)).toList()) {
      messages.add(restore(message));
      if (message.getRole() == ModelMessageRole.SYSTEM) {
        systemMessage = message;
      }
      nextSequence = Math.max(nextSequence, message.getSequence() + 1);
    }
  }

  @Override
  public Object id() {
    return chat.getGemId();
  }

  @Override
  public void add(ChatMessage message) {
    if (message instanceof SystemMessage system) {
      if (systemMessage == null) {
        systemMessage = ChatModuleManager.modelMessageBuilder()
            .sequence(0).role(ModelMessageRole.SYSTEM).content(Optional.of(system.text())).build().orElseThrow();
        chat.addModelMessages(systemMessage, NotificationScope.LOCAL);
        messages.add(0, system);
      } else {
        systemMessage.setContent(Optional.of(system.text()), NotificationScope.LOCAL);
        messages.set(0, system);
      }
    } else {
      ModelMessage stored = store(message);
      chat.addModelMessages(stored, NotificationScope.LOCAL);
      messages.add(message);
    }
    saveHistoryPosition();
  }

  private ModelMessage store(ChatMessage message) {
    var builder = ChatModuleManager.modelMessageBuilder().sequence(nextSequence++);
    if (message instanceof UserMessage user) {
      builder.role(ModelMessageRole.USER).content(Optional.of(user.singleText()));
    } else if (message instanceof AiMessage assistant) {
      builder.role(ModelMessageRole.ASSISTANT).content(Optional.ofNullable(assistant.text()))
          .thinking(Optional.ofNullable(assistant.thinking()));
      for (int i = 0; i < assistant.toolExecutionRequests().size(); i++) {
        ToolExecutionRequest call = assistant.toolExecutionRequests().get(i);
        builder.addToolCalls(ChatModuleManager.toolCallBuilder().sequence(i)
            .callId(Optional.ofNullable(call.id())).name(call.name())
            .arguments(Optional.ofNullable(call.arguments())).build().orElseThrow());
      }
    } else if (message instanceof ToolExecutionResultMessage result) {
      builder.role(ModelMessageRole.TOOL).toolResult(ChatModuleManager.toolResultBuilder()
          .callId(Optional.ofNullable(result.id())).name(result.toolName()).content(result.text())
          .error(Optional.ofNullable(result.isError())).build().orElseThrow());
    } else {
      throw new IllegalArgumentException("Unsupported conversation message: " + message.type());
    }
    return builder.build().orElseThrow();
  }

  private static ChatMessage restore(ModelMessage message) {
    return switch (message.getRole()) {
      case SYSTEM -> SystemMessage.from(message.getContent().orElseThrow());
      case USER -> UserMessage.from(message.getContent().orElseThrow());
      case ASSISTANT -> AiMessage.builder().text(message.getContent().orElse(null))
          .thinking(message.getThinking().orElse(null))
          .toolExecutionRequests(message.getToolCallsList().stream()
              .sorted(Comparator.comparingInt(ToolCall::getSequence))
              .map(call -> ToolExecutionRequest.builder().id(call.getCallId().orElse(null))
                  .name(call.getName()).arguments(call.getArguments().orElse(null)).build()).toList())
          .build();
      case TOOL -> {
        if (!message.isPresentToolResult()) {
          throw new IllegalStateException("Tool message has no result");
        }
        var result = message.getToolResult();
        yield ToolExecutionResultMessage.builder().id(result.getCallId().orElse(null))
            .toolName(result.getName()).text(result.getContent()).isError(result.getError().orElse(null)).build();
      }
    };
  }

  @Override
  public List<ChatMessage> messages() {
    return List.copyOf(messages);
  }

  @Override
  public void clear() {
    // Association removal alone does not delete records from the database.
    for (ModelMessage message : List.copyOf(chat.getModelMessagesList())) {
      for (ToolCall call : List.copyOf(message.getToolCallsList())) {
        message.removeToolCalls(call, NotificationScope.LOCAL);
        ChatModuleManager.deleteToolCall(call.getGemId(), NotificationScope.LOCAL);
      }
      if (message.isPresentToolResult()) {
        long resultId = message.getToolResult().getGemId();
        message.setToolResultAbsent(NotificationScope.LOCAL);
        ChatModuleManager.deleteToolResult(resultId, NotificationScope.LOCAL);
      }
      chat.removeModelMessages(message, NotificationScope.LOCAL);
      ChatModuleManager.deleteModelMessage(message.getGemId(), NotificationScope.LOCAL);
    }
    messages.clear();
    systemMessage = null;
    nextSequence = 1;
    historySize = 0;
    saveHistoryPosition();
  }

  void saveHistoryPosition() {
    chat.setSynchronizedMessageCount(historySize, NotificationScope.LOCAL);
  }
}
