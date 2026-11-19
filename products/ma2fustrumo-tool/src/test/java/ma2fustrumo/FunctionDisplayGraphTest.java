package ma2fustrumo;

import ma2fustrumo.pojo.SimpleChannel;
import ma2fustrumo.pojo.SimpleFunction;
import ma2fustrumo.pojo.SimplePort;
import mc.fenix.functiondisplay.functionstructure4fenix.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import umlp.backendrte.common.BaseClassObjectMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class FunctionDisplayGraphTest {
  private static final String TYPE = Function4Display.class.getName();

  @BeforeEach
  void registerSerializers() {
    de.se_rwth.commons.logging.LogStub.init();
    BaseClassObjectMapper.addDeSers(FunctionStructure4FenixDeSer.getInstance().getDeSerMap());
  }

  private Function4Display transform() {
    SimpleChannel channel = new SimpleChannel(null, null);
    SimpleFunction root = new SimpleFunction("Root", "root", "root");
    SimpleFunction child = new SimpleFunction("Child", "root.child", "child");
    root.addSubfunction("child", child);
    SimplePort input = new SimplePort("root.input", false);
    SimplePort output = new SimplePort("root.child.output", true);
    input.setChannel(channel);
    output.setChannel(channel);
    root.addIn(input);
    child.addOut(output);
    return new FuStruMo2FuStruMo4DisplayTransformer().transform(root);
  }

  private void assertGraph(Function4Display root) {
    assertEquals("root", root.getName());
    assertEquals(1, root.sizeSubFunction());
    Function4Display child = root.getSubFunction(0);
    assertSame(root, child.getOwner().orElseThrow());
    assertTrue(root.getOwner().isEmpty());
    Port4Display input = root.getIn(0);
    Port4Display output = child.getOut(0);
    assertEquals("input", input.getName());
    assertEquals("output", output.getName());
    assertSame(root, input.getOwnerIn().orElseThrow());
    assertTrue(input.getOwnerOut().isEmpty());
    assertSame(child, output.getOwnerOut().orElseThrow());
    assertSame(input.getChannel4Display(), output.getChannel4Display());
    assertEquals(2, input.getChannel4Display().sizePort4Display());
    assertTrue(input.getChannel4Display().containsPort4Display(input));
    assertTrue(input.getChannel4Display().containsPort4Display(output));
  }

  @Test
  void transformerAndMapperPreserveCyclesAndSharedChannels() {
    Function4Display original = transform();
    assertGraph(original);
    String json = BaseClassObjectMapper.serialize(original, TYPE).orElseThrow();
    assertNotNull(de.monticore.symboltable.serialization.JsonParser.parse(json), json);
    assertTrue(json.contains("\"$ref\""));
    Function4Display copy = BaseClassObjectMapper.<Function4Display>deserialize(json, TYPE).orElseThrow();
    assertNotSame(original, copy);
    assertGraph(copy);
    assertGraph(BaseClassObjectMapper.<Function4Display>deserialize(
        BaseClassObjectMapper.serialize(copy, TYPE).orElseThrow(), TYPE).orElseThrow());
  }

  @Test
  void settersBuildersAndListReplacementMaintainBothEnds() {
    Function4Display first = new Function4DisplayBuilder().name("first").build().orElseThrow();
    Function4Display second = new Function4DisplayBuilder().name("second").build().orElseThrow();
    Function4Display child = new Function4DisplayBuilder().name("child")
        .owner(Optional.of(first)).build().orElseThrow();
    assertSame(child, first.getSubFunction(0));
    second.addSubFunction(child);
    assertTrue(first.isEmptySubFunction());
    assertSame(second, child.getOwner().orElseThrow());
    assertFalse(second.addSubFunction(child));
    second.setSubFunctionList(second.getSubFunctionList());
    assertSame(second, child.getOwner().orElseThrow());
    second.removeSubFunction(child);
    assertTrue(child.getOwner().isEmpty());
    first.setSubFunctionList(Arrays.asList(child, child));
    assertEquals(1, first.sizeSubFunction());
    child.setOwner(Optional.empty());
    assertTrue(first.isEmptySubFunction());
    assertThrows(UnsupportedOperationException.class, () -> first.getSubFunctionList().add(child));

    Channel4Display c1 = new Channel4DisplayBuilder().build().orElseThrow();
    Channel4Display c2 = new Channel4DisplayBuilder().build().orElseThrow();
    Port4Display port = new Port4DisplayBuilder().name("p").channel4Display(c1).build().orElseThrow();
    assertSame(port, c1.getPort4Display(0));
    c2.addPort4Display(0, port);
    assertTrue(c1.isEmptyPort4Display());
    assertSame(c2, port.getChannel4Display());
    c2.setPort4DisplayList(Collections.emptyList());
    assertNull(port.getChannel4Display());
  }

  @Test
  void graphContextsAreIndependentAndRecoverAfterInvalidInput() {
    Function4Display root = transform();
    String json = BaseClassObjectMapper.serialize(root, TYPE).orElseThrow();
    assertThrows(IllegalArgumentException.class,
        () -> new Function4DisplayDeSer().deserialize("{\"$ref\":\"missing\"}"));
    assertGraph(new Function4DisplayDeSer().deserialize(json).orElseThrow());
    CompletableFuture<Function4Display> a = CompletableFuture.supplyAsync(
        () -> new Function4DisplayDeSer().deserialize(json).orElseThrow());
    CompletableFuture<Function4Display> b = CompletableFuture.supplyAsync(
        () -> new Function4DisplayDeSer().deserialize(json).orElseThrow());
    assertGraph(a.join());
    assertGraph(b.join());
    assertNotSame(a.join(), b.join());
  }

  @Test
  void callbackSerializerRemainsSupported() {
    FunctionAction action = new FunctionActionBuilder().label("Open").functionClick(f -> {}).build().orElseThrow();
    FunctionAction copy = new FunctionActionDeSer().deserialize(new FunctionActionDeSer().serialize(action)).orElseThrow();
    assertEquals("Open", copy.getLabel());
    assertNotNull(copy.getFunctionClick());
  }
}
