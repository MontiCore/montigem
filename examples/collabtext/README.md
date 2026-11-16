# Collaborative text example

Standalone demo of the [collaborative text module](../../modules/collabtext).
The `home/CollabDemo` page shows a shared text area with live cursors, selections,
and shaded chunks. Each browser connection gets its own user name.
The editor fills the available page space. Use **Share** to open a QR code for
the current page or copy its link to the clipboard.

```sh
gradle :examples:collabtext:backend:run
```

In another terminal, start the frontend:

```sh
gradle :examples:collabtext:frontend:run
```

Open <http://localhost:4200/home/CollabDemo> in two browser windows and type in both.
