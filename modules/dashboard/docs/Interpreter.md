# [Interpreter](https://git.rwth-aachen.de/monticore/umlp/-/blob/ma-kapidani-clean/examples/dashboard/lang/src/main/java/expressionwrapper/Interpreter.java?ref_type=heads)

The data for the visualizations is stored as a String of Java-like/MontiCore expression "code".
In order to create actual data matching the type and structure required by a visualization,
the data String is sent to an interpreter to read it, execute it and return the appropriate data.
This interpreter lies in the *lang* directory of the project.

The 2 main files of concern for the interpreter are: *[Interpreter](https://git.rwth-aachen.de/monticore/umlp/-/blob/ma-kapidani-clean/examples/dashboard/lang/src/main/java/expressionwrapper/Interpreter.java?ref_type=heads)* and *[ExpressionWrapper](https://git.rwth-aachen.de/monticore/umlp/-/blob/ma-kapidani-clean/examples/dashboard/lang/src/main/grammars/ExpressionWrapper.mc4?ref_type=heads)* files.

The basic idea of the interpreter is as following. There are multiple MontiCore grammars that
cover different groups of expressions that we want to translate. Among them there is one for
the Java expressions, one for lambda expressions, one for the simple types, one for function types and so on.
In *[ExpressionWrapper](https://git.rwth-aachen.de/monticore/umlp/-/blob/ma-kapidani-clean/examples/dashboard/lang/src/main/grammars/ExpressionWrapper.mc4?ref_type=heads)* file all current grammars the interpreter extends from are visible.

For specific types or libraries we need to manually give the imports to the interpreter to use in the
"code" translation. For that the import statements and class paths of those external libraries need to be added
at *IExpressionWrapperArtifactScope* and *addAvailableClassesToGlobalScope()* respectively in *[Interpreter](https://git.rwth-aachen.de/monticore/umlp/-/blob/ma-kapidani-clean/examples/dashboard/lang/src/main/java/expressionwrapper/Interpreter.java?ref_type=heads)*.
In case the number of visualizations is increased or changed appropriate changes to the imports and class
paths need to be added.

The "language" expected and used to create the data is a mix of Java regular code with MontiCore expressions.
The users can use all the DashboardManager functions to create or aggregate data however they want for their visualization.

For every type of visualization the interpreter is called in the backend through a designated MontiGem command
to its data type. Then in *[CellDialog](https://git.rwth-aachen.de/monticore/umlp/-/blob/ma-kapidani-clean/examples/dashboard/frontend/src/main/gui/components/CellDialog.gui?ref_type=heads)* component where the string is written and input by the user and evaluated,
the appropriate command to the visualization type stored in the cell's *[CellConfig](https://git.rwth-aachen.de/monticore/umlp/-/blob/ma-kapidani-clean/examples/dashboard/models/Dashboard.umlp?ref_type=heads)* is called. The interpreter
itself gives a feedback error message if it fail, which we use as part of validating the input at *[CellDialog](https://git.rwth-aachen.de/monticore/umlp/-/blob/ma-kapidani-clean/examples/dashboard/frontend/src/main/gui/components/CellDialog.gui?ref_type=heads)*.

In the case new visualizations are added, new commands need to implemented for its data type and than added to the
caller function in hte frontend TOP Java extension in *[CellDialog](https://git.rwth-aachen.de/monticore/umlp/-/blob/ma-kapidani-clean/examples/dashboard/frontend/src/main/gui/components/CellDialog.gui?ref_type=heads)*.

In the current state the interpreter error messages have a chance depending on the error to be
empty, pointing to the wrong direction and in general surely not nicely formatted. So debugging
in the current state without experience can be difficult.

