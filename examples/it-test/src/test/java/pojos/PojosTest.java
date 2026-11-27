/* (c) https://github.com/MontiCore/monticore */

package pojos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pojos.commands.GetPojoTests;
import umlp.backendrte.command.Command;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.command.CommandObjectMapper;
import umlp.backendrte.command.CommandResult;
import umlp.backendrte.command.ListResult;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PojosTest {

  @BeforeEach
  public void setUp(){
    PojosMock.init();
  }

  @Test
  public void testAppExecuteCommandWithPojoResult(){
    ListResult<TestPojo, List<TestPojo>> result = App.executeGetPojoTests(3);

    assertTrue(result.isSuccessful());
    List<TestPojo> pojos = result.get();
    assertEquals(3, pojos.size());
    // Verify the "simple" values
    assertEquals(0, pojos.get(0).getA());
    assertEquals(1, pojos.get(1).getA());
    assertEquals(2, pojos.get(2).getA());

    // Check for the associated domain values
    assertEquals(2, pojos.get(0).getFooDomainsList().size());
    assertEquals(2, pojos.get(1).getFooDomainsList().size());
    assertEquals(2, pojos.get(2).getFooDomainsList().size());

    // Verify that each has the same domain-values
    assertEquals(pojos.get(0).getFooDomainsList().get(0).getGemId(), pojos.get(1).getFooDomainsList().get(0).getGemId());
    assertEquals(pojos.get(0).getFooDomainsList().get(1).getGemId(), pojos.get(1).getFooDomainsList().get(1).getGemId());

    assertEquals(pojos.get(0).getFooDomainsList().get(0).getGemId(), pojos.get(2).getFooDomainsList().get(0).getGemId());
    assertEquals(pojos.get(0).getFooDomainsList().get(1).getGemId(), pojos.get(2).getFooDomainsList().get(1).getGemId());
  }

  @Test
  public void testPojoCommandSerializationRoundtrip(){
    GetPojoTests cmd = new GetPojoTests(3);
    String cmdJson = CommandObjectMapper.serializeCommand(cmd);
    Command<?> deserializedCmd = CommandObjectMapper.deserializeCommand(cmdJson).orElseThrow();
    assertTrue(deserializedCmd instanceof GetPojoTests);
    CommandResult<?> result = CommandManager.executeCommand(deserializedCmd);

    String resultJson = CommandObjectMapper.serializeResponse(result);
    Optional<CommandResult<?>> deserializedResult = CommandObjectMapper.deserializeResponse(resultJson);

    assertTrue(deserializedResult.isPresent());
    assertTrue(deserializedResult.get().isListResult());
    assertEquals(TestPojo.class.getName(),  deserializedResult.get().asListResult().getTypeParameter());

    ListResult<TestPojo, List<TestPojo>> listResult = (ListResult) deserializedResult.get().asListResult();
    assertTrue(listResult.isSuccessful());
    assertEquals(3, listResult.get().size());
  }
}
