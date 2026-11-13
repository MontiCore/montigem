# Ma2FuStruMoTool

A MontiGem tool to convert a MontiArc diagram into a simplified function structure model (`SimpleFunction`, `SimpleChannel`, etc...) and further process that into a FunctionStructure4Fenix for display model.

## Usage

You can use this tool by adding it as a dependency to your project and passing it some MontiArc code from your backend.
Use the [`SimpleMontiArc2Function4DisplayConverter#convert`](src/main/java/ma2fustrumo/SimpleMontiArc2Function4DisplayConverter.java) method for that.
Check out the [demo code](../../examples/montiarc2function4display-test/backend/src/main/java/montiarcvizexperiment/commands/Render.java) to see an example of how that can look.

Use the `Function4Display` result of that, to render it in your frontend gui models with the [FunctionDisplay](../../umlp-rte/frontend/functiondisplay/src/main/gui/mc/fenix/functiondisplay/FunctionDisplay.gui) component.

Alternatively, you can run the tool directly from the command line.
Use the native help commands available there for details on usage and parameters.

### Demo

There is an interactive demo for this tool available [here](../../examples/montiarc2function4display-test/README.md).
