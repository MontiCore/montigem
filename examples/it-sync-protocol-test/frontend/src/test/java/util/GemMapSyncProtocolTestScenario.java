package util;

/**
 * the objects of a generated GemMap sync protocol test: the owner, the values stored under the keys 0, 1 and 2, and
 * the key objects 0 to 3 of a GemEntryMap (key 3 is free, e.g. for rekeying)
 */
public final class GemMapSyncProtocolTestScenario {
  public long ownerId = -1;
  public long value0Id = -1;
  public long value1Id = -1;
  public long value2Id = -1;
  public long targetValueId = -1;
  public long oldValueId = -1;
  public long[] keyIds = {-1, -1, -1, -1};
}
