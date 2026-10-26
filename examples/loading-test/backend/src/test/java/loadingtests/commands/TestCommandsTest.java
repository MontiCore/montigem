package loadingtests.commands;

import loadingtests.Animal;
import loadingtests.LoadingTestsManager;
import loadingtests.LoadingTestsServerApplication;
import loadingtests.LoadingTestsTestContext;
import loadingtests.MyEnum;
import loadingtests.TestCommandParams;
import loadingtests.service.TestClientWebSocketCommandCommunication;
import loadingtests.test_infrastructure.LoadingTestsMockTestSetup;
import loadingtests.util.PerformanceTestUtil;
import loadingtests.util.PerformanceTestUtil.TestResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.command.SimpleResult;
import umlp.backendrte.command.TestClientCommandManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the various commands
 */
@SpringBootTest(classes = LoadingTestsServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestCommandsTest extends LoadingTestsMockTestSetup {

  @LocalServerPort
  private int port;

  private String uri = "ws://localhost:%s/umlp/api/command";

  private LoadingTestsTestContext serverContext;

  private LoadingTestsTestContext clientContext;

  private TestClientWebSocketCommandCommunication localhost;

  public TestCommandsTest() {
    super(1);
  }

  @Override
  public void _init() throws Exception {
    super._init();

    this.serverContext = getBackend();
    this.clientContext = getClients(0);
    this.localhost = new TestClientWebSocketCommandCommunication(
        String.format(uri, port));
    this.localhost.setContext(serverContext, clientContext);
    TestClientCommandManager testClientCommandManager = new TestClientCommandManager(this.localhost);
    CommandManager.initMe(testClientCommandManager);
    clientContext.setCommandManager(testClientCommandManager);
  }

  @BeforeEach
  public void initBeforeEach() throws Exception {
    this._init();
    serverContext.activate();
    LoadingTestsManager.reset();
    clientContext.activate();
    LoadingTestsManager.reset();
  }

  @AfterEach
  public void tearDownAfterEach() throws Exception {
    if (this.localhost != null) {
      this.localhost.close();
    }
  }

  @Test
  public void checkCommandDomain() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_Domain(42);
    Assertions.assertEquals("42", checkRes(res));
  }

  @Test
  public void checkCommandDomainEnum() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_DomainEnum(MyEnum.B);
    Assertions.assertEquals("B", checkRes(res));
  }

  @Test
  public void checkCommandDomainEnumListCT() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_DomainEnumListCT(List.of(MyEnum.C, MyEnum.A));
    Assertions.assertEquals("[C, A]", checkRes(res));
  }

  @Test
  public void checkCommandDomainEnumListJU() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_DomainEnumListJU(List.of(MyEnum.C, MyEnum.A));
    Assertions.assertEquals("[C, A]", checkRes(res));
  }

  @Test
  public void checkCommandDomainEnumOpt_Absent() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_DomainEnumOpt(Optional.empty());
    Assertions.assertEquals("Optional.empty", checkRes(res));
  }

  @Test
  public void checkCommandDomainEnumOpt() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_DomainEnumOpt(Optional.of(MyEnum.B));
    Assertions.assertEquals("Optional[B]", checkRes(res));
  }

  @Test
  public void checkCommandDomainListCT() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_DomainListCT(List.of(1L,2L,3L));
    Assertions.assertEquals("[1, 2, 3]", checkRes(res));
  }

  @Test
  public void checkCommandDomainListJU() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_DomainListJU(List.of(1L,2L,3L));
    Assertions.assertEquals("[1, 2, 3]", checkRes(res));
  }

  @Test
  public void checkCommandDomainOpt_Absent() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_DomainOpt(Optional.empty());
    Assertions.assertEquals("Optional.empty", checkRes(res));
  }

  @Test
  public void checkCommandDomainOpt() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_DomainOpt(Optional.of(42L));
    Assertions.assertEquals("Optional[42]", checkRes(res));
  }

  @Test
  public void checkCommandPrimitive2_i() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_Primitive2_i(1, 4);
    Assertions.assertEquals("1/4", checkRes(res));
  }

  @Test
  public void checkCommandPrimitive_f() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_Primitive_f(6.5f);
    Assertions.assertEquals("6.5", checkRes(res));
  }

  @Test
  public void checkCommandPrimitive_i() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_Primitive_i(52);
    Assertions.assertEquals("52", checkRes(res));
  }

  @Test
  public void checkCommandPrimitive_W_i() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_PrimitiveW_i(5);
    Assertions.assertEquals("5", checkRes(res));
  }

  @Test
  @Disabled
  public void checkCommandPrimitiveWList_iCT() {
    clientContext.activate();

    //    SimpleResult<String> res = TestCommandParams.primitiveWList_iCT(List.of(1,3,5));
    //    Assertions.assertEquals("[1, 3, 5]", checkRes(res));
  }

  @Test
  @Disabled
  public void checkCommandPrimitiveWList_iJU() {
    clientContext.activate();

    //    SimpleResult<String> res = TestCommandParams.primitiveWList_iJU(List.of(1,3,5));
    //    Assertions.assertEquals("[1, 3, 5]", checkRes(res));
  }

  @Test
  public void checkCommandPrimitive_WOptional_i_Absent() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_PrimitiveWOptional_i(Optional.empty());
    Assertions.assertEquals("Optional.empty", checkRes(res));
  }

  @Test
  public void checkCommandPrimitive_WOptional_i() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_PrimitiveWOptional_i(Optional.ofNullable(42));
    Assertions.assertEquals("Optional[42]", checkRes(res));
  }


  @Test
  public void checkCommandString() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_String("Hello world");
    Assertions.assertEquals("Hello world", checkRes(res));
  }

  @Test
  @Disabled
  public void checkCommandStringListCT() {
    clientContext.activate();

//    SimpleResult<String> res = TestCommandParams.executeTestCommand_StringListJU(List.of("Hello", "world"));
//    Assertions.assertEquals("[\"Hello\", \"world\"]", checkRes(res));
  }

  @Test
  public void checkCommandStringListJU() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_StringListJU(List.of("Hello", "world"));
    Assertions.assertEquals("[Hello, world]", checkRes(res));
  }

  @Test
  public void checkCommandStringOpt_Absent() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_StringOpt(Optional.empty());
    Assertions.assertEquals("Optional.empty", checkRes(res));
  }

  @Test
  public void checkCommandStringOpt() {
    clientContext.activate();

    SimpleResult<String> res = TestCommandParams.executeTestCommand_StringOpt(Optional.of("Hello optional world"));
    Assertions.assertEquals("Optional[Hello optional world]", checkRes(res));
  }


  protected String checkRes(SimpleResult<String> res) {
    Assertions.assertTrue(res.isSimpleResult(), "Not a simple result" + res);
    Assertions.assertTrue(res.isSuccessful(), "Not successful" + res);
    return res.get();
  }
}
