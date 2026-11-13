package ma2fustrumo.pojo;

import ma2fustrumo.pojo.values.SimpleValue;

import java.util.function.Consumer;

public final class SimpleFuStruMoVisitor {

  private SimpleFuStruMoVisitor() {
  }

  public static void forEachFunction(SimpleFunction startFunction,
                                     Consumer<SimpleFunction> action) {
    if (startFunction == null) {
      return;
    }
    if (startFunction.getSubfunctions() != null) {
      for (SimpleFunction subfunction : startFunction.getSubfunctions().values()) {
        forEachFunction(subfunction, action);
      }
    }
    action.accept(startFunction);
  }


  public static void forEachPort(SimpleFunction startFunction, Consumer<SimplePort> action) {
    forEachFunction(startFunction, function -> {
      if (function.getInPorts() != null) {
        function.getInPorts().forEach(action);
      }
      if (function.getOutPorts() != null) {
        function.getOutPorts().forEach(action);
      }
    });
  }

  public static void forEachState(SimpleFunction startFunction, Consumer<SimpleState> action) {
    forEachFunction(startFunction, function -> {
      if (function.getStates() != null) {
        function.getStates().forEach(action);
      }
    });
  }

  public static void forEachChannel(SimpleFunction startFunction, Consumer<SimpleChannel> action) {
    forEachPort(startFunction, port -> {
      if (port.getChannel() != null) {
        action.accept(port.getChannel());
      }
    });
  }

  public static void forEachFunctionStream(SimpleFunction startFunction,
                                           Consumer<SimpleFunctionStream> action) {
    forEachState(startFunction, state -> {
      if (state.getFunctionStream() != null) {
        action.accept(state.getFunctionStream());
      }
    });

    forEachChannel(startFunction, channel -> {
      if (channel.functionStream != null) {
        action.accept(channel.getFunctionStream());
      }
    });
  }

  public static void forEachCategory(SimpleFunction startFunction,
                                     Consumer<SimpleCategory> action) {
    // Categories from Channels (via Ports)
    forEachChannel(startFunction, channel -> {
      if (channel.getCategory() != null) {
        action.accept(channel.getCategory());
      }
    });

    // Categories from States
    forEachState(startFunction, state -> {
      if (state.getCategory() != null) {
        action.accept(state.getCategory());
      }
    });

    // Categories from FunctionStreams (via States)
    forEachFunctionStream(startFunction, functionStream -> {
      if (functionStream.getCategory() != null) {
        action.accept(functionStream.getCategory());
      }
    });
  }

  public static void forEachKind(SimpleFunction startFunction, Consumer<SimpleKind> action) {
    forEachCategory(startFunction, category -> {
      if (category.getKind() != null) {
        action.accept(category.getKind());
      }
    });
  }

  public static void forEachType(SimpleFunction startFunction, Consumer<SimpleType> action) {
    forEachCategory(startFunction, category -> {
      if (category.getType() != null) {
        action.accept(category.getType());
      }
    });
  }

  public static void forEachValue(SimpleFunction startFunction, Consumer<SimpleValue> action) {
    forEachFunctionStream(startFunction, functionStream -> {
      if (functionStream.getValues() != null) {
        functionStream.getValues().forEach(action);
      }
    });
  }
}
