package loadingtests.commands;

import loadingtests.Animal;
import loadingtests.Being;
import loadingtests.LoadingTestsManager;
import loadingtests.LoadingTestsServerApplication;
import loadingtests.LoadingTestsTestContext;
import loadingtests.Person;
import loadingtests.Student;
import loadingtests.service.TestClientWebSocketCommandCommunication;
import loadingtests.test_infrastructure.LoadingTestsMockTestSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import umlp.backendrte.command.ClientCommandCommunication;
import umlp.backendrte.command.Command;
import umlp.backendrte.command.CommandManager;
import umlp.backendrte.command.CommandResult;
import umlp.backendrte.command.TestClientCommandManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Batch loading via the abstract superclass Being: loadAllBeingWithIDs must
 * fetch the objects in batches (not one command per gemID), materialize each
 * object as its concrete subtype and register it with the concrete type.
 */
@SpringBootTest(classes = LoadingTestsServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Execution(ExecutionMode.SAME_THREAD)
class LoadAllBeingWithIDsCITest extends LoadingTestsMockTestSetup {

  @LocalServerPort
  private int port;
  private String uri = "ws://localhost:%s/umlp/api/command";
  private LoadingTestsTestContext serverContext;
  private LoadingTestsTestContext clientContext;
  private TestClientWebSocketCommandCommunication localhost;
  private RecordingCommandManager commandManager;
  private int originalLoadBatchSize;

  public LoadAllBeingWithIDsCITest() {
    super(1);
  }

  @Override
  public void _init() throws Exception {
    super._init();
    this.serverContext = getBackend();
    this.clientContext = getClients(0);
    this.localhost = new TestClientWebSocketCommandCommunication(String.format(uri, port));
    this.localhost.setContext(serverContext, clientContext);
    this.commandManager = new RecordingCommandManager(this.localhost);
    CommandManager.initMe(commandManager);
    clientContext.setCommandManager(commandManager);
  }

  @BeforeEach
  public void initBeforeEach() throws Exception {
    this.originalLoadBatchSize = CommandManager.loadBatchSize;
    this._init();
    serverContext.activate();
    LoadingTestsManager.reset();
    clientContext.activate();
    LoadingTestsManager.reset();
  }

  @AfterEach
  public void tearDownAfterEach() throws Exception {
    CommandManager.loadBatchSize = this.originalLoadBatchSize;
    if (this.localhost != null) {
      this.localhost.close();
    }
  }

  @Test
  public void testAbstractBatchLoadingSingleBatch() {
    CommandManager.loadBatchSize = 1000;
    loadMixedBeings(300, 1);
  }

  @Test
  public void testAbstractBatchLoadingMultipleBatches() {
    CommandManager.loadBatchSize = 100;
    loadMixedBeings(250, 3);
  }

  @Test
  public void testAbstractBatchLoadingSkipsAlreadyLoaded() {
    CommandManager.loadBatchSize = 100;
    serverContext.activate();
    List<Being> created = createMixedBeings(30);
    List<Long> ids = gemIds(created);

    clientContext.activate();
    List<Long> firstHalf = ids.subList(0, 15);
    assertEquals(15, LoadingTestsManager.loadAllBeingWithIDs(firstHalf).size());

    commandManager.recorded.clear();
    List<Being> loaded = LoadingTestsManager.loadAllBeingWithIDs(ids);

    // only the 15 unknown gemIDs are requested, in a single batch
    assertEquals(1, commandManager.recorded.size());
    assertInstanceOf(LoadAllBeingWithIDs.class, commandManager.recorded.get(0));
    assertEquals(ids.subList(15, 30),
        ((LoadAllBeingWithIDs) commandManager.recorded.get(0)).getGemIDsList());
    // ... but the result contains all of them, in the requested order
    assertEquals(ids, gemIds(loaded));
  }

  @Test
  public void testAbstractBatchLoadingEmpty() {
    clientContext.activate();
    commandManager.recorded.clear();
    assertTrue(LoadingTestsManager.loadAllBeingWithIDs(new ArrayList<>()).isEmpty());
    assertTrue(commandManager.recorded.isEmpty());
  }

  private void loadMixedBeings(int numberOfObjects, int expectedBatches) {
    serverContext.activate();
    List<Being> created = createMixedBeings(numberOfObjects);
    List<Long> ids = gemIds(created);
    Map<Long, Class<?>> expectedTypes = new HashMap<>();
    Map<Long, String> expectedNames = new HashMap<>();
    for (Being being : created) {
      expectedTypes.put(being.getGemId(), concreteType(being));
      expectedNames.put(being.getGemId(), being.getName());
    }

    clientContext.activate();
    for (Long id : ids) {
      assertFalse(LoadingTestsManager.isLoadedBeing(id));
    }
    commandManager.recorded.clear();
    List<Being> loaded = LoadingTestsManager.loadAllBeingWithIDs(ids);

    // loaded in batches: no single-object load commands
    assertEquals(expectedBatches, commandManager.recorded.size());
    for (Command<?> command : commandManager.recorded) {
      assertInstanceOf(LoadAllBeingWithIDs.class, command);
      assertTrue(((LoadAllBeingWithIDs) command).getGemIDsList().size() <= CommandManager.loadBatchSize);
    }

    // complete, in the requested order, as the concrete subtype
    assertEquals(ids, gemIds(loaded));
    for (Being being : loaded) {
      assertTrue(being.isBuildFinished());
      assertEquals(expectedTypes.get(being.getGemId()), concreteType(being));
      assertEquals(expectedNames.get(being.getGemId()), being.getName());
    }

    // registered with the manager of the concrete type: lookups hit the cache
    commandManager.recorded.clear();
    for (Being being : loaded) {
      long id = being.getGemId();
      assertTrue(LoadingTestsManager.isLoadedBeing(id));
      assertSame(being, LoadingTestsManager.beingOpt(id).get());
      if (being instanceof Animal) {
        assertTrue(LoadingTestsManager.isLoadedAnimal(id));
        assertSame(being, LoadingTestsManager.animalOpt(id).get());
      } else {
        assertTrue(LoadingTestsManager.isLoadedPerson(id));
        assertSame(being, LoadingTestsManager.personOpt(id).get());
        if (being instanceof Student) {
          assertTrue(LoadingTestsManager.isLoadedStudent(id));
          assertSame(being, LoadingTestsManager.studentOpt(id).get());
        }
      }
    }
    assertTrue(commandManager.recorded.isEmpty(),
        "Cache lookups after batch loading must not send commands, but sent: " + commandManager.recorded);
  }

  /** Creates Animals, Persons and Students in turn. */
  private List<Being> createMixedBeings(int numberOfObjects) {
    List<Being> created = new ArrayList<>();
    for (int i = 0; i < numberOfObjects; i++) {
      switch (i % 3) {
        case 0:
          created.add(LoadingTestsManager.animalBuilder().name("animal" + i).legs(i % 8).build().get());
          break;
        case 1:
          created.add(LoadingTestsManager.personBuilder().name("person" + i).age(i % 90).build().get());
          break;
        default:
          created.add(LoadingTestsManager.studentBuilder().name("student" + i).age(i % 90).matNr(i).build().get());
          break;
      }
    }
    return created;
  }

  private static Class<?> concreteType(Being being) {
    if (being instanceof Student) {
      return Student.class;
    }
    if (being instanceof Person) {
      return Person.class;
    }
    if (being instanceof Animal) {
      return Animal.class;
    }
    return Being.class;
  }

  private static List<Long> gemIds(List<Being> beings) {
    return beings.stream().map(Being::getGemId).collect(Collectors.toList());
  }

  /** Records every command the client sends to the server. */
  private static class RecordingCommandManager extends TestClientCommandManager {
    final List<Command<?>> recorded = new ArrayList<>();

    RecordingCommandManager(ClientCommandCommunication commandCommunication) {
      super(commandCommunication);
    }

    @Override
    protected <T> CommandResult<T> _executeCommand(Command<T> command) {
      recorded.add(command);
      return super._executeCommand(command);
    }
  }
}
