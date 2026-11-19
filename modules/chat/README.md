# Chat module

Reusable conversation models and a synchronized chat component. The module follows the dashboard
module's backend/frontend structure.

- `models/ChatModule.umlp`: `SenderService`, `Message`, `MessageHistory`, and `Source`.
- `backend`: shared message helpers, the `SendMessage` command, `MessageSender` extension point,
  and a LangChain4j `ChatService` with persistent conversation memory and classpath instruction loading.
- `frontend/chat`: `chatcomponents.Conversation` and the `GemChat`, `GemChatMessage`, and `GemChatSource` primitives
  (package `mc.fenix.chat`), published together as `@montigem/chat`. Component styles are bundled with the library.

`Conversation` manages a `MessageHistory` and sends messages through `SenderService`.
`GemChat` provides the message area and input field for composing a custom chat UI.

## Usage

Add `montigemModule` and `montigemModuleUMLPModels` dependencies on `umlp.modules:chat` to the
backend. Include `configurations.montigemModuleUMLPModels` in the `umlp2java` task's `modelDir`.
The frontend needs `montigemModuleUMLPModels`, `montigemModuleUMLPSymbols`,
`montigemModuleGUISymbols`, and `montigemModuleImportMap` dependencies on the same module.
Add the UMLP symbols configuration to the `gui2java` task's `symbolPath`, and install
`@montigem/chat` and its runtime peer dependencies.
See [the dashboard example](../../examples/dashboard) for project dependencies and source linking.

Import `ChatModule.*` in the application's domain model to compose it with the chat domain.
Create a conversation using `ChatModuleManager.messageHistoryBuilder().name("Chat").build().get()`.
Pass it to the component:

```java
import ChatModule.MessageHistory;
import chatcomponents.Conversation;

component ChatPanel(MessageHistory history) {
  chat@Conversation(msgHistory = history);
}
```

Register the backend handler during application startup:

```java
MessageSender.setHandler((message, history, contextGemId) -> {
  // Call the application's model provider and append replies using ChatModuleManager.messageBuilder().
  // history.addMessages(reply) synchronizes the reply to connected clients.
});
```

`contextGemId` is optional and defaults to `0`. An application can pass an object ID to the
component and resolve it in its handler, as the dashboard example does for the selected view.
The module imposes no context type, model provider, tools, or system instructions.
The backend includes LangChain4j; applications construct their own `ChatModel` and add the
provider integration dependency they need.
The handler runs on the backend; missing registration fails the command and restores the input.

`MessageHistory.modelMessages` stores the model transcript as typed `ModelMessage` records.
Each record has a `ModelMessageRole`, an explicit sequence number, optional content and thinking text,
and either a list of `ToolCall` records or a `ToolResult`. Calls store their sequence within an assistant
message, external call ID, tool name, and JSON arguments. Results store the matching call ID, tool name,
content, and optional error status. The generated DAOs persist these records and associations.
`MessageHistory.synchronizedMessageCount` tracks the visible messages already included in the transcript.

The module's `PersistedChatMemory` maps these records to LangChain4j messages, saves each exchange,
and restores messages and batched calls in sequence order. Provider implementations remain application dependencies.
The UI continues to render only `MessageHistory.messages`.

The component handles user messages, waiting state, reply timestamps, Markdown, and source links.
Its `placeholder`, `sendLabel`, `waitingLabel`, and `sourcesLabel` parameters customize the text.

## LangChain4j service

`chatmodule.ChatService` accepts an application-created `ChatModel` and system instruction text.
Its `send(history, tools...)` method accepts tools for each request, so context can change between
requests without sharing tools between conversations. Calls without tools are supported.
The service restores the complete persisted transcript, including tool calls and results, and
appends the final reply to the synchronized visible messages. Model failures produce a visible
error reply. Requests on the same history object are serialized.

```java
String instructions = AssistantInstructions.load(MyApplication.class.getClassLoader(), "chat/instructions.list");
ChatService service = new ChatService(model, instructions);
MessageSender.setHandler((message, history, contextGemId) ->
    service.send(history, new ApplicationTools(contextGemId)));
```

`AssistantInstructions.load(classLoader, manifest)` composes UTF-8 classpath resources in manifest
order. Blank lines and `#` comments are ignored; missing or empty resources fail immediately.
Applications own the manifest, persona, model construction, and context resolution.
