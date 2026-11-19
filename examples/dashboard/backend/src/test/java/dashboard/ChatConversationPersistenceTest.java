package dashboard;

import chatmodule.PersistedChatMemory;

import chatmodule.ChatModuleDAOFactory;
import chatmodule.ChatModuleManager;
import chatmodule.ChatModuleManagerImpl;
import chatmodule.MessageHistory;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import umlp.backendrte.common.DatabaseAccessManager;
import umlp.backendrte.common.DatabaseAccessManager4SQL;
import umlp.test_rte.ClientSetup;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatConversationPersistenceTest extends DashboardMockTestSetup {
  ChatConversationPersistenceTest() {
    super(ClientSetup.of(0));
  }

  @Override
  protected void initBackendManager() {
    super.initBackendManager();
    chatmodule.ChatModuleMock.init();
    dashboardmodule.DashboardModuleMock.init();
  }

  @BeforeEach
  void setUpDatabase() throws Exception {
    getBackend().activate();
    // Explicitly use a disposable H2 database, regardless of deployment credentials.
    DatabaseAccessManager.setInstance(new TestDatabase());
    DatabaseAccessManager.setup();
    try (var connection = DatabaseAccessManager.getConnection()) {
      assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:chat-transcript-"));
    }
    try (var schema = getClass().getClassLoader().getResourceAsStream("ChatModule.sql")) {
      assertNotNull(schema);
      assertTrue(DatabaseAccessManager4SQL.initSchema(new String(schema.readAllBytes(), StandardCharsets.UTF_8)));
    }
    ChatModuleManagerImpl.init();
  }

  @AfterEach
  void closeDatabase() {
    DatabaseAccessManager.closeConnection();
  }

  @Test
  void restoresBatchedToolCallsAndResultsFromSqlAfterManagerAndServiceReconstruction() throws Exception {
    MessageHistory history = ChatModuleManager.messageHistoryBuilder().name("Persistent chat").build().get();
    history.addMessages(ChatModuleManager.messageBuilder()
        .content("Show the teams\nwith names like \"Müller\" " + "context ".repeat(1000))
        .timestamp("").user(true).build().get());
    ToolExecutionRequest schemaCall = ToolExecutionRequest.builder()
        .id("schema-call").name("getDomainModel").arguments("{}").build();
    ToolExecutionRequest layoutCall = ToolExecutionRequest.builder()
        .id("layout-call").name("getCurrentLayout").arguments("{}").build();
    ChatModel model = mock(ChatModel.class);
    when(model.chat(any(ChatRequest.class))).thenReturn(ChatResponse.builder()
        .aiMessage(AiMessage.from(schemaCall, layoutCall)).build())
        .thenAnswer(ignored -> {
          // Both tool results must already be durable before the next model call finishes.
          var stored = ChatModuleDAOFactory.getMessageHistoryDAO().selectMessageHistory(history.getGemId()).orElseThrow();
          assertEquals(2, stored.getModelMessagesList().stream()
              .filter(message -> message.getRole() == chatmodule.ModelMessageRole.TOOL).count());
          var calls = stored.getModelMessagesList().stream()
              .filter(message -> message.getRole() == chatmodule.ModelMessageRole.ASSISTANT).findFirst().orElseThrow();
          assertEquals(2, calls.getToolCallsList().size());
          assertEquals("{}", calls.getToolCallsList().get(0).getArguments().orElseThrow());
          return response("Here are the teams.");
        }).thenReturn(response("The schema is still available."));
    new ChatService(model).send(history);
    assertEquals("Here are the teams.", history.getMessages(1).getContent());

    long historyId = history.getGemId();
    List<ChatMessage> savedMessages = new PersistedChatMemory(history).messages();
    ChatModuleManagerImpl.init();
    MessageHistory restored = ChatModuleManager.getMessageHistorySure(historyId);
    assertNotSame(history, restored);
    assertEquals(savedMessages, new PersistedChatMemory(restored).messages());
    assertEquals(2, restored.getMessagesList().size());
    restored.addMessages(ChatModuleManager.messageBuilder()
        .content("Use that schema again").timestamp("").user(true).build().get());

    new ChatService(model).send(restored);

    ArgumentCaptor<ChatRequest> requests = ArgumentCaptor.forClass(ChatRequest.class);
    verify(model, times(3)).chat(requests.capture());
    List<ChatMessage> expected = new ArrayList<>(requests.getAllValues().get(1).messages());
    expected.add(AiMessage.from("Here are the teams."));
    expected.add(UserMessage.from("Use that schema again"));
    assertEquals(expected, requests.getAllValues().get(2).messages());
    assertEquals(AiMessage.from(schemaCall, layoutCall), expected.get(2));
    var persisted = ChatModuleDAOFactory.getMessageHistoryDAO().selectMessageHistory(historyId).orElseThrow();
    assertEquals(new PersistedChatMemory(restored).messages(), new PersistedChatMemory(persisted).messages());
    assertEquals(4, persisted.getMessagesList().size());
  }

  @Test
  void persistsTypedFieldsAndDeletesOwnedTranscriptRecordsWhenMemoryIsCleared() {
    MessageHistory history = ChatModuleManager.messageHistoryBuilder().name("Typed transcript").build().orElseThrow();
    ToolExecutionRequest firstCall = ToolExecutionRequest.builder().id("call-1").name("query")
        .arguments("{\"filter\":\"Müller\\n\\\"team\\\"\"}").build();
    ToolExecutionRequest secondCall = ToolExecutionRequest.builder().id("call-2").name("preview")
        .arguments("{}").build();
    List<ChatMessage> expected = List.of(SystemMessage.from("Dashboard instructions"), UserMessage.from("Show teams"),
        AiMessage.builder().text("Checking both sources").thinking("Need query and preview")
            .toolExecutionRequests(List.of(firstCall, secondCall)).build(),
        ToolExecutionResultMessage.builder().id("call-1").toolName("query").text("No matching rows\nTry again")
            .isError(true).build(),
        ToolExecutionResultMessage.from(secondCall, "[]"));
    PersistedChatMemory memory = new PersistedChatMemory(history);
    expected.forEach(memory::add);
    long historyId = history.getGemId();

    ChatModuleManagerImpl.init();
    MessageHistory restored = ChatModuleManager.getMessageHistorySure(historyId);
    PersistedChatMemory restoredMemory = new PersistedChatMemory(restored);
    assertEquals(expected, restoredMemory.messages());
    assertEquals(5, restored.getModelMessagesList().size());
    assertEquals(2, ChatModuleManager.getToolCallList().size());
    assertEquals(2, ChatModuleManager.getToolResultList().size());

    restoredMemory.clear();

    ChatModuleManagerImpl.init();
    assertTrue(ChatModuleManager.getMessageHistorySure(historyId).getModelMessagesList().isEmpty());
    assertTrue(ChatModuleManager.getModelMessageList().isEmpty());
    assertTrue(ChatModuleManager.getToolCallList().isEmpty());
    assertTrue(ChatModuleManager.getToolResultList().isEmpty());
  }

  private static ChatResponse response(String text) {
    return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
  }

  private static final class TestDatabase extends DatabaseAccessManager4SQL {
    @Override
    protected void _setup() {
      createDataSource("jdbc:h2:mem:chat-transcript-" + UUID.randomUUID(), new Properties());
    }
  }
}
