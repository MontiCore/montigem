import { TypeCheckerUtil, TypeEnum } from "./type-checker.util";
import { Diff, Differences, MapDiff, ObjectDiff, TypeSelector } from "./types";

import { Utils } from "./utils";

/**
 * Compares two values of possibly different types and returns an object containing the differences between them.
 *
 * @template T1 - The type of the first argument to compare.
 * @template T2 - The type of the second argument to compare.
 * @param {T1} argument1 - The first argument to compare.
 * @param {T2} argument2 - The second argument to compare.
 * @returns {Diff<T1, T2>} - An object representing the differences between argument1 and argument2.
 */
function primitiveDiffer<T1, T2 = T1>(argument1: T1, argument2: T2): Diff<T1, T2> {
  // Set of types that can be simply compared by coercing to string
  const simpleComparison: Set<TypeEnum> = new Set<TypeEnum>([
    TypeEnum.FUNCTION,
    TypeEnum.CLASS,
    TypeEnum.CLASS_INSTANCE,
    TypeEnum.REG_EXP,
  ]);

  // If both arguments are null or undefined, they are considered equal
  if (TypeCheckerUtil.isNullOrUndefined(argument1) && TypeCheckerUtil.isNullOrUndefined(argument2)) return {};

  const type: TypeEnum = TypeCheckerUtil.getType(argument1);
  const diff: Diff<T1, T2> = { previous: argument1, current: argument2 };

  // If types of both arguments are not same, they are considered different
  if (type !== TypeCheckerUtil.getType(argument2)) return diff;

  // For simple types and primitive types, compare their string representation
  if (simpleComparison.has(type) || TypeCheckerUtil.isTypePrimitive(type)) {
    return String(argument1) === String(argument2) ? {} : diff;
  }

  // For Date objects, compare their time value
  if (TypeCheckerUtil.isDate(argument1) && TypeCheckerUtil.isDate(argument2)) {
    const t1: number | string = TypeCheckerUtil.isString(argument1) ? argument1 : argument1.getTime();
    const t2: number | string = TypeCheckerUtil.isString(argument2) ? argument2 : argument2.getTime();
    return t1 === t2 ? {} : diff;
  }

  return diff;
}

/**
 * Compares two iterables (i.e., arrays and sets) and returns an object representing the differences between them.
 *
 * @remarks
 *   - For complex iterables such as `maps`, please use {@link mapDiffer}, and {@link objectDiffer} for `objects`.
 *
 * @template T1 - The type of the first iterable to compare.
 * @template T2 - The type of the second iterable to compare.
 * @param {Iterable<T1>} iterable1 - The first iterable to compare.
 * @param {Iterable<T2>} iterable2 - The second iterable to compare.
 * @returns {Diff<Iterable<T1>, Iterable<T2>>} - An object representing the differences between iterable1 and iterable2.
 */
function iterableDiffer<T1, T2>(iterable1: Iterable<T1>, iterable2: Iterable<T2>): Diff<Iterable<T1>, Iterable<T2>> {
  // Convert iterables to arrays for comparison
  const array1: T1[] = [...iterable1];
  const array2: T2[] = [...iterable2];
  const diff: Diff<Iterable<T1>, Iterable<T2>> = { previous: iterable1, current: iterable2 };

  // If lengths are not same, they are considered different
  if (array1.length !== array2.length) return diff;

  // Compare all values of both arrays element-wise, if any is different, the iterables are considered different
  for (const [index, element] of array1.entries()) {
    if (!Utils.isEqual(element, array2[index])) return diff;
  }

  return {};
}

/**
 * Compares two maps and returns a MapDiff object representing the differences between them.
 *
 * @template K1 - The type of the key of the first map to compare.
 * @template V1 - The type of the value of the first map to compare.
 * @template K2 - The type of the key of the second map to compare.
 * @template V2 - The type of the value of the second map to compare.
 * @param {Map<K1, V1>} map1 - The first map to compare.
 * @param {Map<K2, V2>} map2 - The second map to compare.
 * @returns {MapDiff<Map<K1, V1>, Map<K2, V2>>} - A MapDiff object representing the differences between map1 and map2.
 */
function mapDiffer<K1, V1, K2, V2>(map1: Map<K1, V1>, map2: Map<K2, V2>): MapDiff<Map<K1, V1>, Map<K2, V2>> {
  const diff: MapDiff<Map<K1, V1>, Map<K2, V2>> = new Map();

  // Combine all keys from map1 and map2 into a single set
  const allKeys: Set<K1 | K2> = new Set([...map1.keys(), ...map2.keys()]);

  // Iterate over all keys
  for (const key of allKeys) {
    // Find an equivalent key in map1 and map2 (by value)
    const key1: K1 | undefined = [...map1.keys()].find((k: K1) => Utils.isEqual(k, key));
    const key2: K2 | undefined = [...map2.keys()].find((k: K2) => Utils.isEqual(k, key));

    const value1: V1 | undefined = map1.get(key1 as K1);
    const value2: V2 | undefined = map2.get(key2 as K2);

    // If no equivalent key is found, or the values are not equal, add the difference to the diff map
    if (!Utils.isEqual(value1, value2)) {
      diff.set(key, { previous: value1, current: value2 });
    }
  }

  return diff;
}

/**
 * Compares two objects and returns an ObjectDiff object representing the differences between them.
 *
 * @template T1 - The type of the first object to compare.
 * @template T2 - The type of the second object to compare.
 * @param {T1} object1 - The first object to compare.
 * @param {T2} object2 - The second object to compare.
 * @returns {ObjectDiff<T1, T2>} - An ObjectDiff object representing the differences between object1 and object2.
 */
function objectDiffer<T1 extends object, T2 extends object>(object1: T1, object2: T2): ObjectDiff<T1, T2> {
  const diff: ObjectDiff<T1, T2> = {};

  // Create a set of all keys in both objects
  const keys: Set<PropertyKey> = new Set([...Object.keys(object1), ...Object.keys(object2)]);

  // Iterate over the keys, if any pair of values for the same key is different, add it to the diff object
  for (const key of keys) {
    const value1: T1[keyof T1] | undefined = object1[key as keyof T1];
    const value2: T2[keyof T2] | undefined = object2[key as keyof T2];
    if (!Utils.isEqual(value1, value2)) {
      (diff as any)[key] = {
        previous: value1,
        current: value2,
      };
    }
  }
  return diff;
}

/**
 * Determines the types of the arguments and calls the appropriate differ function.
 *
 * @template T1 - The type of the first argument to compare.
 * @template T2 - The type of the second argument to compare.
 * @param {T1} argument1 - The first argument to compare.
 * @param {T2} argument2 - The second argument to compare.
 * @returns {Differences<T1, T2>} - An object representing the differences between argument1 and argument2.
 */
export function getDifferences<T1, T2>(argument1: T1, argument2: T2): Differences<T1, T2> {
  if (
    (TypeCheckerUtil.isArray(argument1) && TypeCheckerUtil.isArray(argument2)) ||
    (TypeCheckerUtil.isSet(argument1) && TypeCheckerUtil.isSet(argument2))
  ) {
    return iterableDiffer(argument1, argument2) as Differences<T1, T2>;
  } else if (TypeCheckerUtil.isMap(argument1) && TypeCheckerUtil.isMap(argument2)) {
    return mapDiffer(argument1, argument2) as Differences<T1, T2>;
  } else if (TypeCheckerUtil.isObject(argument1) && TypeCheckerUtil.isObject(argument2)) {
    return objectDiffer(argument1 as object, argument2 as object) as Differences<T1, T2>;
  } else {
    return primitiveDiffer(argument1, argument2) as Differences<T1, T2>;
  }
}

/**
 * Extracts the current value from the provided differences object.
 *
 * @template T1 Type of the previous entity.
 * @template T2 Type of the current entity.
 * @param {Differences<T1, T2>} differences An object representing differences between two versions of an entity.
 * @returns {Partial<T2>} The current value as per the differences object that have changed with respect to the previous
 *   entity.
 */
export function extractCurrentFromDifferences<T1, T2>(differences: Differences<T1, T2>): Partial<T2> {
  return extractCurrentOrPreviousFromDifferences(differences, "current");
}

/**
 * Extracts the previous value from the provided differences object.
 *
 * @template T1 Type of the previous entity.
 * @template T2 Type of the current entity.
 * @param {Differences<T1, T2>} differences An object representing differences between two versions of an entity.
 * @returns {Partial<T1>} The previous value as per the differences object that have changed with respect to the current
 *   entity.
 */
export function extractPreviousFromDifferences<T1, T2>(differences: Differences<T1, T2>): Partial<T1> {
  return extractCurrentOrPreviousFromDifferences(differences, "previous");
}

/**
 * Extracts either the current or the previous value from the provided differences object.
 *
 * @template T1 Type of the previous value.
 * @template T2 Type of the current value.
 * @template U Type of the extraction strategy, either "current" or "previous".
 * @param {Differences<T1, T2>} differences - An object representing differences between two versions of an entity.
 * @param {U} type - The extraction strategy, either "current" or "previous".
 * @returns {Partial<TypeSelector<T1, T2, U>>} The extracted value as per the extraction strategy that have changed with
 *   respect to the other entity.
 */
function extractCurrentOrPreviousFromDifferences<T1, T2, U extends keyof Diff<T1, T2>>(
  differences: Differences<T1, T2>,
  type: U
): Partial<TypeSelector<T1, T2, U>> {
  if (type in differences) {
    return (differences as Diff<T1, T2>)[type] as TypeSelector<T1, T2, U>;
  } else if (TypeCheckerUtil.isMap(differences)) {
    return new Map(
      [...differences].map(([key, diff]) => [key, (diff as Diff<unknown, unknown>)[type]])
    ) as TypeSelector<T1, T2, U>;
  } else if (TypeCheckerUtil.isObject(differences)) {
    return Object.fromEntries(Object.entries(differences).map(([key, diff]) => [key, diff[type]])) as TypeSelector<
      T1,
      T2,
      U
    >;
  } else if (TypeCheckerUtil.isArray(differences) || TypeCheckerUtil.isSet(differences)) {
    return Array.from(differences, (diff) => (diff as Diff<unknown, unknown>)[type]) as TypeSelector<T1, T2, U>;
  } else {
    throw new TypeError(`Unsupported type: ${typeof differences}`);
  }
}
