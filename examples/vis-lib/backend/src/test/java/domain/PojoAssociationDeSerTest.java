/* (c) https://github.com/MontiCore/monticore */
package domain;

import mc.fenix.functiondisplay.functionstructure4fenix.Channel4Display;
import mc.fenix.functiondisplay.functionstructure4fenix.Channel4DisplayBuilder;
import mc.fenix.functiondisplay.functionstructure4fenix.Channel4DisplayDeSer;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4Display;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4DisplayBuilder;
import mc.fenix.functiondisplay.functionstructure4fenix.Function4DisplayDeSer;
import mc.fenix.functiondisplay.functionstructure4fenix.FunctionStructure4FenixDeSer;
import mc.fenix.functiondisplay.functionstructure4fenix.Port4Display;
import mc.fenix.functiondisplay.functionstructure4fenix.Port4DisplayBuilder;
import mc.fenix.functiondisplay.functionstructure4fenix.Port4DisplayDeSer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import umlp.backendrte.command.DeSer;
import umlp.backendrte.command.JsonDeSer;
import umlp.backendrte.common.BaseClassObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Round trips real bidirectional associations from the generated POJO CD. */
class PojoAssociationDeSerTest {
  private Map<String, DeSer> previousDeSers;

  @BeforeEach
  void registerGeneratedDeSers() {
    previousDeSers = new HashMap<>(BaseClassObjectMapper.getInstance().getBaseDeSers());
    BaseClassObjectMapper.addDeSers(FunctionStructure4FenixDeSer.getInstance().getDeSerMap());
  }

  @AfterEach
  void restoreDeSers() {
    Map<String, DeSer> registered = BaseClassObjectMapper.getInstance().getBaseDeSers();
    registered.clear();
    registered.putAll(previousDeSers);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void roundTripsFunctionPortAssociationFromFunction(boolean input) {
    Function4Display function = function("Function");
    Port4Display port = port("Port", new Channel4DisplayBuilder().build().orElseThrow());
    if (input) {
      function.addIn(port);
    } else {
      function.addOut(port);
    }

    Function4Display restored = roundTrip(function, new Function4DisplayDeSer());
    List<Port4Display> ports = input ? restored.getInList() : restored.getOutList();
    assertEquals(1, ports.size());
    Port4Display restoredPort = ports.get(0);
    assertNotSame(port, restoredPort);
    assertEquals("Port", restoredPort.getName());
    assertSame(restored, (input ? restoredPort.getOwnerIn() : restoredPort.getOwnerOut()).orElseThrow());
    assertTrue((input ? restoredPort.getOwnerOut() : restoredPort.getOwnerIn()).isEmpty());
    assertTrue((input ? restored.getOutList() : restored.getInList()).isEmpty());
    assertEquals(1, restoredPort.getChannel4Display().getPort4DisplayList().size());
    assertSame(restoredPort, restoredPort.getChannel4Display().getPort4Display(0));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void roundTripsFunctionPortAssociationFromPort(boolean input) {
    Function4Display function = function("Function");
    Port4Display port = port("Port", new Channel4DisplayBuilder().build().orElseThrow());
    if (input) {
      port.setOwnerIn(Optional.of(function));
    } else {
      port.setOwnerOut(Optional.of(function));
    }

    Port4Display restored = roundTrip(port, new Port4DisplayDeSer());
    Function4Display restoredOwner = (input ? restored.getOwnerIn() : restored.getOwnerOut()).orElseThrow();
    assertNotSame(function, restoredOwner);
    assertEquals("Function", restoredOwner.getName());
    List<Port4Display> ports = input ? restoredOwner.getInList() : restoredOwner.getOutList();
    assertEquals(1, ports.size());
    assertSame(restored, ports.get(0));
    assertTrue((input ? restored.getOwnerOut() : restored.getOwnerIn()).isEmpty());
    assertSame(restored, restored.getChannel4Display().getPort4Display(0));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void roundTripsBidirectionalCompositionFromEitherEnd(boolean parentRoot) {
    Function4Display parent = function("Parent");
    Function4Display child = function("Child");
    Function4Display sibling = function("Sibling");
    parent.addSubFunction(child);
    sibling.setOwner(Optional.of(parent));

    Function4Display restored = roundTrip(parentRoot ? parent : child, new Function4DisplayDeSer());
    Function4Display restoredParent = parentRoot ? restored : restored.getOwner().orElseThrow();
    assertNotSame(parent, restoredParent);
    assertEquals("Parent", restoredParent.getName());
    assertTrue(restoredParent.getOwner().isEmpty());
    assertEquals(2, restoredParent.getSubFunctionList().size());
    Function4Display restoredChild = restoredParent.getSubFunction(0);
    Function4Display restoredSibling = restoredParent.getSubFunction(1);
    assertEquals("Child", restoredChild.getName());
    assertEquals("Sibling", restoredSibling.getName());
    assertNotSame(restoredChild, restoredSibling);
    assertSame(restoredParent, restoredChild.getOwner().orElseThrow());
    assertSame(restoredParent, restoredSibling.getOwner().orElseThrow());
    if (!parentRoot) {
      assertSame(restored, restoredChild);
    }
    assertTrue(restoredChild.getSubFunctionList().isEmpty());
    assertTrue(restoredSibling.getSubFunctionList().isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"source", "target", "channel"})
  void preservesSharedChannelAndBothFunctionOwners(String root) {
    Function4Display source = function("Source");
    Function4Display target = function("Target");
    Channel4Display channel = new Channel4DisplayBuilder().lineColor("red").build().orElseThrow();
    Port4Display output = port("Output", channel);
    Port4Display input = port("Input", channel);
    source.addOut(output);
    target.addIn(input);

    Channel4Display restoredChannel;
    if ("source".equals(root)) {
      Function4Display restoredSource = roundTrip(source, new Function4DisplayDeSer());
      restoredChannel = restoredSource.getOut(0).getChannel4Display();
      assertSame(restoredSource, restoredChannel.getPort4Display(0).getOwnerOut().orElseThrow());
    } else if ("target".equals(root)) {
      Function4Display restoredTarget = roundTrip(target, new Function4DisplayDeSer());
      restoredChannel = restoredTarget.getIn(0).getChannel4Display();
      assertSame(restoredTarget, restoredChannel.getPort4Display(1).getOwnerIn().orElseThrow());
    } else {
      restoredChannel = roundTrip(channel, new Channel4DisplayDeSer());
    }

    assertNotSame(channel, restoredChannel);
    assertEquals("red", restoredChannel.getLineColor());
    assertEquals(2, restoredChannel.getPort4DisplayList().size());
    Port4Display restoredOutput = restoredChannel.getPort4Display(0);
    Port4Display restoredInput = restoredChannel.getPort4Display(1);
    assertEquals("Output", restoredOutput.getName());
    assertEquals("Input", restoredInput.getName());
    assertNotSame(restoredOutput, restoredInput);
    assertSame(restoredChannel, restoredOutput.getChannel4Display());
    assertSame(restoredChannel, restoredInput.getChannel4Display());
    Function4Display restoredSource = restoredOutput.getOwnerOut().orElseThrow();
    Function4Display restoredTarget = restoredInput.getOwnerIn().orElseThrow();
    assertNotSame(restoredSource, restoredTarget);
    assertEquals("Source", restoredSource.getName());
    assertEquals("Target", restoredTarget.getName());
    assertEquals(1, restoredSource.getOutList().size());
    assertEquals(1, restoredTarget.getInList().size());
    assertSame(restoredOutput, restoredSource.getOut(0));
    assertSame(restoredInput, restoredTarget.getIn(0));
    assertTrue(restoredOutput.getOwnerIn().isEmpty());
    assertTrue(restoredInput.getOwnerOut().isEmpty());
  }

  @Test
  void roundTripsAbsentAssociationEnds() {
    Function4Display restored = roundTrip(function("Isolated"), new Function4DisplayDeSer());
    assertTrue(restored.getInList().isEmpty());
    assertTrue(restored.getOutList().isEmpty());
    assertTrue(restored.getOwner().isEmpty());
    assertTrue(restored.getSubFunctionList().isEmpty());
    Port4Display restoredPort = roundTrip(
        port("Unowned", new Channel4DisplayBuilder().build().orElseThrow()), new Port4DisplayDeSer());
    assertTrue(restoredPort.getOwnerIn().isEmpty());
    assertTrue(restoredPort.getOwnerOut().isEmpty());
    assertEquals(1, restoredPort.getChannel4Display().getPort4DisplayList().size());
    assertSame(restoredPort, restoredPort.getChannel4Display().getPort4Display(0));
  }

  private Function4Display function(String name) {
    return new Function4DisplayBuilder().name(name).build().orElseThrow();
  }

  private Port4Display port(String name, Channel4Display channel) {
    return new Port4DisplayBuilder().name(name).channel4Display(channel).build().orElseThrow();
  }

  private <T> T roundTrip(T original, JsonDeSer<T> deser) {
    T restored = deser.deserialize(deser.serialize(original)).orElseThrow();
    assertNotSame(original, restored);
    return restored;
  }
}
