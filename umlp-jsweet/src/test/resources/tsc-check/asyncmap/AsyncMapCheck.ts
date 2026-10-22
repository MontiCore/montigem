/*
 * Runtime checks of AsyncMap (commonj2ts) for exectests.AsyncMapExecutionTest. Each method returns a
 * string that the test compares with the same scenario run on a java.util.LinkedHashMap, where Java
 * has a counterpart.
 */
import { AsyncMap, AsyncSet, MapEntryHelper } from "@umlp/commonj2ts";

async function newMap<K, V>(...keysAndValues: any[]): Promise<AsyncMap<K, V>> {
  const m = await new AsyncMap<K, V>().__AsyncMapConstructor();
  for (let i = 0; i < keysAndValues.length; i += 2) {
    await m.put(keysAndValues[i], keysAndValues[i + 1]);
  }
  return m;
}

function s(...values: any[]): string {
  return values.map((v) => String(v)).join(",");
}

function tick(): Promise<void> {
  return new Promise<void>((resolve) => setTimeout(resolve, 1));
}

function exceptionName(e: any): string {
  return e && e.__classes && e.__classes.indexOf("java.lang.UnsupportedOperationException") >= 0
    ? "UnsupportedOperationException" : "other: " + e;
}

export class AsyncMapCheck {

  static async basics(): Promise<string> {
    const m = await newMap<string, number>("a", 1, "b", 2);
    return s(await m.put("a", 3), await m.get("a"), await m.get("x"), await m.containsKey("b"), await m.containsKey("x"),
      await m.containsValue(2), await m.containsValue(1), await m.size(), await m.isEmpty(),
      await m.remove("b"), await m.remove("x"), m.toString(), await m.size());
  }

  static async defaults(): Promise<string> {
    const m = await newMap<string, number>("a", 1, "n", null);
    const r: any[] = [];
    r.push(await m.getOrDefault("a", 9), await m.getOrDefault("x", 9), await m.getOrDefault("n", 9));
    r.push(await m.putIfAbsent("a", 5), await m.putIfAbsent("b", 2), await m.putIfAbsent("n", 7));
    r.push(await m.remove("a", 2), await m.remove("a", 1), await m.containsKey("a"));
    r.push(await m.replace("b", 3), await m.replace("x", 3), await m.containsKey("x"));
    r.push(await m.replace("b", 9, 4), await m.replace("b", 3, 4), await m.get("b"));
    const increment = (k: string, v: number) => v == null ? 1 : v + 1;
    r.push(await m.compute("c", increment), await m.compute("c", increment), await m.compute("c", () => null), await m.containsKey("c"));
    r.push(await m.computeIfAbsent("d", () => 10), await m.computeIfAbsent("d", () => 20), await m.computeIfAbsent("e", () => null), await m.containsKey("e"));
    r.push(await m.computeIfPresent("d", (k, v) => v + 1), await m.computeIfPresent("x", () => 1), await m.computeIfPresent("d", () => null), await m.containsKey("d"));
    r.push(await m.merge("f", 1, (a, b) => a + b), await m.merge("f", 5, (a, b) => a + b), await m.merge("f", 1, () => null), await m.containsKey("f"));
    r.push(m.toString());
    return s(...r);
  }

  static async nullArguments(): Promise<string> {
    const m = await newMap<string, number>("a", 1, "n", null);
    return s(await m.remove("a", null), await m.remove("n", null), await m.containsKey("n"),
      await m.replace("a", null, 5), await m.replace("a", 1, null), await m.get("a"), await m.containsKey("a"));
  }

  static async asyncLambdas(): Promise<string> {
    const m = await newMap<string, number>("a", 1, "b", 2);
    await m.compute("a", async (k, v) => { await tick(); return v + 10; });
    await m.merge("b", 5, async (x, y) => { await tick(); return x * y; });
    await m.replaceAll(async (k, v) => { await tick(); return v + 1; });
    const seen: string[] = [];
    await m.forEach(async (k, v) => { await tick(); seen.push(k + "=" + v); });
    return s(m.toString(), seen.join(";"));
  }

  static async views(): Promise<string> {
    const m = await newMap<string, number>("a", 1, "b", 2, "c", 2);
    const keys = await m.keySet();
    const values = await m.values();
    const entries = await m.entrySet();
    await m.put("d", 4);
    const r: any[] = [await keys.size(), await keys.contains("d"), await values.contains(4),
      await entries.contains(["d", 4]), await entries.contains(["d", 5])];
    r.push(await keys.removeElement("a"), await keys.removeElement("x"), await m.containsKey("a"));
    r.push(await values.removeElement(2), m.toString());
    r.push(await entries.removeElement(["c", 3]), await entries.removeElement(["c", 2]), m.toString());
    r.push(await keys.equals(await AsyncSet.of("d")), await m.size());
    await keys.clear();
    r.push(await m.isEmpty(), await keys.size());
    return s(...r);
  }

  static async viewBulkOperations(): Promise<string> {
    const m = await newMap<string, number>("a", 1, "b", 2, "c", 3, "d", 4);
    const keys = await m.keySet();
    const r: any[] = [];
    r.push(await keys.removeIf((k) => k === "a"), m.toString());
    r.push(await keys.removeAll(await AsyncSet.of("b", "x")), m.toString());
    r.push(await keys.retainAll(await AsyncSet.of("d")), m.toString());
    r.push(await keys.containsAll(await AsyncSet.of("d")), await keys.containsAll(await AsyncSet.of("d", "a")));
    return s(...r);
  }

  /** no Java counterpart: java.util.LinkedHashMap throws a ConcurrentModificationException here */
  static async weaklyConsistentIteration(): Promise<string> {
    const m = await newMap<string, number>("a", 1, "b", 2, "c", 3);
    const seen: string[] = [];
    for await (const [k, v] of await m.entrySet()) {
      seen.push(k + "=" + v);
      if (k === "a") {
        await m.remove("b");
        await m.put("c", 30);
        await m.put("z", 26);
      }
    }
    return s(seen.join(";"), m.toString());
  }

  static async entrySetValue(): Promise<string> {
    const m = await newMap<string, number>("a", 1, "b", 2);
    const r: any[] = [];
    for await (const e of await m.entrySet()) {
      r.push(await MapEntryHelper.setValue(e, e[1] * 10), e[1]);
    }
    r.push(m.toString());
    // an entry whose key was removed: only the entry changes, like a detached java.util.LinkedHashMap entry
    let held: [string, number] = null;
    for await (const e of await m.entrySet()) {
      held = e;
      break;
    }
    await m.remove("a");
    r.push(await MapEntryHelper.setValue(held, 7), held[1], m.toString());
    return s(...r);
  }

  static async unsupportedOperations(): Promise<string> {
    const m = await newMap<string, number>("a", 1);
    const r: string[] = [];
    try {
      await MapEntryHelper.setValue(["x", 1] as [string, number], 2);
      r.push("no exception");
    } catch (e) {
      r.push(exceptionName(e));
    }
    try {
      await (await m.keySet()).add("b");
      r.push("no exception");
    } catch (e) {
      r.push(exceptionName(e));
    }
    try {
      await (await m.values()).add(2);
      r.push("no exception");
    } catch (e) {
      r.push(exceptionName(e));
    }
    try {
      await (await m.entrySet()).add(["b", 2]);
      r.push("no exception");
    } catch (e) {
      r.push(exceptionName(e));
    }
    r.push(m.toString());
    return s(...r);
  }

  static async equalsAndHashCode(): Promise<string> {
    const m1 = await newMap<string, number>("a", 1, "b", 2);
    const m2 = await newMap<string, number>("b", 2, "a", 1);
    const m3 = await newMap<string, number>("a", 1, "b", 3);
    const m4 = await newMap<string, number>("a", 1, "c", null);
    const m5 = await newMap<string, number>("a", 1, "d", null);
    return s(await m1.equals(m2), await m1.equals(m3), await m4.equals(m5), await m1.hashCode(), await m4.hashCode(),
      (await m1.hashCode()) === (await m2.hashCode()));
  }

  /** no Java counterpart for wrapping a native Map */
  static async constructors(): Promise<string> {
    const original = await newMap<string, number>("a", 1);
    const copy = await new AsyncMap<string, number>().__AsyncMapConstructor(original);
    await copy.put("b", 2);
    const sized = await new AsyncMap<string, number>().__AsyncMapConstructor(16, 0.75);
    const native = new Map<string, number>([["n", 1]]);
    const wrapped = await new AsyncMap<string, number>().__AsyncMapConstructor(native);
    native.set("m", 2);
    await wrapped.put("w", 3);
    const fromTuples = await new AsyncMap<string, number>().__AsyncMapConstructor([["t", 1], ["u", 2]]);
    const all = await new AsyncMap<string, number>().__AsyncMapConstructor();
    await all.putAll(original);
    await all.putAll(native);
    return s(original.toString(), copy.toString(), await sized.size(), wrapped.toString(), native.get("w"),
      wrapped.__internalMap() === native, fromTuples.toString(), all.toString());
  }
}
