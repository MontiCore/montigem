/* (c) https://github.com/MontiCore/monticore */
package associations;

import associations.commands.NumPlainOwner_putByKey;
import associations.commands.NumPlainOwner_rekeyByKey;
import associations.test_infrastructure.AssociationsMockTestSetup;
import de.se_rwth.commons.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import umlp.backendrte.command.CommandManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synchronization scenarios of attribute qualified associations (GemMaps) between the backend and two clients that do
 * not fit the generated sync protocol tests (GemMapSyncTestGenerator, syncprotocol.generated.gemmap): commands sent
 * by hand, key collisions, builders on a client and values in several maps. The map is NumPlain: an int key.
 */
public class GemMapSyncTest extends AssociationsMockTestSetup {

  long ownerId;
  long annaId;
  long benId;

  public GemMapSyncTest() {
    super(2);
  }

  @BeforeEach
  public void createObjectsInBackend() {
    getBackend().activate();
    this.ownerId = AssociationsManager.numPlainOwnerBuilder().build().get().getGemId();
    this.annaId = AssociationsManager.numPlainValueBuilder().key(1001).build().get().getGemId();
    this.benId = AssociationsManager.numPlainValueBuilder().key(1002).build().get().getGemId();
  }

  NumPlainOwner owner() {
    return AssociationsManager.getNumPlainOwnerSure(ownerId);
  }

  NumPlainValue value(long id) {
    return AssociationsManager.getNumPlainValueSure(id);
  }

  /** for the tests that provoke errors: collect them instead of failing */
  void collectFindings() {
    Log.init();
    Log.enableFailQuick(false);
  }

  boolean logged(String code) {
    return Log.getFindings().stream().anyMatch(f -> f.getMsg().startsWith(code));
  }

  @AfterEach
  public void resetLog() {
    Log.init();
  }

  /** a key that does not match the value (stale on the client) is replaced by the server's derived key */
  @Test
  public void testPutWithWrongKeyIsCorrectedByServer() {
    getClients(1).activate();
    assertEquals(0, owner().sizeByKeys());

    getClients(0).activate();
    CommandManager.executeCommand(new NumPlainOwner_putByKey(ownerId, annaId, 9999));

    getBackend().activate();
    assertTrue(owner().containsKeyByKey(1001));
    assertFalse(owner().containsKeyByKey(9999));

    getClients(1).activate();
    assertTrue(owner().containsKeyByKey(1001));
    assertFalse(owner().containsKeyByKey(9999));
  }

  /** only the server sends rekeying: it rejects a rekeying sent by a client and does not send it on */
  @Test
  public void testRekeyFromClientIsRejected() {
    getBackend().activate();
    owner().putByKey(value(annaId));

    getClients(1).activate();
    assertTrue(owner().containsKeyByKey(1001));

    getClients(0).activate();
    assertFalse(CommandManager.executeCommand(new NumPlainOwner_rekeyByKey(ownerId, annaId, 1001, 7777))
        .isSuccessful());

    getBackend().activate();
    assertTrue(owner().containsKeyByKey(1001));
    assertFalse(owner().containsKeyByKey(7777));

    getClients(1).activate();
    assertTrue(owner().containsKeyByKey(1001));
    assertFalse(owner().containsKeyByKey(7777));
  }

  /** a builder on client 0 keeps the value until it is built */
  @Test
  public void testBuilderOnClient() {
    getClients(0).activate();
    NumPlainOwner built = AssociationsManager.numPlainOwnerBuilder().byKeyPut(value(annaId)).build().get();
    long builtId = built.getGemId();
    assertTrue(built.containsKeyByKey(1001));

    getBackend().activate();
    assertTrue(AssociationsManager.getNumPlainOwnerSure(builtId).containsKeyByKey(1001));

    getClients(1).activate();
    assertTrue(AssociationsManager.getNumPlainOwnerSure(builtId).containsKeyByKey(1001));
  }

  /**
   * a key that changes while the owner is still a builder is derived when the owner is built (the managers observe
   * the values of the owners they know): the key of a built value changed by its setter, and the key of a value
   * builder set after it was put
   */
  @Test
  public void testKeyChangeBeforeTheOwnerIsBuilt() {
    getClients(0).activate();
    NumPlainOwnerBuilder ownerBuilder = AssociationsManager.numPlainOwnerBuilder();
    ownerBuilder.byKeyPut(value(annaId));
    NumPlainValueBuilder carlBuilder = AssociationsManager.numPlainValueBuilder().key(1003);
    ownerBuilder.byKeyPut(carlBuilder);
    value(annaId).setKey(2001);
    carlBuilder.key(2003);
    long builtId = ownerBuilder.build().get().getGemId();
    long carlId = carlBuilder.getGemId();

    getClients(0).activate();
    assertBuiltOwnerKeys(builtId, carlId);
    getBackend().activate();
    assertBuiltOwnerKeys(builtId, carlId);
    getClients(1).activate();
    assertBuiltOwnerKeys(builtId, carlId);
  }

  void assertBuiltOwnerKeys(long builtId, long carlId) {
    NumPlainOwner built = AssociationsManager.getNumPlainOwnerSure(builtId);
    assertEquals(annaId, built.getByKey(2001).getGemId());
    assertFalse(built.containsKeyByKey(1001));
    assertEquals(carlId, built.getByKey(2003).getGemId());
    assertFalse(built.containsKeyByKey(1003));
    assertEquals(2, built.sizeByKeys());
  }

  /**
   * a value is stored under one key only: after its rekeying was refused it keeps its old key; putting it again moves
   * it to the key it derives, on every participant, instead of storing it twice
   */
  @Test
  public void testPutMovesValueFromStaleKey() {
    collectFindings();
    getBackend().activate();
    owner().putByKey(value(annaId));
    owner().putByKey(value(benId));

    getClients(1).activate();
    assertEquals(2, owner().sizeByKeys());

    getBackend().activate();
    // Ben takes Anna's key: his rekeying is refused, he stays under 1002
    value(benId).setKey(1001);
    assertTrue(logged("0xA7304"));
    assertEquals(benId, owner().getByKey(1002).getGemId());

    // Anna leaves, Ben is put again: moved from 1002 to 1001
    owner().removeByKey(1001);
    owner().putByKey(value(benId));
    assertEquals(1, owner().sizeByKeys());
    assertEquals(benId, owner().getByKey(1001).getGemId());
    assertFalse(owner().containsKeyByKey(1002));

    getClients(1).activate();
    assertEquals(1, owner().sizeByKeys());
    assertTrue(owner().containsKeyByKey(1001));
    assertFalse(owner().containsKeyByKey(1002));
    assertFalse(logged("0xA7307"));
  }

  /** replacing a stale entry (its value derives another key by now) is a repair, not an error */
  @Test
  public void testPutReplacingStaleEntryIsNotReported() {
    collectFindings();
    getBackend().activate();
    owner().putByKey(value(annaId));
    owner().putByKey(value(benId));
    // Anna takes Ben's key: her rekeying is refused, she stays under 1001
    value(annaId).setKey(1002);
    assertTrue(logged("0xA7304"));

    long carlId = AssociationsManager.numPlainValueBuilder().key(1001).build().get().getGemId();
    owner().putByKey(value(carlId));
    assertFalse(logged("0xA7307"));
    assertEquals(2, owner().sizeByKeys());
    assertEquals(carlId, owner().getByKey(1001).getGemId());
    assertEquals(benId, owner().getByKey(1002).getGemId());
  }

  /**
   * deleting a value (not removing it from one map) removes its entry from every map holding it, on every participant
   * that loaded the owner, without loading the value there
   */
  @Test
  public void testDeleteValueRemovesItsEntryEverywhere() {
    getBackend().activate();
    owner().putByKey(value(annaId));
    owner().putByKey(value(benId));
    long otherOwnerId = AssociationsManager.numPlainOwnerBuilder().byKeyPut(value(annaId)).build().get().getGemId();

    getClients(1).activate();
    assertEquals(2, owner().sizeByKeys());
    assertEquals(1, AssociationsManager.getNumPlainOwnerSure(otherOwnerId).sizeByKeys());

    getClients(0).activate();
    assertTrue(AssociationsManager.deleteNumPlainValue(annaId));

    getBackend().activate();
    assertFalse(owner().containsKeyByKey(1001));
    assertTrue(owner().containsKeyByKey(1002));
    assertFalse(AssociationsManager.getNumPlainOwnerSure(otherOwnerId).containsKeyByKey(1001));

    getClients(1).activate();
    assertFalse(owner().containsKeyByKey(1001));
    assertEquals(1, owner().sizeByKeys());
    assertEquals(0, AssociationsManager.getNumPlainOwnerSure(otherOwnerId).sizeByKeys());
    assertFalse(AssociationsManager.isLoadedNumPlainValue(annaId));
  }

  /** removing a value from one map keeps it in the others */
  @Test
  public void testRemoveKeepsTheValueInOtherMaps() {
    getBackend().activate();
    owner().putByKey(value(annaId));
    long otherOwnerId = AssociationsManager.numPlainOwnerBuilder().byKeyPut(value(annaId)).build().get().getGemId();

    getClients(0).activate();
    owner().removeByKey(1001);

    getBackend().activate();
    assertFalse(owner().containsKeyByKey(1001));
    assertTrue(AssociationsManager.getNumPlainOwnerSure(otherOwnerId).containsKeyByKey(1001));

    getClients(1).activate();
    assertTrue(AssociationsManager.getNumPlainOwnerSure(otherOwnerId).containsKeyByKey(1001));
  }
}
