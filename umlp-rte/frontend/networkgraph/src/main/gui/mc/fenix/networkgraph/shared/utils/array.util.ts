import { ArrayType, ComparisonFunction, NestedArray, SortingMode } from "./types";
import { TypeCheckerUtil } from "./type-checker.util";
import { compareValues } from "./comparison.util";
import { ObjectUtil } from "./object.util";
import { CleanUtil } from "./clean.util";

/**
 * The Arrays class provides utility methods for arrays. It provides the following static methods:
 *
 * @example
 *   // Sorts an array of primitive values.
 *   sortPrimitiveArray();
 *   // Check whether the array is an array of primitives
 *   isArrayOfPrimitives();
 *   // Recursively flattens an array
 *   flatten();
 */
export class ArrayUtil {
  /**
   * Sorts an array of objects or primitive values based on a specified property name in ascending or descending order.
   * If the array contains primitives (strings or numbers) and propName is undefined, the array is sorted directly.
   *
   * @remarks
   *   Nullish (null, undefined, or empty) entries are removed before sorting.
   * @example
   *   sortPrimitiveArray([3, 1, 4, 2]); // [1, 2, 3, 4]
   *   sortPrimitiveArray(["c", "a", "b"], "DESC"); // ['c', 'b', 'a']
   *   sortPrimitiveArray([false, true, false], "ASC"); // [false, false, true]
   *
   * @template T - The type of the elements in the array.
   * @param {T[]} array - The array of primitive values to be sorted.
   * @param {"ASC" | "DESC"} mode - The sorting mode. Default is 'ASC' (ascending). Can be set to 'DESC' for descending
   *   order.
   * @param {ComparisonFunction<T>} compareFunction - An optional custom compare function.
   * @returns {T[]} The sorted array of objects or primitive values.
   * @throws Error if the specified property is not found in some objects in the array.
   */
  public static sortPrimitiveArray<T extends string | number | boolean | null | undefined>(
    array: T[],
    mode: SortingMode = SortingMode.ASC,
    compareFunction?: ComparisonFunction<T>
  ): T[] {
    // Remove empty and nullish entries
    array = CleanUtil.cleanDeep(array);

    return array.sort((a: T, b: T): number => {
      return compareValues(a, b, mode, compareFunction);
    });
  }

  /**
   * Sorts an array of objects based on a specified property name in ascending or descending order.
   *
   * @remarks
   *   Nullish (null, undefined, or empty) entries are removed before sorting.
   * @template T - The type of the objects in the array.
   * @template K - The type of the keys of the object in the array.
   * @param {T[]} array - The array of objects to be sorted.
   * @param {keyof T} propertyName - The name of the property to sort by.
   * @param {"ASC" | "DESC"} mode - The sorting mode. Default is 'ASC' (ascending). Can be set to 'DESC' for descending
   *   order.
   * @param {ComparisonFunction<T[K]>} compareFunction - An optional custom compare function.
   * @returns {T[]} The sorted array of objects or primitive values.
   * @throws Error if the specified property is not found in some objects in the array.
   */
  public static sortArrayByProperty<T extends object>(
    array: T[],
    propertyName: keyof T,
    mode: SortingMode = SortingMode.ASC,
    compareFunction?: ComparisonFunction<T[keyof T]>
  ): T[] {
    // Remove empty and nullish entries
    array = CleanUtil.cleanDeep(array);

    // Check if every entry has the specified property
    if (!ArrayUtil.hasObjectsWithProperty(array, propertyName)) {
      throw new Error(`Property '${String(propertyName)}' was not found in some objects in the array: ${array}`);
    }

    return array.sort((a: T, b: T): number => {
      return compareValues(a[propertyName], b[propertyName], mode, compareFunction);
    });
  }

  /**
   * Check if a value is an array of primitive types.
   *
   * @param {unknown} value - The array to check.
   * @returns {boolean} - True if the array consists only of primitive types, false otherwise.
   */
  public static isArrayOfPrimitives<T>(value: ArrayType<T>): boolean {
    return value.every((a: unknown) => TypeCheckerUtil.isPrimitive(a));
  }

  /**
   * Checks if every object in an array has a specified property.
   *
   * @remarks
   *   This method can be used to validate the shape of the objects in the array. For example, it could be used to ensure
   *   that every object in the array has a required 'id' property before processing the array.
   * @template T - The type of the values in the record.
   * @template K - The key of the record, which extends PropertyKey.
   * @param {unknown} value - The array to check.
   * @param {K} property - The property to check for in each object in the array.
   * @returns {boolean} - True if every object in the array has the specified property, false otherwise. This method
   *   uses a type guard to narrow the type of the array if it returns true.
   */
  public static hasObjectsWithProperty<T, K extends PropertyKey>(
    value: unknown,
    property: K
  ): value is Array<Record<K, T>> {
    return (
      TypeCheckerUtil.isArray(value) &&
      !TypeCheckerUtil.isNullish(value) &&
      value.every((item) => ObjectUtil.hasProperties(item, property))
    );
  }

  /**
   * Utility function that recursively flattens any array.
   *
   * @example
   *   console.log(flatten([1, 2, 3, [4, [5, [6, [7]]]]])); // [1, 2, 3, 4, 5, 6, 7]
   *
   * @param {NestedArray<T>} input - The array to flatten
   * @param {T[]} accumulator - The current accumulator (for recursive calls only)
   * @returns {T[]} - Flattened array
   */
  public static flatten<T>(input: NestedArray<T>, accumulator: T[] = []): T[] {
    return input.reduce<T[]>((_: T[], current: T | NestedArray<T>): T[] => {
      if (Array.isArray(current)) return this.flatten(current, accumulator);
      accumulator.push(current as T);
      return accumulator;
    }, []);
  }
}
