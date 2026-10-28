import {IUMLPMap} from "../common/IUMLPMap";
import {JavaUtilUtils} from "./JavaUtilUtils";

/**
 * java.util.Map.Entry in transpiled code is a tuple [key, value]: getKey() is entry[0], getValue() is entry[1].
 * The entries of AsyncMap#entrySet are backed by their map (a non-enumerable reference on the tuple),
 * so that setValue writes through to the map.
 */
export class MapEntryHelper {

  /** Backs the entry by the map, so that setValue writes through to it */
  public static __backedBy<K, V>(entry: [K, V], map: IUMLPMap<K, V>): [K, V] {
    Object.defineProperty(entry, "__map", {value: map, configurable: true});
    return entry;
  }

  /**
   * java.util.Map.Entry#setValue(V) → V
   *
   * Sets the value of the entry and, while the map still contains its key, writes it through to the map.
   * @throws UnsupportedOperationException if the entry is not backed by a map, e.g. entries of GemMaps,
   *   whose keys are derived from their values
   */
  public static async setValue<K, V>(entry: [K, V], value: V): Promise<V> {
    const map: IUMLPMap<K, V> = (entry as any).__map;
    if (map == null) {
      throw JavaUtilUtils.unsupportedOperation("setValue is not supported by this Map.Entry, it is not backed by a modifiable map");
    }
    let oldValue = entry[1];
    if (await map.containsKey(entry[0])) {
      oldValue = await map.put(entry[0], value);
    }
    entry[1] = value;
    return oldValue;
  }
}
