Example application using the [dashboard module](../../modules/dashboard) and
[chat module](../../modules/chat).
It provides an example domain (organizations, teams, people, ...) and dashboard configurations visualizing it.
The page `home/CollabDemo` additionally shows the text area of the
[collaborative text module](../../modules/collabtext): open it in two windows to edit the same text.

Prerequisites: Java 21 installed 
To build the single jar for sharing use `gradle :examples:dashboard:frontend:fullAppJar`
The jar will be built to `frontend/build/libs`

To run the fullApp jar use the following commands:
````bash
java -Djarmode=tools -jar .\frontend-7.9.1-SNAPSHOT-fullAppJar.jar extract
cd .\frontend-7.9.1-SNAPSHOT-fullAppJar
java -jar .\frontend-7.9.1-SNAPSHOT-fullAppJar.jar
````
Then open localhost:4200 in your browser. 

The `home/TestDashboard` page embeds one chat beside the dashboard. The shared `chatcomponents.Conversation` UI
and message/history/source models come from `@montigem/chat` and `ChatModule`, backed by the
`GemChat` primitives from the same module. Each page instance starts a new conversation.
Messages and replies use the application's normal websocket synchronization.
At startup, the example registers a `MessageSender` handler that resolves the selected view from
the context ID and calls its dashboard-specific `ChatService`. The example constructs the model provider and composes the module-provided chat service and dashboard tools.
Its instruction manifest and persona remain application resources.

The example constructs LangChain4j's `OpenAiChatModel` for an OpenAI-compatible endpoint.
Configure the backend with these required environment variables:

| Variable | Purpose |
| --- | --- |
| `CHATBOT_DEMO_BASEURL` | Server base URL; `/openai/v1` is appended when absent |
| `CHATBOT_DEMO_KEY` | API key |
| `CHATBOT_DEMO_MODEL` | Model name |

Model construction stays in the example's `dashboard.ChatService`; the reusable
`chatmodule.ChatService` receives the model, system instructions, and request-specific tools.

Each request passes the current conversation as LangChain4j user and assistant messages in order.
Replies render as Markdown. This direct model integration does not retrieve documents or attach
source citations. An unavailable model or an empty response produces a visible error reply;
the input becomes usable again. Requests time out after 120 seconds, with no automatic retries.

The assistant uses LangChain4j AI Services and annotation-based tools. The selected model must support tool calling.
Each chat's `MessageHistory.modelMessages` stores the complete model conversation as typed
`ModelMessage`, `ToolCall`, and `ToolResult` records. The backend saves each exchange through the generated
database observer and restores it on subsequent requests, including after a restart. No message window
evicts older exchanges. Histories without a model transcript start from their visible messages.
The chat UI displays user messages and final replies; internal exchanges are separate domain records.

The chat has two tools for the view selected when a message is sent:

- `getCurrentLayout` returns JSON with the view name/ID and info, the 12-column grid limits, and
  every tile's ID, name, `x`, `y`, `width`, and `height` in grid units (not pixels).
- `setCurrentLayout(tiles)` applies a complete list of placements for that view. Include
  every tile exactly once. Coordinates must be nonnegative integers, width and height
  must be at least 3, and `x + width` must not exceed 12. Tiles must not overlap.
  Invalid proposals leave the layout unchanged. Tile content and other views are preserved.

Try “What is the current layout?” or “Swap the positions of Lines of Code and No. of Commits.”
The assistant can read the layout and then apply a new one in the same turn. Successful
changes update the visible grid and persist through the normal domain synchronization.
Dragging/resizing tiles also saves their geometry, so subsequent layout queries see those changes.
The grid observes tile positions and sizes directly and batches incoming changes before refreshing.

The assistant's system instructions come from the ordered classpath resource list in
[`chat/instructions.list`](backend/src/main/resources/chat/instructions.list): the
[persona](backend/src/main/resources/chat/persona.md),
[dashboard operations skill](../../modules/dashboard/backend/src/main/resources/skills/dashboard-operations/SKILL.md), and
[chart data skill](../../modules/dashboard/backend/src/main/resources/skills/dashboard-chart-data/SKILL.md).
To add a skill, bundle its resource and add its classpath path to the manifest; blank lines and
lines starting with `#` are ignored. Missing or empty resources fail when the chat service is
constructed. The loader reads classpath streams, so the same resources work in a packaged JAR.
Tool descriptions contain each operation's parameters and constraints.

Two more tools let the assistant add charts:

- `checkInterpreterCode(viz, data)` executes a script with the dashboard interpreter and returns
  parse, type, runtime, or incorrect return type errors so the assistant can repair it.
- `createCell(name, info, viz, data)` validates the script again, creates the cell with default chart
  options, and adds it to the selected view. New cells appear immediately below existing cells,
  with width 6 and height 4. The assistant can then rearrange them with the layout tools.
  `info` concisely captures the intent of the cell (shown as tooltip of its info icon);
  `setViewInfo(info)` does the same for the selected view, shown in its info dialog.

Supported visualizations are `BAR`, `LINE`, `PIE`, `SCATTER`, `BOX`, `SPEED`, `NETWORK`, and `FILTERABLE_TABLE`.
A `FILTERABLE_TABLE` cell computes typed columns (numbers, texts, or booleans), which can be sorted by clicking their
headers, as in the "Teams" cell of "View 2". Separate expressions per column filter its rows
(e.g., `>= 30`, set with the filter button of a header) and render its values (e.g., `Math.round(v) + " years"`), as in the "People of age 30 or older" cell of "View 2";
the assistant sets them with `setCellOptions`.
Scripts execute real code and should only read business data and construct chart payloads.
Try "Add a pie chart showing completed and open tasks per team." or
"Add a table with the number of international people per team." or
"Add a filterable table of all people with their role and age, showing only seniors."

The assistant calls `getDomainModel` to read `models/Dashboard.umlp` from the backend's classpath
before writing data-backed scripts. The build packages the source model from `models/` into the
backend resources. The reusable skills in the dashboard module describe the interpreter and chart
APIs; the diagram supplies the application's classes, fields, and associations.
The frontend also publishes the model at `/assets/models/Dashboard.umlp` through
`montigem.publishDomainModels = true`.
