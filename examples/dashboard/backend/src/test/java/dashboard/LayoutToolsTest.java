package dashboard;

import de.monticore.symboltable.serialization.JsonParser;
import de.monticore.symboltable.serialization.JsonPrinter;
import de.monticore.symboltable.serialization.json.JsonObject;

import chatmodule.ChatModuleManager;
import chatmodule.MessageHistory;

import dashboardmodule.LayoutTools;
import dashboardmodule.LayoutTools.TilePlacement;
import dashboard.test_infrastructure.DashboardMockTestSetup;
import dashboardmodule.CellConfig;
import dashboardmodule.DashboardModuleManager;
import dashboardmodule.DashboardModuleMock;
import dashboardmodule.ViewConfig;
import dashboardmodule.VizType;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import umlp.test_rte.ClientSetup;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LayoutToolsTest extends DashboardMockTestSetup {
  private ViewConfig view;
  private ViewConfig otherView;
  private CellConfig first;
  private CellConfig second;
  private CellConfig other;
  private LayoutTools tools;

  LayoutToolsTest() {
    super(ClientSetup.of(0));
  }

  @Override
  protected void initBackendManager() {
    super.initBackendManager();
    chatmodule.ChatModuleMock.init();
    DashboardModuleMock.init();
  }

  @BeforeEach
  void setUp() {
    getBackend().activate();
    view = DashboardModuleManager.viewConfigBuilder().vid(0).name("Current view").build().get();
    otherView = DashboardModuleManager.viewConfigBuilder().vid(1).name("Other view").build().get();
    first = cell("First tile", 0);
    second = cell("Second tile", 3);
    other = cell("Other tile", 0);
    view.addCells(first);
    view.addCells(second);
    otherView.addCells(other);
    tools = new LayoutTools(view);
  }

  @Test
  void readsCurrentViewLayoutWithTileIdsNamesAndGridUnits() throws Exception {
    JsonObject layout = JsonParser.parseJsonObject(tools.getCurrentLayout());
    assertEquals(view.getGemId(), layout.getLongMember("viewId"));
    assertEquals("Current view", layout.getStringMember("name"));
    assertEquals(12, layout.getIntegerMember("columns"));
    assertEquals(2, layout.getArrayMember("tiles").size());
    JsonObject tile = layout.getArrayMember("tiles").get(0).getAsJsonObject();
    assertEquals(first.getGemId(), tile.getLongMember("id"));
    assertEquals("First tile", tile.getStringMember("name"));
    assertEquals(0, tile.getDoubleMember("x"));
    assertEquals(0, tile.getDoubleMember("y"));
    assertEquals(3, tile.getDoubleMember("width"));
    assertEquals(3, tile.getDoubleMember("height"));
  }

  @Test
  void swapsAndResizesTilesWithoutChangingContentOrAnotherView() {
    String result = tools.setCurrentLayout(List.of(place(first, 6, 0, 6, 4), place(second, 0, 0, 3, 3)));
    assertTrue(result.startsWith("Updated"));
    assertEquals(6, first.getPosition().getX());
    assertEquals(6, first.getSize().getWidth());
    assertEquals(4, first.getSize().getHeight());
    assertEquals(0, second.getPosition().getX());
    assertEquals("return example data;", first.getData());
    assertEquals(0, other.getPosition().getX());
  }

  @Test
  void rejectsInvalidLayoutsBeforeChangingAnyTile() {
    List<List<TilePlacement>> invalidLayouts = List.of(
        List.of(place(first, 6, 0, 3, 3)), // missing tile
        List.of(place(first, 6, 0, 3, 3), place(first, 0, 0, 3, 3)), // duplicate
        List.of(place(first, 6, 0, 3, 3), place(other, 0, 0, 3, 3)), // another view
        List.of(place(first, 6, 0, 3, 3), place(second, 7, 0, 3, 3)), // overlap
        List.of(place(first, 6, 0, 3, 3), place(second, -1, 0, 3, 3)),
        List.of(place(first, 6, 0, 3, 3), place(second, 10, 0, 3, 3)),
        List.of(place(first, 6, 0, 3, 3), place(second, 0, 0, 2, 3)),
        List.of(place(first, 6, 0, 3, 3), place(second, 0, -1, 3, 3)),
        List.of(place(first, 6, 0, 3, 3), place(second, 0, Integer.MAX_VALUE, 3, 3)),
        List.of(place(first, 6, 0, 3, 3), new TilePlacement(second.getGemId(), null, 0, 3, 3)));
    for (List<TilePlacement> layout : invalidLayouts) {
      assertTrue(tools.setCurrentLayout(layout).startsWith("Invalid"));
      assertEquals(0, first.getPosition().getX());
      assertEquals(3, second.getPosition().getX());
      assertEquals(3, first.getSize().getWidth());
    }
    assertTrue(tools.setCurrentLayout(null).startsWith("Invalid"));
  }

  @Test
  void reportsMissingCurrentView() {
    LayoutTools unavailable = new LayoutTools(null);
    assertEquals("No current dashboard view is available.", unavailable.getCurrentLayout());
    assertEquals("No current dashboard view is available.", unavailable.setCurrentLayout(List.of()));
  }

  @Test
  void rejectsInvalidJsonPlacementsBeforeChangingAnyTile() {
    for (Map<String, Object> invalid : List.<Map<String, Object>>of(
        Map.of("id", second.getGemId(), "x", 0, "y", 0, "width", 3),
        Map.of("id", second.getGemId(), "x", 0.5, "y", 0, "width", 3, "height", 3),
        Map.of("id", second.getGemId(), "x", "0", "y", 0, "width", 3, "height", 3),
        Map.of("id", second.getGemId(), "x", 2147483648L, "y", 0, "width", 3, "height", 3))) {
      assertTrue(tools.applyCurrentLayout(List.of(
          Map.of("id", first.getGemId(), "x", 6, "y", 0, "width", 3, "height", 3), invalid))
          .startsWith("Invalid layout:"));
      assertEquals(0.0, first.getPosition().getX());
      assertEquals(3.0, second.getPosition().getX());
    }
  }

  @Test
  void modelCanReadThenApplyLayoutUsingAnnotatedTools() throws Exception {
    ChatModel model = mock(ChatModel.class);
    JsonPrinter printer = new JsonPrinter(true);
    printer.beginObject();
    printer.beginArray("tiles");
    for (CellConfig cell : List.of(first, second)) {
      printer.beginObject();
      printer.member("id", cell.getGemId());
      printer.member("name", cell.getName());
      printer.member("x", cell == first ? 3 : 0);
      printer.member("y", 0);
      printer.member("width", 3);
      printer.member("height", 3);
      printer.endObject();
    }
    printer.endArray();
    printer.endObject();
    String arguments = printer.getContent();
    when(model.chat(any(ChatRequest.class))).thenReturn(call("getCurrentLayout", "{}"),
        call("setCurrentLayout", arguments),
        ChatResponse.builder().aiMessage(AiMessage.from("Updated the layout.")).build());
    MessageHistory history = ChatModuleManager.messageHistoryBuilder().name("Layout chat").build().get();
    history.addMessages(ChatModuleManager.messageBuilder().user(true).timestamp("12:00:00")
        .content("Swap the two tiles").build().get());

    new ChatService(model).send(history, view);

    assertEquals(3, first.getPosition().getX());
    assertEquals(0, second.getPosition().getX());
    assertTrue(history.getMessages(1).getContent().startsWith("Updated the layout"));
    ArgumentCaptor<ChatRequest> requests = ArgumentCaptor.forClass(ChatRequest.class);
    verify(model, times(3)).chat(requests.capture());
    assertTrue(requests.getAllValues().get(1).messages().stream()
        .anyMatch(message -> message instanceof ToolExecutionResultMessage result
            && result.text().contains("First tile")));
  }

  private CellConfig cell(String name, int x) {
    return DashboardModuleManager.cellConfigBuilder().name(name).data("return example data;").viz(VizType.TILE)
        .position(DashboardModuleManager.dashboardModulePositionBuilder().x((double) x).y(0.0).build().get())
        .size(DashboardModuleManager.dashboardModuleSizeBuilder().width(3.0).height(3.0).build().get())
        .build().get();
  }

  private TilePlacement place(CellConfig cell, int x, int y, int width, int height) {
    return new TilePlacement(cell.getGemId(), x, y, width, height);
  }

  private ChatResponse call(String name, String arguments) {
    return ChatResponse.builder().aiMessage(AiMessage.from(ToolExecutionRequest.builder()
        .id(name + "-1").name(name).arguments(arguments).build())).build();
  }
}
