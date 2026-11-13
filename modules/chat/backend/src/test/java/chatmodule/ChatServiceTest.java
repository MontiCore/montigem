package chatmodule;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the reusable service without an application domain or model-provider implementation. */
class ChatServiceTest {
  @BeforeEach
  void setUp() {
    ChatModuleMock.init();
  }

  @Test
  void restoresToolTranscriptAndUsesTheToolsSuppliedForEachRequest() {
    List<ChatRequest> requests = new ArrayList<>();
    ChatModel model = new ChatModel() {
      @Override
      public ChatResponse chat(ChatRequest request) {
        requests.add(request);
        int turn = requests.size();
        AiMessage reply = turn % 2 == 1
            ? AiMessage.from(ToolExecutionRequest.builder().id("call-" + turn)
                .name("getContext").arguments("{}").build())
            : AiMessage.from("Reply " + turn);
        return ChatResponse.builder().aiMessage(reply).build();
      }
    };
    MessageHistory history = history("First question");
    new ChatService(model, "Application instructions").send(history, new ContextTools("first"));
    history.addMessages(ChatModuleManager.messageBuilder()
        .content("Second question").user(true).timestamp("").build().get());

    new ChatService(model, "Application instructions").send(history, new ContextTools("second"));

    assertEquals(4, requests.size());
    assertEquals(SystemMessage.from("Application instructions"), requests.get(0).messages().get(0));
    assertEquals("first", ((ToolExecutionResultMessage) requests.get(1).messages().get(3)).text());
    List<dev.langchain4j.data.message.ChatMessage> restored = requests.get(2).messages();
    assertEquals(requests.get(1).messages(), restored.subList(0, 4));
    assertEquals(AiMessage.from("Reply 2"), restored.get(4));
    assertEquals(UserMessage.from("Second question"), restored.get(5));
    var lastMessages = requests.get(3).messages();
    assertEquals("second", ((ToolExecutionResultMessage) lastMessages.get(lastMessages.size() - 1)).text());
    assertEquals("Reply 4", history.getMessages(3).getContent());
    assertEquals(4, history.getSynchronizedMessageCount());
  }

  @Test
  void supportsConversationsWithoutTools() {
    ChatModel model = new ChatModel() {
      @Override
      public ChatResponse chat(ChatRequest request) {
        assertEquals(List.of(SystemMessage.from("Instructions"), UserMessage.from("Hello")), request.messages());
        return ChatResponse.builder().aiMessage(AiMessage.from("Hello back")).build();
      }
    };
    MessageHistory history = history("Hello");

    new ChatService(model, "Instructions").send(history);

    assertEquals("Hello back", history.getMessages(1).getContent());
    assertFalse(history.getMessages(1).isUser());
    assertEquals(new PersistedChatMemory(history).messages().size(), history.getModelMessagesList().size());
  }

  @Test
  void appendsVisibleErrorWhenTheSuppliedModelFails() {
    ChatModel model = new ChatModel() {
      @Override
      public ChatResponse chat(ChatRequest request) {
        throw new IllegalStateException("Unavailable");
      }
    };
    MessageHistory history = history("Hello");

    new ChatService(model, "Instructions").send(history);

    assertEquals("Serverfehler. Bitte erneut versuchen.", history.getMessages(1).getContent());
    assertEquals(2, history.getSynchronizedMessageCount());
  }

  private MessageHistory history(String question) {
    MessageHistory history = ChatModuleManager.messageHistoryBuilder().name("Chat").build().get();
    history.addMessages(ChatModuleManager.messageBuilder()
        .content(question).user(true).timestamp("").build().get());
    return history;
  }

  public static class ContextTools {
    private final String context;

    ContextTools(String context) {
      this.context = context;
    }

    @Tool("Returns the current application context")
    public String getContext() {
      return context;
    }
  }
}
