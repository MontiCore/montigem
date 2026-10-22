# MontiArc -> Function4Display Demo

This project is a demo of how to use the [MA2FuStruMoTool](../../products/ma2fustrumo-tool/README.md).
MontiArc code can be entered into an interactive web-editor and the resulting component is rendered next to it, using the `Function4Display` representation.

Run the backend of the demo with

```bash
gradle :examples:montiarc2function4display-test:backend:bootRun
```

and then start the frontend via

```bash
gradle :examples:montiarc2function4display-test:frontend:runApp
```

The web app will run [on the default port](http://localhost:8080).
