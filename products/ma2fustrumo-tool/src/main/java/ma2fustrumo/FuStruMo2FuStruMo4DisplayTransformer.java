package ma2fustrumo;

import ma2fustrumo.pojo.SimpleFunction;
import ma2fustrumo.pojo.SimplePort;
import mc.fenix.functiondisplay.functionstructure4fenix.Channel4Display;
import mc.fenix.functiondisplay.functionstructure4fenix.Channel4DisplayBuilder;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4DisplayBuilder;
import mc.fenix.functiondisplay.functionstructure4fenix.Port4Display;
import mc.fenix.functiondisplay.functionstructure4fenix.Port4DisplayBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FuStruMo2FuStruMo4DisplayTransformer {

  protected static final String[] CHANNEL_COLORS = {
      "#00549F",
      "#57ab27",
      "#646567",
      "#cc071e",
      "#E30066",
      "#f6a800",
      "#11750e",
      "#FFED00",
      "#bdcd00",
      "#006165",
      "#0098A1",
      "#A11035",
      "#612158",
      "#7A6FAC",
      "#8EBAE5",
      "#555555",
      "#f3cdbb",
      "#F19EB1",
      "#FFF59B",
      "#BFD0D1",
      "#CD8B87",
  };

  protected final Map<Long, String> channelColorMap = new HashMap<>();
  protected final Map<Long, Channel4Display> channelMap = new HashMap<>();
  protected int nextColorIndex = 0;

  public Function4Display transform(SimpleFunction function) {
    return transformFunction(function, Optional.empty(), 2);
  }

  protected Function4Display transformFunction(SimpleFunction function,
                                               Optional<Function4Display> owner,
                                               int depth) {
    var builder = new Function4DisplayBuilder()
        .borderColor("#407fb7")
        .borderColorHover("#407fb7")
        .name(function.getName());
    if (owner.isPresent()) {
      builder.owner(owner);
    }

    for (var port : function.getInPorts()) {
      builder.addIn(wrapPort(port));
    }

    for (var port : function.getOutPorts()) {
      builder.addOut(wrapPort(port));
    }

    for (var subfunction : function.getSubfunctions().values()) {
      builder.addSubFunction(transformFunction(subfunction, Optional.of(builder), depth - 1));
    }

    return builder.build().orElseThrow();
  }

  protected Port4Display wrapPort(SimplePort port) {
    var channelId = port.getChannel().getId();
    var channelColor = getColor(channelId);
    var channel = getChannel(channelId);
    return new Port4DisplayBuilder()
        .name(port.getName())
        .borderColor(channelColor)
        .borderColorHover(channelColor)
        .channel4Display(channel)
        .build()
        .orElseThrow();
  }

  protected Channel4Display getChannel(long channelId) {
    return channelMap.computeIfAbsent(channelId, c -> {
      var channelColor = getColor(channelId);
      return new Channel4DisplayBuilder()
          .lineColor(channelColor)
          .lineColorHover(channelColor)
          .build()
          .orElseThrow();
    });
  }

  protected String getColor(long channelId) {
    return channelColorMap.computeIfAbsent(channelId, c -> {
      var colorIndex = nextColorIndex++ % CHANNEL_COLORS.length;
      return CHANNEL_COLORS[colorIndex];
    });
  }
}
