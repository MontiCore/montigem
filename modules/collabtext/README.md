# Collaborative text module

A text area in which several users edit the same plain text at the same time. Changes appear live
for all of them, and each user sees the cursors and selections of the others. The text is
synchronized through the domain model's generated observers and mutations, so the component is
part of a module with its own domain model. The module has the same structure as the chat module.

- `models/CollabTextModule.umlp`: `CollabText` with its chunks and cursors.
- `backend`: `CollabText`, which reads and changes chunks using generated mutations and is also
  transpiled for the frontend.
- `frontend/collabtext`: `GemCollabTextarea` and `GemCollabTextCursor`
  (package `mc.fenix.collabtext`), published
  together as `@montigem/collabtext`.

Only plain text is supported, there is no styling of the text.

The standalone [collaborative text example](../../examples/collabtext) demonstrates
the component with a shared text and one cursor per browser connection.

## Usage

The dependencies are those of the [chat module](../chat/README.md#usage), with
`umlp.modules:collabtext` and `@montigem/collabtext`.
Import `CollabTextModule.*` in the domain model of the application.
Create a text in the backend and pass it to the component:

```java
CollabText notes = CollabTextModuleManager.collabTextBuilder().build().get();
notes.replace(0, 0, "Initial content");
```

```java
import CollabTextModule.CollabText;
import mc.fenix.collabtext.GemCollabTextarea;

component Notes(CollabText notes, String userName) {
  editor@GemCollabTextarea(text = notes, userName = userName);
}
```

`userName` labels the cursor of the user for the others, `userColor` sets its color. Without it,
each user gets one of six colors. The component fills its parent, `placeholder` and `disabled`
are passed to the text area. `showChunks = true` shades the chunks of the text alternately, to
show how the text is stored and which chunk an edit changes.

The text is stored as `CollabText.chunks`. `text.getContent()` joins their current contents and
`text.getLength()` sums their lengths. `text.setContent(value)` changes the affected chunk contents
and associations, splitting or removing chunks as needed. Only those generated mutations are
synchronized; the assembled string is local to the editor.

## Synchronization

`GemCollabTextarea` receives the `CollabText` and creates the current user's `CollabTextCursor`.
Typing sets the text's content; selection changes set the cursor's anchor and head attributes.
Generated observers synchronize the data. The text area uses generated observers to refresh its
rendering when chunk contents or associations change.

There are no edit events, change payload classes, edit histories, revision counters, acknowledgements,
or rebasing. Concurrent changes use the standard synchronization mechanism directly. A pending
browser input value only serializes local asynchronous setter calls.

`GemCollabTextarea` removes the user's cursor when leaving a text. It draws the chunks, cursors,
and selections from the current domain data.
