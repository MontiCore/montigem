package dashboard;

import de.monticore.symboltable.serialization.JsonParser;
import de.monticore.symboltable.serialization.JsonPrinter;
import de.monticore.symboltable.serialization.json.JsonObject;

import dashboardmodule.CellTools;
import dashboardmodule.LayoutTools;

import chatmodule.ChatModuleManager;
import chatmodule.MessageHistory;

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
import dashboardmodule.commands.InterpreterWithErrorHandling;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import umlp.test_rte.ClientSetup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CellToolsTest extends DashboardMockTestSetup {
  private static final String PIE_SCRIPT = """
      GemPieChartEntry entry = GemPieChartEntryBuilder().value(20).label("Completed").build().get();
      List<GemPieChartEntry> entries = [entry];
      return GemPieChartDataBuilder().entries(entries).build().get();
      """;
  private ViewConfig view;
  private CellTools tools;

  CellToolsTest() {
    super(ClientSetup.of(0));
  }

  // As the application does during startup, such that the scripts of the tables can read the domain
  @BeforeAll
  static void registerDomain() {
    InterpreterWithErrorHandling.addDomain("dashboard", DashboardManager.class);
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
    view = DashboardModuleManager.viewConfigBuilder().vid(0).name("Current view").info("").build().get();
    tools = new CellTools(view);
  }

  @Test
  void executesScriptAndRejectsWrongReturnTypeAndMalformedCode() {
    assertTrue(tools.checkInterpreterCode("PIE", PIE_SCRIPT).startsWith("Success:"));
    assertTrue(tools.checkInterpreterCode("BAR", PIE_SCRIPT).contains("expected GemBarChartData"));
    assertTrue(tools.checkInterpreterCode("PIE", "this is not a script;").startsWith("Interpreter error:"));
    assertTrue(view.getCellsList().isEmpty());
  }

  @Test
  void createsCellsWithStoredScriptDefaultOptionsAndNonoverlappingGeometry() {
    ViewConfig other = DashboardModuleManager.viewConfigBuilder().vid(1).name("Other view").info("").build().get();
    assertTrue(tools.createCell("First chart", "Test info.", "PIE", PIE_SCRIPT).startsWith("Created cell"));
    assertTrue(tools.createCell("Second chart", "Test info.", "PIE", PIE_SCRIPT).startsWith("Created cell"));
    assertEquals(2, view.getCellsList().size());
    CellConfig first = view.getCellsList().get(0);
    CellConfig second = view.getCellsList().get(1);
    assertEquals(PIE_SCRIPT, first.getData());
    assertEquals(VizType.PIE, first.getViz());
    assertEquals(0, first.getExtra().getPiextra().getInnerRadius());
    assertEquals(6, first.getSize().getWidth());
    assertEquals(4, first.getSize().getHeight());
    assertEquals(0, first.getPosition().getY());
    assertEquals(4, second.getPosition().getY());
    assertTrue(other.getCellsList().isEmpty());
  }

  @Test
  void storesUpdatesAndCopiesTheInfoOfCellsAndViews() throws Exception {
    CellConfig cell = pie();
    assertEquals("Test info.", cell.getInfo());
    assertEquals("Test info.", JsonParser.parseJsonObject(tools.getCell(cell.getGemId())).getStringMember("info"));

    assertTrue(tools.updateCell(cell.getGemId(), "{'info':' Share of the tasks. '}".replace('\'', '"'))
        .startsWith("Updated cell"));
    assertEquals("Share of the tasks.", cell.getInfo());
    assertEquals("Original", cell.getName());
    assertTrue(tools.updateCell(cell.getGemId(), "{'info':' '}".replace('\'', '"')).startsWith("Invalid cell:"));
    assertEquals("Share of the tasks.", cell.getInfo());

    tools.duplicateCell(cell.getGemId(), "Copy");
    assertEquals("Share of the tasks.", view.getCellsList().get(view.getCellsList().size() - 1).getInfo());

    assertTrue(tools.setViewInfo(" Progress of the teams. ").startsWith("Updated info"));
    assertEquals("Progress of the teams.", view.getInfo());
    assertEquals("Progress of the teams.",
        JsonParser.parseJsonObject(new LayoutTools(view).getCurrentLayout()).getStringMember("info"));
    assertTrue(tools.setViewInfo("").startsWith("Invalid view info"));
    assertEquals("Progress of the teams.", view.getInfo());
    assertTrue(new CellTools(null).setViewInfo("Info").contains("No current"));
  }

  @Test
  void invalidRequestsDoNotAddCells() {
    assertTrue(tools.createCell("Chart", "Test info.", "BAR", PIE_SCRIPT).contains("expected GemBarChartData"));
    assertTrue(tools.createCell("Chart", "Test info.", "PIE", "broken;").startsWith("Interpreter error:"));
    assertTrue(tools.createCell(" ", "Test info.", "PIE", PIE_SCRIPT).startsWith("Invalid cell:"));
    assertTrue(tools.createCell("Chart", " ", "PIE", PIE_SCRIPT).startsWith("Invalid cell:"));
    assertTrue(tools.createCell("Chart", null, "PIE", PIE_SCRIPT).startsWith("Invalid cell:"));
    assertTrue(tools.createCell("Chart", "Test info.", "FILTERABLE_TABLE", PIE_SCRIPT).contains("GemFilterableTableData"));
    assertTrue(tools.createCell("Chart", "Test info.", "TILE", PIE_SCRIPT).startsWith("Unsupported visualization"));
    assertTrue(tools.checkInterpreterCode("PIE", "").startsWith("Interpreter error:"));
    assertTrue(new CellTools(null).createCell("Chart", "Test info.", "PIE", PIE_SCRIPT).contains("No current"));
    assertTrue(view.getCellsList().isEmpty());
  }

  @Test
  void assistantChecksCodeThenCreatesCellInSelectedView() throws Exception {
    ChatModel model = mock(ChatModel.class);
    JsonPrinter check = new JsonPrinter(true);
    check.beginObject();
    check.member("viz", "PIE");
    check.member("data", PIE_SCRIPT);
    check.endObject();
    JsonPrinter create = new JsonPrinter(true);
    create.beginObject();
    create.member("name", "Completed");
    create.member("info", "Share of the completed tasks.");
    create.member("viz", "PIE");
    create.member("data", PIE_SCRIPT);
    create.endObject();
    when(model.chat(any(ChatRequest.class))).thenReturn(
        toolCall("checkInterpreterCode", check.getContent()),
        toolCall("createCell", create.getContent()),
        ChatResponse.builder().aiMessage(AiMessage.from("Created the chart.")).build());
    MessageHistory history = ChatModuleManager.messageHistoryBuilder().name("Chat").build().get();
    history.addMessages(ChatModuleManager.messageBuilder().content("Add a pie chart")
        .timestamp("").user(true).build().get());

    new ChatService(model).send(history, view);

    assertEquals(1, view.getCellsList().size());
    assertEquals("Completed", view.getCellsList().get(0).getName());
    assertEquals("Created the chart.", history.getMessages(1).getContent());
    ArgumentCaptor<ChatRequest> requests = ArgumentCaptor.forClass(ChatRequest.class);
    verify(model, times(3)).chat(requests.capture());
    assertTrue(requests.getAllValues().get(1).messages().stream().anyMatch(message ->
        message instanceof ToolExecutionResultMessage result && result.text().startsWith("Success:")));
  }

  @Test
  void createsTablesFromApplicationDomain() throws IOException {
    Person alice = DashboardManager.personBuilder().name("Alice").role(Role.SENIOR).tenure(7)
        .education(Education.MASTER).international(true).age(33)
        .statistics(DashboardManager.statisticsBuilder().build().get()).build().get();
    Task task = DashboardManager.taskBuilder().name("Migration").description("Move services").completed(true)
        .build().get();
    DashboardManager.teamBuilder().name("Platform").department(Department.DEVOPS)
        .people(Set.of(alice)).tasks(Set.of(task)).build().get();
    String script = TEAM_TABLE_SCRIPT;

    assertTrue(tools.createCell("Teams", "Test info.", "FILTERABLE_TABLE", script).startsWith("Created cell"));
    CellConfig cell = view.getCellsList().get(0);
    assertEquals(VizType.FILTERABLE_TABLE, cell.getViz());
    assertTrue(tools.getCell(cell.getGemId()).contains("\"options\":{\"columns\":[]}"));
    assertTrue(tools.setCellOptions(cell.getGemId(), "{'stacked':true}".replace('\'', '"')).startsWith("Invalid options:"));
    var preview = JsonParser.parseJsonObject(tools.previewCell(cell.getGemId()));
    assertEquals(List.of("Team", "Department", "People", "Completed tasks"),
        preview.getArrayMember("headers").stream().map(e -> e.getAsJsonString().getValue()).toList());
    assertEquals(1, preview.getIntegerMember("itemCount"));
    assertEquals("Platform | DEVOPS | 1 | 1 of 1", preview.getArrayMember("sample").get(0).getAsJsonString().getValue());
  }

  @Test
  void createsFilterableTablesWithColumnScriptsAsInTheSkill() throws IOException {
    Person alice = DashboardManager.personBuilder().name("Alice").role(Role.SENIOR).tenure(7)
        .education(Education.MASTER).international(true).age(33)
        .statistics(DashboardManager.statisticsBuilder().build().get()).build().get();
    Person bob = DashboardManager.personBuilder().name("Bob").role(Role.JUNIOR).tenure(1)
        .education(Education.BACHELOR).international(false).age(41)
        .statistics(DashboardManager.statisticsBuilder().build().get()).build().get();
    Person carol = DashboardManager.personBuilder().name("Carol").role(Role.MANAGER).tenure(4)
        .education(Education.MASTER).international(false).age(29)
        .statistics(DashboardManager.statisticsBuilder().build().get()).build().get();

    assertTrue(tools.createCell("People", "Test info.", "FILTERABLE_TABLE", PEOPLE_TABLE_SCRIPT)
        .startsWith("Created cell"));
    CellConfig cell = view.getCellsList().get(0);
    assertTrue(tools.getCell(cell.getGemId()).contains("\"options\":{\"columns\":[]}"));
    var preview = JsonParser.parseJsonObject(tools.previewCell(cell.getGemId()));
    assertEquals(3, preview.getIntegerMember("itemCount"));

    String options = exampleOfTheSkill("```json\n");
    assertTrue(tools.setCellOptions(cell.getGemId(), options).startsWith("Updated cell options"));
    assertEquals(3, cell.getExtra().getTableextra().getColumnsList().size());
    preview = JsonParser.parseJsonObject(tools.previewCell(cell.getGemId()));
    assertEquals(1, preview.getIntegerMember("itemCount"));
    assertEquals("Alice | SENIOR | 33 years | yes", preview.getArrayMember("sample").get(0).getAsJsonString().getValue());

    // the scripts are checked together with the data script before any change
    assertTrue(tools.setCellOptions(cell.getGemId(), "{'columns':[{'column':'Age','filter':'return (Double v) -> v;'}]}"
        .replace('\'', '"')).contains("has to return a boolean"));
    assertTrue(tools.setCellOptions(cell.getGemId(), "{'columns':[{'column':'Height','filter':''}]}"
        .replace('\'', '"')).startsWith("Updated cell options"));
    assertTrue(tools.setCellOptions(cell.getGemId(), "{'columns':[{'column':'Height','filter':'return (Double v) -> true;'}]}"
        .replace('\'', '"')).contains("There is no column \"Height\""));
    assertEquals(1, cell.getExtra().getTableextra().getColumnsList().size());
    assertEquals("Height", cell.getExtra().getTableextra().getColumns(0).getColumn());
    assertTrue(tools.updateCell(cell.getGemId(), "{'data':'return 1;'}".replace('\'', '"'))
        .contains("has to return a GemFilterableTableData"));
  }

  @Test
  void genericTableExampleWorksWithoutApplicationData() throws IOException {
    assertTrue(tools.createCell("Sample", "Test info.", "FILTERABLE_TABLE",
        exampleOfTheSkill("### Filterable table\n\n```java\n")).startsWith("Created cell"));
    var preview = JsonParser.parseJsonObject(tools.previewCell(view.getCells(0).getGemId()));
    assertEquals(2, preview.getIntegerMember("itemCount"));
    assertEquals("Alpha | 12 | true | 2026-01-01 | 2026-01-01T09:00",
        preview.getArrayMember("sample").get(0).getAsJsonString().getValue());
    long cellId = view.getCells(0).getGemId();
    assertTrue(tools.setCellOptions(cellId,
        "{\"columns\":[{\"column\":\"Date\",\"filter\":\">= \\\"2026-01-02\\\"\"},"
            + "{\"column\":\"Time\",\"render\":\"v.getHour() + \\\"h\\\"\"}]}").startsWith("Updated cell options"));
    var filtered = JsonParser.parseJsonObject(tools.previewCell(cellId));
    assertEquals(1, filtered.getIntegerMember("itemCount"));
    assertEquals("Beta | 24 | false | 2026-01-02 | 14h",
        filtered.getArrayMember("sample").get(0).getAsJsonString().getValue());
  }

  private static final String TEAM_TABLE_SCRIPT = """
      List<String> teams = [];
      List<String> departments = [];
      List<Double> people = [];
      List<String> completedTasks = [];
      for (Team team : DashboardManager.getTeamList()) {
        int completed = 0;
        for (Task task : team.getTasksList()) {
          if (task.isCompleted()) {
            completed = completed + 1;
          }
        }
        teams.add(team.getName());
        departments.add(team.getDepartment().toString());
        people.add(1.0 * team.getPeopleList().size());
        completedTasks.add(completed + " of " + team.getTasksList().size());
      }
      List<GemFilterableTableColumn> columns = [
        GemFilterableTableColumnBuilder().header("Team").texts(teams).build().get(),
        GemFilterableTableColumnBuilder().header("Department").texts(departments).build().get(),
        GemFilterableTableColumnBuilder().header("People").numbers(people).build().get(),
        GemFilterableTableColumnBuilder().header("Completed tasks").texts(completedTasks).build().get()
      ];
      return GemFilterableTableDataBuilder().columns(columns).build().get();
      """;

  private static final String PEOPLE_TABLE_SCRIPT = """
      List<String> names = [];
      List<String> roles = [];
      List<Double> ages = [];
      List<Boolean> international = [];
      for (Person person : DashboardManager.getPersonList()) {
        names.add(person.getName());
        roles.add(person.getRole().toString());
        ages.add(1.0 * person.getAge());
        international.add(person.isInternational());
      }
      List<GemFilterableTableColumn> columns = [
        GemFilterableTableColumnBuilder().header("Name").texts(names).build().get(),
        GemFilterableTableColumnBuilder().header("Role").texts(roles).build().get(),
        GemFilterableTableColumnBuilder().header("Age").numbers(ages).build().get(),
        GemFilterableTableColumnBuilder().header("International").booleans(international).build().get()
      ];
      return GemFilterableTableDataBuilder().columns(columns).build().get();
      """;


  private static String exampleOfTheSkill(String start) throws IOException {
    try (InputStream stream = CellToolsTest.class.getClassLoader()
        .getResourceAsStream("skills/dashboard-chart-data/SKILL.md")) {
      assertNotNull(stream);
      String skill = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      Matcher matcher = Pattern.compile(Pattern.quote(start) + "(.*?)```", Pattern.DOTALL).matcher(skill);
      assertTrue(matcher.find());
      return matcher.group(1);
    }
  }

  private CellConfig pie() {
    assertTrue(tools.createCell("Original", "Test info.", "PIE", PIE_SCRIPT).startsWith("Created cell"));
    return view.getCellsList().get(view.getCellsList().size() - 1);
  }

  @Test
  void inspectsAndUpdatesCellWithoutChangingOtherFields() throws Exception {
    CellConfig cell = pie();
    var snapshot = JsonParser.parseJsonObject(tools.getCell(cell.getGemId()));
    assertEquals(PIE_SCRIPT, snapshot.getStringMember("data"));
    assertEquals("PIE", snapshot.getStringMember("viz"));
    assertEquals(0, snapshot.getObjectMember("options").getIntegerMember("innerRadius"));
    assertEquals(6, snapshot.getDoubleMember("width"));

    assertTrue(tools.setCellOptions(cell.getGemId(), "{'innerRadius':40}".replace('\'', '"')).startsWith("Updated"));
    assertTrue(tools.updateCell(cell.getGemId(), "{'name':'Renamed'}".replace('\'', '"')).startsWith("Updated"));
    assertEquals("Renamed", cell.getName());
    assertEquals(PIE_SCRIPT, cell.getData());
    assertEquals(40, cell.getExtra().getPiextra().getInnerRadius());
    assertEquals(0, cell.getPosition().getY());
    assertEquals(6, cell.getSize().getWidth());

    assertTrue(tools.updateCell(cell.getGemId(), "{'name':'Rejected','viz':'BAR'}".replace('\'', '"'))
        .contains("expected GemBarChartData"));
    assertEquals("Renamed", cell.getName());
    assertEquals(VizType.PIE, cell.getViz());
    assertEquals(40, cell.getExtra().getPiextra().getInnerRadius());
    assertTrue(tools.updateCell(cell.getGemId(), "{'data':'broken;'}".replace('\'', '"')).contains("Interpreter error"));
    assertEquals(PIE_SCRIPT, cell.getData());
  }

  @Test
  void rejectsMalformedUpdatesAndOptionsWithoutChangingCell() {
    CellConfig cell = pie();
    for (String patch : List.of("", "null", "[]", "{}", "{", "{'name':null}", "{'width':3}", "{'name':' '}")) {
      assertTrue(tools.updateCell(cell.getGemId(), patch.replace('\'', '"')).startsWith("Invalid cell:"));
    }
    for (String patch : List.of("", "null", "[]", "{}", "{", "{'stacked':true}",
        "{'innerRadius':'40'}", "{'innerRadius':null}", "{'innerRadius':-1}", "{'innerRadius':101}",
        "{'innerRadius':2.5}", "{'innerRadius':999999999999}")) {
      assertTrue(tools.setCellOptions(cell.getGemId(), patch.replace('\'', '"')).startsWith("Invalid options:"));
    }
    assertEquals("Original", cell.getName());
    assertEquals(0, cell.getExtra().getPiextra().getInnerRadius());
  }

  @Test
  void changesVisualizationWithMatchingScriptAndResetsOptions() {
    CellConfig cell = pie();
    String gauge = "return GemGaugeChartDataBuilder().entries([]).build().get();";
    JsonPrinter printer = new JsonPrinter(true);
    printer.beginObject();
    printer.member("viz", "SPEED");
    printer.member("data", gauge);
    printer.endObject();
    String patch = printer.getContent();
    assertTrue(tools.updateCell(cell.getGemId(), patch).startsWith("Updated"));
    assertEquals(VizType.SPEED, cell.getViz());
    assertEquals("", cell.getExtra().getSpeedextra().getUnit());
    assertTrue(tools.setCellOptions(cell.getGemId(), "{'unit':'%','max':100}".replace('\'', '"')).startsWith("Updated"));
    assertTrue(tools.setCellOptions(cell.getGemId(), "{'min':110,'textValue':'Rejected'}".replace('\'', '"'))
        .startsWith("Invalid options:"));
    assertEquals(0, cell.getExtra().getSpeedextra().getMin());
    assertEquals("", cell.getExtra().getSpeedextra().getTextValue());
    assertTrue(tools.setCellOptions(cell.getGemId(), "{'max':null}".replace('\'', '"')).startsWith("Updated"));
    assertEquals("%", cell.getExtra().getSpeedextra().getUnit());
    assertTrue(cell.getExtra().getSpeedextra().getMax().isEmpty());
  }

  @Test
  void previewsEverySupportedChartType() throws Exception {
    CellConfig cell = pie();
    Map<VizType, String> scripts = Map.of(
        VizType.BAR, "return GemBarChartDataBuilder().entries([]).labels([]).build().get();",
        VizType.LINE, "return GemLineChartDataBuilder().entries([]).labels([]).build().get();",
        VizType.BOX, "return GemBoxPlotDataBuilder().entries([]).build().get();",
        VizType.SPEED, "return GemGaugeChartDataBuilder().entries([]).build().get();",
        VizType.SCATTER, "return GemScatterPlotDataBuilder().sets([]).build().get();",
        VizType.NETWORK, "return GraphBuilder().nodes([]).edges([]).build().get();",
        VizType.FILTERABLE_TABLE, "return GemFilterableTableDataBuilder().columns([]).build().get();");
    for (var entry : scripts.entrySet()) {
      cell.setViz(entry.getKey());
      cell.setData(entry.getValue());
      String preview = tools.previewCell(cell.getGemId());
      assertTrue(preview.startsWith("{"), preview);
      var result = JsonParser.parseJsonObject(preview);
      assertEquals(0, result.getIntegerMember("itemCount"));
      assertTrue(result.getBooleanMember("empty"));
    }
  }

  @Test
  void duplicatesOptionsAndGeometryIndependently() {
    CellConfig source = pie();
    tools.setCellOptions(source.getGemId(), "{'innerRadius':40}".replace('\'', '"'));
    source.getSize().setWidth(9.0);
    source.getSize().setHeight(5.0);
    assertTrue(tools.duplicateCell(source.getGemId(), "Copy").startsWith("Duplicated cell:"));
    CellConfig copy = view.getCellsList().get(1);
    assertNotEquals(source.getGemId(), copy.getGemId());
    assertNotEquals(source.getExtra().getGemId(), copy.getExtra().getGemId());
    assertNotEquals(source.getExtra().getPiextra().getGemId(), copy.getExtra().getPiextra().getGemId());
    assertEquals(PIE_SCRIPT, copy.getData());
    assertEquals(9, copy.getSize().getWidth());
    assertEquals(5, copy.getPosition().getY());
    tools.setCellOptions(copy.getGemId(), "{'innerRadius':20}".replace('\'', '"'));
    assertEquals(40, source.getExtra().getPiextra().getInnerRadius());
    assertEquals(20, copy.getExtra().getPiextra().getInnerRadius());
    assertTrue(tools.duplicateCell(source.getGemId(), " ").startsWith("Invalid"));
    assertEquals(2, view.getCellsList().size());
  }

  @Test
  void removesOnlyRequestedCurrentViewLinksAndValidatesWholeRequest() {
    CellConfig first = pie();
    CellConfig second = pie();
    ViewConfig other = DashboardModuleManager.viewConfigBuilder().vid(1).name("Other").info("").build().get();
    other.addCells(first);
    assertTrue(tools.removeCells(List.of(first.getGemId(), -1L)).startsWith("Invalid"));
    assertTrue(tools.removeCells(List.of(first.getGemId(), first.getGemId())).startsWith("Invalid"));
    assertTrue(tools.removeCells(null).startsWith("Invalid"));
    assertTrue(tools.removeCells(List.of()).startsWith("Invalid"));
    assertEquals(2, view.getCellsList().size());
    assertTrue(tools.removeCells(List.of(first.getGemId(), second.getGemId())).startsWith("Removed 2"));
    assertTrue(view.getCellsList().isEmpty());
    assertEquals(first.getGemId(), other.getCellsList().get(0).getGemId());
  }

  @Test
  void previewsDataEmptinessAndErrorsWithoutChangingConfiguration() throws Exception {
    CellConfig cell = pie();
    var result = JsonParser.parseJsonObject(tools.previewCell(cell.getGemId()));
    assertEquals(1, result.getIntegerMember("itemCount"));
    assertFalse(result.getBooleanMember("empty"));
    assertEquals("Completed: 20", result.getArrayMember("sample").get(0).getAsJsonString().getValue());
    assertEquals(PIE_SCRIPT, cell.getData());
    cell.setData("return GemPieChartDataBuilder().entries([]).build().get();");
    assertTrue(JsonParser.parseJsonObject(tools.previewCell(cell.getGemId())).getBooleanMember("empty"));
    cell.setViz(VizType.BAR);
    assertTrue(tools.previewCell(cell.getGemId()).contains("expected GemBarChartData"));
    cell.setData("broken;");
    assertTrue(tools.previewCell(cell.getGemId()).startsWith("Preview error:"));
    assertEquals(1, view.getCellsList().size());
  }

  @Test
  void allNewToolsRejectCellsOutsideSelectedViewAndMissingViews() {
    CellConfig cell = pie();
    ViewConfig other = DashboardModuleManager.viewConfigBuilder().vid(1).name("Other").info("").build().get();
    for (CellTools scoped : List.of(new CellTools(other), new CellTools(null))) {
      assertFalse(scoped.getCell(cell.getGemId()).startsWith("{"));
      assertFalse(scoped.updateCell(cell.getGemId(), "{'name':'Wrong'}".replace('\'', '"')).startsWith("Updated"));
      assertFalse(scoped.setCellOptions(cell.getGemId(), "{'innerRadius':20}".replace('\'', '"')).startsWith("Updated"));
      assertFalse(scoped.duplicateCell(cell.getGemId(), "Wrong").startsWith("Duplicated"));
      assertFalse(scoped.previewCell(cell.getGemId()).startsWith("{"));
      assertFalse(scoped.removeCells(List.of(cell.getGemId())).startsWith("Removed"));
    }
    assertEquals("Original", cell.getName());
    assertEquals(1, view.getCellsList().size());
  }

  @Test
  void assistantDiscoversAndInvokesNewCellTools() throws Exception {
    CellConfig cell = pie();
    ChatModel model = mock(ChatModel.class);
    JsonPrinter get = new JsonPrinter(true);
    get.beginObject();
    get.member("cellId", cell.getGemId());
    get.endObject();
    JsonPrinter update = new JsonPrinter(true);
    update.beginObject();
    update.member("cellId", cell.getGemId());
    update.member("patchJson", "{\"name\":\"Renamed\"}");
    update.endObject();
    when(model.chat(any(ChatRequest.class))).thenReturn(
        toolCall("getCell", get.getContent()),
        toolCall("updateCell", update.getContent()),
        ChatResponse.builder().aiMessage(AiMessage.from("Renamed the chart.")).build());
    MessageHistory history = ChatModuleManager.messageHistoryBuilder().name("Chat").build().get();
    history.addMessages(ChatModuleManager.messageBuilder().content("Rename this chart")
        .timestamp("").user(true).build().get());
    new ChatService(model).send(history, view);
    assertEquals("Renamed", cell.getName());
    assertEquals("Renamed the chart.", history.getMessages(1).getContent());
    ArgumentCaptor<ChatRequest> requests = ArgumentCaptor.forClass(ChatRequest.class);
    verify(model, times(3)).chat(requests.capture());
    var names = requests.getAllValues().get(0).parameters().toolSpecifications().stream().map(t -> t.name()).toList();
    assertTrue(names.containsAll(Set.of("getCell", "updateCell", "setCellOptions", "duplicateCell", "removeCells", "previewCell")));
  }

  private ChatResponse toolCall(String name, String arguments) {
    return ChatResponse.builder().aiMessage(AiMessage.from(ToolExecutionRequest.builder()
        .id(name + "-1").name(name).arguments(arguments).build())).build();
  }
}
