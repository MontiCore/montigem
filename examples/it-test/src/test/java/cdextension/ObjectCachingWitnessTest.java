/* (c) https://github.com/MontiCore/monticore */
package cdextension;

import cdbase.BaseClass;
import cdbase.CDBaseManager;
import cdbase.ConcreteBaseClass;
import cdextension.test_infrastructure.CDExtensionSQLDBTestSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Witness tests for monticore#5062: when a MontiGem CD extends another CD
 * (CDExtension imports CDBase.*), loading an object of an
 * extending class via the base path (CDBaseManager) used to materialize a
 * base-typed object in the wrong client-side manager cache.
 *
 * <p>Model: CDBase declares BaseClass and ConcreteBaseClass extends BaseClass;
 * CDExtension declares Extension extends BaseClass. So the base CD has its own
 * class hierarchy and the extension hangs off the shared BaseClass.
 *
 * <p>Fixed by the runtime-type discriminator: domain objects know their DeSer
 * (GemIdentifiable.getDeSer()), serialization writes an "@type" member, the
 * receiver materializes the concrete type via its DeSer, and the client
 * managers route registration to the manager owning the concrete type
 * (_register hooks, ClientDelegatedOpt/ClientDelegatedRegister templates).
 */
public class ObjectCachingWitnessTest extends CDExtensionSQLDBTestSetup {

  public ObjectCachingWitnessTest() {
    super(1);
  }

  // These tests end with a client context active (getClients().activate()). Restore the backend
  // context so a following test does not accidentally run its commands through the client roundtrip.
  @AfterEach
  public void restoreBackendContext() {
    getBackend().activate();
  }

  private Extension createExtensionInBackend() {
    getBackend().activate();
    return CDExtensionManager.extensionBuilder().foo(1).bar(2).build().get();
  }

  /**
   * Loading an Extension instance via the base path must yield an Extension
   * cached in the CDExtensionManager, not a base-typed copy cached in the
   * CDBaseManager. The base manager still reports it as loaded because
   * _isLoaded delegates to the extending-type managers.
   */
  @Test
  public void testObjectLoadedViaBasePathIsCachedInExtensionManager() {
    Extension objServer = createExtensionInBackend();

    getClients(0).activate();
    BaseClass objClient = CDBaseManager.getBaseClassSure(objServer.getGemId());
    assertNotSame(objServer, objClient);

    assertAll(
        () -> assertInstanceOf(Extension.class, objClient),
        () -> assertTrue(CDBaseManager.isLoadedBaseClass(objServer.getGemId())),
        () -> assertTrue(CDExtensionManager.isLoadedExtension(objServer.getGemId()))
    );
  }

  /**
   * The subclass fields must survive a load via the base path (the base DeSer
   * only serializes base fields, so bar used to be lost on the wire).
   */
  @Test
  public void testSubclassFieldsSurviveLoadViaBasePath() {
    Extension objServer = createExtensionInBackend();

    getClients(0).activate();
    BaseClass objClient = CDBaseManager.getBaseClassSure(objServer.getGemId());

    Extension objClientAsExtension = assertInstanceOf(Extension.class, objClient);
    assertEquals(1, objClientAsExtension.getFoo());
    assertEquals(2, objClientAsExtension.getBar());
  }

  /**
   * Loading the same object first via the base path and then via the
   * extension path must yield the same instance — otherwise two different
   * instances with the same gemId live in two caches.
   */
  @Test
  public void testLoadViaBaseAndExtensionPathYieldsSameInstance() {
    Extension objServer = createExtensionInBackend();

    getClients(0).activate();
    BaseClass objViaBase = CDBaseManager.getBaseClassSure(objServer.getGemId());
    Extension objViaExtension = CDExtensionManager.getExtensionSure(objServer.getGemId());

    assertSame(objViaExtension, objViaBase);
  }

  /**
   * Control case: subclasses declared in the base CD itself are not affected
   * by the delegation — a ConcreteBaseClass loaded via the base path stays in
   * the CDBaseManager, with its concrete type and all fields.
   */
  @Test
  public void testSubclassOfBaseCDStaysInBaseManager() {
    getBackend().activate();
    ConcreteBaseClass objServer =
        CDBaseManager.concreteBaseClassBuilder().foo(1).foo2("x").build().get();

    getClients(0).activate();
    BaseClass objClient = CDBaseManager.getBaseClassSure(objServer.getGemId());

    ConcreteBaseClass objClientConcrete = assertInstanceOf(ConcreteBaseClass.class, objClient);
    assertAll(
        () -> assertEquals(1, objClientConcrete.getFoo()),
        () -> assertEquals("x", objClientConcrete.getFoo2()),
        () -> assertTrue(CDBaseManager.isLoadedConcreteBaseClass(objServer.getGemId())),
        () -> assertFalse(CDExtensionManager.isLoadedExtension(objServer.getGemId()))
    );
  }
}
