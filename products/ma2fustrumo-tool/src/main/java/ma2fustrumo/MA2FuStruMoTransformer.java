package ma2fustrumo;

import arcbasis._ast.ASTArcComponentType;
import arcbasis._ast.ASTArcField;
import arcbasis._ast.ASTArcPort;
import arcbasis._ast.ASTComponentInstance;
import arcbasis._ast.ASTComponentInstantiation;
import arcbasis._ast.ASTConnector;
import arcbasis._ast.ASTPortAccess;
import arcbasis._ast.ASTPortDeclaration;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.monticore.symbols.oosymbols.types3.OOSymbolsSymTypeRelations;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.monticore.types.check.SymTypePrimitive;
import de.monticore.umlstereotype._ast.ASTStereoValue;
import de.monticore.umlstereotype._ast.ASTStereotype;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import ma2fustrumo.pojo.SimpleCategory;
import ma2fustrumo.pojo.SimpleChannel;
import ma2fustrumo.pojo.SimpleData;
import ma2fustrumo.pojo.SimpleFunction;
import ma2fustrumo.pojo.SimpleFunctionStream;
import ma2fustrumo.pojo.SimplePort;
import ma2fustrumo.pojo.SimpleState;
import ma2fustrumo.pojo.SimpleType;
import ma2fustrumo.pojo.values.SimpleBooleanValue;
import ma2fustrumo.pojo.values.SimpleByteValue;
import ma2fustrumo.pojo.values.SimpleDoubleValue;
import ma2fustrumo.pojo.values.SimpleFloatValue;
import ma2fustrumo.pojo.values.SimpleIntegerValue;
import ma2fustrumo.pojo.values.SimpleLongValue;
import ma2fustrumo.pojo.values.SimpleShortValue;
import ma2fustrumo.pojo.values.SimpleStringValue;
import montiarc.MontiArcMill;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class MA2FuStruMoTransformer {
  protected static final String MQTT_NAMESPACE = "mqttNamespace";
  protected static final String MQTT = "mqtt";
  protected static final String MQTT_CONVERTER = "mqttConverter";

  protected final SimpleData data;
  protected Stack<String> stack;
  protected List<SimpleType> types;
  protected Map<String, SimpleCategory> categories;
  protected List<SimpleFunction> functions;
  protected List<SimplePort> ports;
  protected List<SimpleState> states;
  protected HashMap<String, SimpleChannel> channelMap;
  protected TagLookup tagLookup;

  public MA2FuStruMoTransformer() {
    channelMap = new HashMap<>();
    stack = new Stack<>();
    types = new ArrayList<>();
    functions = new ArrayList<>();
    ports = new ArrayList<>();
    categories = new HashMap<>();
    states = new ArrayList<>();
    data = new SimpleData();
    tagLookup = new TagLookup();
  }

  public SimpleFunction transform(ASTArcComponentType node) {
    SimpleFunction res = createFunction(node);
    markSpanningPorts(res);
    return res;
  }

  public String getQname() {
    String qname = "";
    for (String name : stack) {
      if (!qname.isEmpty()) qname += ".";
      qname += name;
    }
    return qname;
  }

  //Method to build Functions including its ports, states, and subfunctions
  public SimpleFunction createFunction(ASTArcComponentType node) {
    if (this.getQname().isEmpty()) {
      stack.push(StringTransformations.uncapitalize(node.getName()));
    }
    SimpleFunction function = new SimpleFunction(node.getSymbol().getFullName(),
                                                 getQname(),
                                                 stack.peek());
    tagLookup
        .getValuedTag(node.getSymbol(), MQTT_NAMESPACE)
        .ifPresent(value -> function.putMetadata(MQTT_NAMESPACE, value));
    if (node.isPresentStereotype()) {
      for (ASTStereoValue value : node.getStereotype().getValuesList()) {
        function.putMetadata(value.getName(), value.getValue());
      }
    }

    for (ASTConnector c : node.getConnectors()) {
      mapConnectorToChannel(c);
    }

    if (node.getSymbol().isDecomposed()) {
      // Collect stereotypes on instance level
      Map<ASTComponentInstance, ASTStereotype> stereotypes = new HashMap<>();
      for (ASTComponentInstantiation instantiation : node.getSubComponentInstantiations()) {
        if (instantiation.isPresentStereotype()) {
          for (ASTComponentInstance instance : instantiation.getComponentInstanceList()) {
            stereotypes.put(instance, instantiation.getStereotype());
          }
        }
      }

      for (ASTComponentInstance instance : node.getSubComponents()) {
        stack.push(instance.getName());
        SimpleFunction subfunction = createFunction((ASTArcComponentType) node
            .getSpannedScope()
            .resolveComponentType(instance.getSymbol().getType().getTypeInfo().getName())
            .get()
            .getAstNode());

        // Instance metadata overrides metadata inherited from the component type.
        tagLookup
            .getValuedTag(instance.getSymbol(), MQTT_NAMESPACE)
            .ifPresent(value -> subfunction.putMetadata(MQTT_NAMESPACE, value));

        // Add instance level stereotypes
        if (stereotypes.containsKey(instance)) {
          for (ASTStereoValue value : stereotypes.get(instance).getValuesList()) {
            subfunction.putMetadata(value.getName(), value.getValue());
          }
        }

        function.addSubfunction(instance.getName(), subfunction);
        stack.pop();
      }
    }

    for (ASTPortDeclaration decl : node.getPortDeclarations()) {
      List<ASTArcPort> arcPortList = decl.getArcPortList();
      for (ASTArcPort astPort : arcPortList) {
        SimplePort port = createPort(astPort);
        if (decl.isPresentStereotype()) {
          for (ASTStereoValue value : decl.getStereotype().getValuesList()) {
            port.putMetadata(value.getName(), value.getValue());
          }
        }

        if (astPort.getSymbol().isIncoming()) {
          function.addIn(port);
        } else {
          function.addOut(port);
        }
      }
    }

    for (ASTArcField field : node.getFields()) {
      function.addState(createState(field));
    }
    functions.add(function);
    if (getQname().isEmpty()) {
      stack.pop();
    }
    return function;
  }

  protected void markSpanningPorts(SimpleFunction function) {
    Map<String, SimplePort> fqnToPortMap = new HashMap<>();
    ports.forEach(p -> fqnToPortMap.put(p.getFqn(), p));

    Multimap<SimpleChannel, String> channelToPortFqns = ArrayListMultimap.create();
    channelMap.forEach((fqn, channel) -> channelToPortFqns.put(channel, fqn));
    Set<SimpleChannel> distinctChannels = new HashSet<>(channelMap.values());

    for (SimpleChannel channel : distinctChannels) {
      // Retrieve the actual port objects connected to this channel
      List<SimplePort> connectedPorts = channelToPortFqns
          .get(channel)
          .stream()
          .map(fqnToPortMap::get)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

      SimplePort spanningPort = null;

      List<SimplePort> outputs = connectedPorts
          .stream()
          .filter(SimplePort::isOutput)
          .collect(Collectors.toList());

      if (!outputs.isEmpty()) {
        // Case 1: Sub.Out -> Sub.In (Assembly)
        // Case 2: Sub.Out -> Super.Out (Delegation)
        // Rule: If output ports exist, the source is the one producing data.
        // If multiple outputs exist (Delegation), the source is the deepest nested one.
        spanningPort = outputs
            .stream()
            .max(Comparator.comparingInt(p -> p.getFqn().split("[.]").length))
            .orElse(null);
      } else {
        // Case 3: Super.In -> Sub.In (Delegation)
        // Rule: If NO output ports exist (only inputs), the source is the
        // boundary port receiving external data. This is the highest level port.
        spanningPort = connectedPorts
            .stream()
            .min(Comparator.comparingInt(p -> p.getFqn().split("[.]").length))
            .orElse(null);
      }

      if (spanningPort != null) {
        spanningPort.setSpansChannel(true);
      } else {
        Log.error("Can not find spanning port for channel connecting: " +
                  channelToPortFqns.get(channel));
      }
    }
  }


  public SimpleState createState(ASTArcField node) {
    SimpleState state = new SimpleState(node.getName());
    tagLookup
        .getValuedTag(node.getSymbol(), MQTT)
        .ifPresent(value -> state.putMetadata(MQTT, value));
    tagLookup
        .getValuedTag(node.getSymbol(), MQTT_CONVERTER)
        .ifPresent(value -> state.putMetadata(MQTT_CONVERTER, value));
    state.setCategory(createCategory(node.getSymbol().getType().getTypeInfo()));

    SymTypeExpression exp = node.getSymbol().getType();
    SimpleFunctionStream functionStream = null;
    try {
      String initial = MontiArcMill.prettyPrint(node.getInitial(), false);
      functionStream = new SimpleFunctionStream(state.getCategory());
      if (exp.isPrimitive()) {
        SymTypePrimitive primitive = exp.asPrimitive();
        String type = primitive.getPrimitiveName();
        LocalDateTime timestamp = LocalDateTime.now();
        if ("int".equals(type)) {
          int i = Integer.parseInt(initial);
          functionStream.addValue(new SimpleIntegerValue(timestamp, i));
        } else if ("byte".equals(type)) {
          byte b = Byte.parseByte(initial);
          functionStream.addValue(new SimpleByteValue(timestamp, b));
        } else if ("long".equals(type)) {
          long l = Long.parseLong(initial);
          functionStream.addValue(new SimpleLongValue(timestamp, l));
        } else if ("float".equals(type)) {
          float f = Float.parseFloat(initial);
          functionStream.addValue(new SimpleFloatValue(timestamp, f));
        } else if ("double".equals(type)) {
          double d = Double.parseDouble(initial);
          functionStream.addValue(new SimpleDoubleValue(timestamp, d));
        } else if ("short".equals(type)) {
          short s = Short.parseShort(initial);
          functionStream.addValue(new SimpleShortValue(timestamp, s));
        } else if ("boolean".equals(type)) {
          boolean bool = Boolean.parseBoolean(initial);
          functionStream.addValue(new SimpleBooleanValue(timestamp, bool));
        }
        //Assumption: if not primitive its a String
      } else {
        functionStream.addValue(new SimpleStringValue(LocalDateTime.now(), initial));
      }
      state.setFunctionStream(functionStream);
    } catch (Exception e) {
      Log.warn("Problem while creating state for " + node.getName(), e);
    }
    states.add(state);

    return state;
  }

  public SimpleCategory createCategory(TypeSymbol symbol) {
    SimpleCategory category;
    if (categories.containsKey(symbol.getFullName())) {
      category = categories.get(symbol.getFullName());
    } else {
      category = new SimpleCategory(Optional.of(false), data, createType(symbol));
      categories.put(symbol.getFullName(), category);
    }
    return category;
  }

  public SimpleType createType(TypeSymbol node) {
    SymTypeExpression symType = SymTypeExpressionFactory.createFromSymbol(node);
    SimpleType type = new SimpleType(node.getFullName(), OOSymbolsSymTypeRelations.isEnum(symType));
    types.add(type);
    return type;
  }

  public SimplePort createPort(ASTArcPort node) {
    String portFqn = getQname() + "." + node.getName();
    SimplePort port = new SimplePort(portFqn, node.getSymbol().isOutgoing());
    tagLookup
        .getValuedTag(node.getSymbol(), MQTT)
        .ifPresent(value -> port.putMetadata(MQTT, value));
    tagLookup
        .getValuedTag(node.getSymbol(), MQTT_CONVERTER)
        .ifPresent(value -> port.putMetadata(MQTT_CONVERTER, value));
    if (channelMap.containsKey(portFqn)) {
      port.setChannel(channelMap.get(portFqn));
    } else {
      // Unconnected port: it spans its own channel, so its stream exists in the function
      // structure and can be fed (e.g. over MQTT tags) or read before a consumer is
      // modeled — a component's interface may be ahead of its wiring.
      SimpleCategory category = createCategory(node.getSymbol().getType().getTypeInfo());
      SimpleChannel channel = new SimpleChannel(category, new SimpleFunctionStream(category));
      channelMap.put(portFqn, channel);
      port.setChannel(channel);
    }
    ports.add(port);
    return port;
  }

  public void mapConnectorToChannel(ASTConnector connector) {
    ASTPortAccess src = connector.getSource();
    for (ASTPortAccess tgt : connector.getTargetList()) {
      if (src.isPresentComponent() && tgt.isPresentComponent()) {
        createChannel(src, tgt);
      } else if (!src.isPresentComponent() && tgt.isPresentComponent()) {
        createChannel(tgt, src);
      } else if (src.isPresentComponent() && !tgt.isPresentComponent()) {
        createChannel(src, tgt);
      }
    }
  }

  public void createChannel(ASTPortAccess key, ASTPortAccess value) {
    if (channelMap.containsKey(this.getQname() + "." + value.getQName())) {
      if (channelMap.containsKey(this.getQname() + "." + key.getQName())) {
        SimpleChannel del = channelMap.get(this.getQname() + "." + value.getQName());
        for (Map.Entry<String, SimpleChannel> entry : channelMap.entrySet()) {
          if (entry.getValue().equals(del)) {
            channelMap.put(entry.getKey(), channelMap.get(this.getQname() + "." + key.getQName()));
          }
        }
      } else {
        channelMap.put(this.getQname() + "." + key.getQName(),
                       channelMap.get(this.getQname() + "." + value.getQName()));
      }
    } else if (channelMap.containsKey(this.getQname() + "." + key.getQName())) {
      channelMap.put(this.getQname() + "." + value.getQName(),
                     channelMap.get(this.getQname() + "." + key.getQName()));
    } else {
      SimpleCategory tmpCat = createCategory(key.getPortSymbol().getTypeInfo());
      SimpleChannel c = new SimpleChannel(tmpCat, new SimpleFunctionStream(tmpCat));
      channelMap.put(this.getQname() + "." + key.getQName(), c);
      channelMap.put(this.getQname() + "." + value.getQName(), c);
    }
  }
}
