package dashboard;

import de.monticore.symboltable.serialization.JsonParser;
import de.monticore.symboltable.serialization.json.JsonObject;

import chatmodule.ChatModuleManager;
import chatmodule.Message;
import chatmodule.MessageHistory;
import com.sun.net.httpserver.HttpServer;
import dashboard.test_infrastructure.DashboardMockTestSetup;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import umlp.test_rte.ClientSetup;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatServiceTest extends DashboardMockTestSetup {
  private ChatModel model;
  private ChatService service;
  private MessageHistory history;

  ChatServiceTest() {
    super(ClientSetup.of(0));
  }

  @Override
  protected void initBackendManager() {
    super.initBackendManager();
    chatmodule.ChatModuleMock.init();
    dashboardmodule.DashboardModuleMock.init();
  }

  @BeforeEach
  void setUp() {
    getBackend().activate();
    model = mock(ChatModel.class);
    service = new ChatService(model);
    history = ChatModuleManager.messageHistoryBuilder().name("Dashboard chat").build().get();
    history.addMessages(ChatModuleManager.messageBuilder()
        .content("Show the teams").timestamp("12:00:00").user(true).build().get());
  }

  @Test
  void sendsConversationInOrderWithRolesAndAppendsMarkdownReply() {
    history.addMessages(ChatModuleManager.messageBuilder()
        .content("Two teams").timestamp("12:00:01").user(false).build().get());
    history.addMessages(ChatModuleManager.messageBuilder()
        .content("Who leads them?").timestamp("12:00:02").user(true).build().get());
    when(model.chat(any(ChatRequest.class))).thenReturn(response("**Team leaders**"));

    service.send(history);

    ArgumentCaptor<ChatRequest> request = ArgumentCaptor.forClass(ChatRequest.class);
    verify(model).chat(request.capture());
    assertEquals(List.of(UserMessage.from("Show the teams"), AiMessage.from("Two teams"),
        UserMessage.from("Who leads them?")), request.getValue().messages().subList(1, 4));
    assertInstanceOf(SystemMessage.class, request.getValue().messages().get(0));
    assertEquals(12, request.getValue().parameters().toolSpecifications().size());
    String instructions = ((SystemMessage) request.getValue().messages().get(0)).text();
    assertTrue(instructions.contains("# Dashboard assistant"));
    assertTrue(instructions.contains("# Dashboard operations"));
    assertTrue(instructions.contains("# Dashboard chart data interpreter models"));
    assertFalse(instructions.contains("DashboardManager"));
    assertTrue(request.getValue().parameters().toolSpecifications().stream()
        .anyMatch(tool -> tool.name().equals("getDomainModel")));
    assertTrue(request.getValue().parameters().toolSpecifications().stream()
        .anyMatch(tool -> tool.name().equals("checkInterpreterCode")));
    assertTrue(request.getValue().parameters().toolSpecifications().stream()
        .anyMatch(tool -> tool.name().equals("createCell")));
    assertTrue(request.getValue().parameters().toolSpecifications().stream()
        .anyMatch(tool -> tool.name().equals("setViewInfo")));
    assertEquals(4, history.getMessagesList().size());
    Message reply = history.getMessages(3);
    assertFalse(reply.isUser());
    assertEquals("**Team leaders**", reply.getContent());
    assertTrue(reply.getSourcesList().isEmpty());
  }

  @Test
  void doesNotMixConversationsBetweenRequests() {
    when(model.chat(any(ChatRequest.class))).thenReturn(response("Reply"));
    service.send(history);
    MessageHistory anotherChat = ChatModuleManager.messageHistoryBuilder().name("Other chat").build().get();
    anotherChat.addMessages(ChatModuleManager.messageBuilder()
        .content("Separate question").timestamp("12:00:00").user(true).build().get());

    service.send(anotherChat);

    ArgumentCaptor<ChatRequest> requests = ArgumentCaptor.forClass(ChatRequest.class);
    verify(model, times(2)).chat(requests.capture());
    assertEquals(List.of(UserMessage.from("Separate question")),
        requests.getAllValues().get(1).messages().subList(1, 2));
    assertEquals(2, history.getMessagesList().size());
    assertEquals(2, anotherChat.getMessagesList().size());
  }

  @Test
  void showsErrorWhenModelFails() {
    when(model.chat(any(ChatRequest.class))).thenThrow(new IllegalStateException("Model unavailable"));
    service.send(history);
    assertErrorReply();
  }

  @Test
  void showsErrorWhenResponseIsMissing() {
    when(model.chat(any(ChatRequest.class))).thenReturn(null);
    service.send(history);
    assertErrorReply();
  }

  @Test
  void showsErrorWhenReplyIsBlank() {
    when(model.chat(any(ChatRequest.class))).thenReturn(response("   "));
    service.send(history);
    assertErrorReply();
  }

  @Test
  void retainsToolCallsAndResultsAcrossRequestsWithoutDuplicatingVisibleMessages() {
    dashboardmodule.ViewConfig view = dashboardmodule.DashboardModuleManager.viewConfigBuilder()
        .vid(1).name("Overview").info("").build().get();
    when(model.chat(any(ChatRequest.class))).thenReturn(ChatResponse.builder()
        .aiMessage(AiMessage.from(toolCall("getCurrentLayout"))).build(),
        response("The Overview view is empty."), response("You are welcome."));
    service.send(history, view);
    assertEquals("The Overview view is empty.", history.getMessages(1).getContent());
    history.addMessages(ChatModuleManager.messageBuilder()
        .content("Thank you").timestamp("12:00:02").user(true).build().get());

    new ChatService(model).send(history, view);

    ArgumentCaptor<ChatRequest> requests = ArgumentCaptor.forClass(ChatRequest.class);
    verify(model, times(3)).chat(requests.capture());
    List<ChatMessage> expected = new ArrayList<>(requests.getAllValues().get(1).messages());
    expected.add(AiMessage.from("The Overview view is empty."));
    expected.add(UserMessage.from("Thank you"));
    assertEquals(expected, requests.getAllValues().get(2).messages());
    assertEquals(AiMessage.from(toolCall("getCurrentLayout")), expected.get(2));
    assertInstanceOf(ToolExecutionResultMessage.class, expected.get(3));
    assertEquals(4, history.getMessagesList().size());
    assertEquals("You are welcome.", history.getMessages(3).getContent());
  }

  @Test
  void retainsEveryExchangeBeyondTheOldWindowAndKeepsChatsSeparate() {
    AtomicInteger invocation = new AtomicInteger();
    when(model.chat(any(ChatRequest.class))).thenAnswer(ignored -> {
      int index = invocation.getAndIncrement();
      if (index < 8) {
        return ChatResponse.builder().aiMessage(AiMessage.from(ToolExecutionRequest.builder()
            .id("call-" + index).name(index == 0 ? "getDomainModel" : "getCurrentLayout")
            .arguments("{}").build())).build();
      }
      return response("Done");
    });
    service.send(history);
    history.addMessages(ChatModuleManager.messageBuilder()
        .content("Keep using that schema").timestamp("12:00:02").user(true).build().get());
    service.send(history);

    MessageHistory anotherChat = ChatModuleManager.messageHistoryBuilder().name("Other chat").build().get();
    anotherChat.addMessages(ChatModuleManager.messageBuilder()
        .content("Separate question").timestamp("12:00:03").user(true).build().get());
    service.send(anotherChat);

    ArgumentCaptor<ChatRequest> requests = ArgumentCaptor.forClass(ChatRequest.class);
    verify(model, times(11)).chat(requests.capture());
    List<ChatMessage> afterTools = requests.getAllValues().get(8).messages();
    assertEquals(18, afterTools.size());
    assertInstanceOf(SystemMessage.class, afterTools.get(0));
    assertEquals(UserMessage.from("Show the teams"), afterTools.get(1));
    for (int i = 0; i < 8; i++) {
      AiMessage call = assertInstanceOf(AiMessage.class, afterTools.get(2 + 2 * i));
      ToolExecutionResultMessage result = assertInstanceOf(ToolExecutionResultMessage.class,
          afterTools.get(3 + 2 * i));
      assertEquals("call-" + i, call.toolExecutionRequests().get(0).id());
      assertEquals("call-" + i, result.id());
    }
    assertTrue(((ToolExecutionResultMessage) afterTools.get(3)).text().contains("umlp Dashboard"));
    List<ChatMessage> expected = new ArrayList<>(afterTools);
    expected.add(AiMessage.from("Done"));
    expected.add(UserMessage.from("Keep using that schema"));
    assertEquals(expected, requests.getAllValues().get(9).messages());
    assertEquals(List.of(afterTools.get(0), UserMessage.from("Separate question")),
        requests.getAllValues().get(10).messages());
  }

  @Test
  void retainsCompletedToolExchangesWhenTheModelFailsAndTheUserRetries() {
    when(model.chat(any(ChatRequest.class))).thenReturn(ChatResponse.builder()
        .aiMessage(AiMessage.from(toolCall("getDomainModel"))).build())
        .thenThrow(new IllegalStateException("Model unavailable"))
        .thenReturn(response("Recovered"));
    service.send(history);
    assertErrorReply();
    history.addMessages(ChatModuleManager.messageBuilder()
        .content("Try again").timestamp("12:00:02").user(true).build().get());
    new ChatService(model).send(history);

    ArgumentCaptor<ChatRequest> requests = ArgumentCaptor.forClass(ChatRequest.class);
    verify(model, times(3)).chat(requests.capture());
    List<ChatMessage> expected = new ArrayList<>(requests.getAllValues().get(1).messages());
    expected.add(AiMessage.from("Serverfehler. Bitte erneut versuchen."));
    expected.add(UserMessage.from("Try again"));
    assertEquals(expected, requests.getAllValues().get(2).messages());
    assertEquals("Recovered", history.getMessages(3).getContent());
  }

  @Test
  void reportsIncompleteToolMessageWithoutOverwritingItOrCallingTheModel() {
    var incomplete = ChatModuleManager.modelMessageBuilder().sequence(1)
        .role(chatmodule.ModelMessageRole.TOOL).build().orElseThrow();
    history.addModelMessages(incomplete);

    service.send(history);

    assertErrorReply();
    assertEquals(List.of(incomplete), history.getModelMessagesList());
    verifyNoInteractions(model);
  }

  @Test
  void callsOpenRouterCompatibleEndpointThroughLangChain4j() throws Exception {
    AtomicReference<String> requestPath = new AtomicReference<>();
    AtomicReference<String> requestBody = new AtomicReference<>();
    AtomicReference<String> authorization = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      requestPath.set(exchange.getRequestURI().getPath());
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] body = """
          {"id":"test-response","object":"chat.completion","created":0,"model":"openai/gpt-6-sol",
           "choices":[{"index":0,"message":{"role":"assistant","content":"**Two teams**"},
           "finish_reason":"stop"}],"usage":{"prompt_tokens":5,"completion_tokens":3,"total_tokens":8}}
          """.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      ChatModel openRouter = OpenAiChatModel.builder()
          .baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1")
          .apiKey("test-api-key")
          .modelName("openai/gpt-6-sol").timeout(Duration.ofSeconds(5)).maxRetries(0).build();
      new ChatService(openRouter).send(history);

      assertEquals("/api/v1/chat/completions", requestPath.get());
      assertEquals("Bearer test-api-key", authorization.get());
      JsonObject request = JsonParser.parseJsonObject(requestBody.get());
      assertEquals("openai/gpt-6-sol", request.getStringMember("model"));
      assertFalse(request.getBooleanMemberOpt("stream").orElse(false));
      assertEquals("system", request.getArrayMember("messages").get(0).getAsJsonObject().getStringMember("role"));
      assertEquals("user", request.getArrayMember("messages").get(1).getAsJsonObject().getStringMember("role"));
      assertEquals("Show the teams", request.getArrayMember("messages").get(1).getAsJsonObject().getStringMember("content"));
      assertEquals(12, request.getArrayMember("tools").size());
      assertEquals("**Two teams**", history.getMessages(1).getContent());
    } finally {
      server.stop(0);
    }
  }

  private ChatResponse response(String content) {
    return ChatResponse.builder().aiMessage(AiMessage.from(content)).build();
  }

  private ToolExecutionRequest toolCall(String name) {
    return ToolExecutionRequest.builder().id(name + "-1").name(name).arguments("{}").build();
  }

  private void assertErrorReply() {
    assertEquals(2, history.getMessagesList().size());
    assertFalse(history.getMessages(1).isUser());
    assertEquals("Serverfehler. Bitte erneut versuchen.", history.getMessages(1).getContent());
  }
}
