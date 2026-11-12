# Collaborative text module

A text area in which several users edit the same plain text at the same time. Changes appear live
for all of them, and each user sees the cursors and selections of the others. The text is
synchronized with the MontiGem commands, hence the component is part of a module with its own
domain model. The module has the same structure as the chat module.

- `models/CollabTextModule.umlp`: `CollabText` with its `CollabTextChunk`s and `CollabTextCursor`s,
  and `CollabTextService`.
- `backend`: the `ApplyEdit` command, `CollabTextSync`, which applies the edits of all users one
  after another, and `TextEdit`, which is also transpiled for the frontend.
- `frontend/collabtext`: `collabtextcomponents.CollabTextarea` and the primitives
  `GemCollabTextarea` and `GemCollabTextCursor` (package `mc.fenix.collabtext`), published
  together as `@montigem/collabtext`.

Only plain text is supported, there is no styling of the text.

## Usage

The dependencies are those of the [chat module](../chat/README.md#usage), with
`umlp.modules:collabtext` and `@montigem/collabtext`.
Import `CollabTextModule.*` in the domain model of the application.
Create a text in the backend and pass it to the component:

```java
CollabText notes = CollabTextModuleManager.collabTextBuilder().build().get();
CollabTextSync.apply(notes, new TextEdit(0, 0, "Initial content"));
```

```java
import CollabTextModule.CollabText;
import collabtextcomponents.CollabTextarea;

component Notes(CollabText notes, String userName) {
  editor@CollabTextarea(text = notes, userName = userName);
}
```

`userName` labels the cursor of the user for the others, `userColor` sets its color. Without it,
each user gets one of six colors. The component fills its parent, `placeholder` and `disabled`
are passed to the text area. `showChunks = true` shades the chunks of the text alternately, to
show how the text is stored and which chunk an edit changes.

The text is stored as `CollabText.chunks`, pieces of at most 512 characters. `text.getContent()`
joins them and `text.getLength()` gives the length without reading them. Do not change the chunks
or `lastEdit` directly: the backend changes a text with
`CollabTextSync.apply(text, new TextEdit(position, deleteCount, inserted))`, which editors that
have the text open follow like the edit of a user.

## Synchronization

Setting the whole text on every key press would make users overwrite each other, and store and
send the whole text every time. Instead, the text area reports each change as a `TextEdit`: delete
some characters at a position and insert others.

1. The editor shows the edit immediately and sends it with the `ApplyEdit` command, together with
   the revision of the text it was made on. It sends one edit at a time, edits made in the
   meantime wait and are merged if they adjoin, e.g. typed characters.
2. `CollabTextSync` rebases the edit on the edits of other users it has not seen
   (`TextEdit.rebase`) and changes the chunks the edit touches: typing sets the content of one
   chunk, a chunk that gets too long is split, an empty one is removed. Then it sets `lastEdit`,
   which holds the new revision, the author, the new length of the text and the rebased edit.
   `lastEdit` is an ordinary attribute, so all clients get the edits in the same order.
3. An editor reads the chunks once, when it opens the text. Afterwards it applies the published
   edits to its copy of the text, and rebases its own unconfirmed edits on those of other users.
   Its own edit coming back confirms it, the next one is sent. If its copy does not have the
   published length, it reads the chunks again. It follows the lengths of the chunks the same way:
   `ChunkChange` decides how an edit changes the chunks, in the backend and in the editor.

So an edit costs one command to the backend, and one small update of a chunk and of `lastEdit`
for the database and each client, independent of the length of the text.

Two edits at the same place both take effect. An edit within a range that another user replaces at
the same time is dropped. The backend keeps the last 1000 edits of a text in memory. If it cannot
rebase an edit, e.g. after a restart, the command fails and the editor reads the text again,
without the edits of the user that were not confirmed yet.

Each editor adds a `CollabTextCursor` to the text and removes it when it is closed. `anchor` and
`head` are sent when the user moves the cursor or selects text. With edits, every client moves the
cursors itself. The backend removes the cursors of users whose connection is closed with the next edit.
