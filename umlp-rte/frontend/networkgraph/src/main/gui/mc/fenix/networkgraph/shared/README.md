# Shared Utilities

This collection of utilities encompasses a set of utility classes, functions, decorators, directives, pipes and services like debounce, throttle, capitalize pipe, color conversion, and type checking utilities, enhancing the functionality and performance of the dashboard.

- [Shared Utilities](#shared-utilities)
  * [Features](#features)
  * [Required Libraries](#required-libraries)
  * [Usage](#usage)
  * [Authors](#authors)

## Features

Below is a tabular view of the available utilities categorized by their types and functionalities:

| Type       | Name                    | Description                                                                                                                                                                             |
|:-----------|-------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Decorators | Auto Unsubscribe        | Automatically unsubscribes from observables when the component is destroyed to prevent memory leaks.                                                                                    |
|            | Debounce                | Delays the processing of a function until after wait a specified time has elapsed since the last time the decorated function was invoked.                                               |
|            | Throttle                | Throttles a function, allowing it to be called at most once in a specified amount of time.                                                                                              |
|            | Throttle and Debounce   | Combines both throttling and debouncing to manage the frequency and delay of function executions.                                                                                       |
| Directives | Copy                    | Provides functionality to copy text to the clipboard.                                                                                                                                   |
|            | SVG Inserter            | Inserts SVG elements into the DOM, allowing for dynamic SVG rendering.                                                                                                                  |
| Pipes      | Capitalize First Letter | Transforms the first letter of a string to uppercase.                                                                                                                                   |
|            | Object Keys             | Returns an array of a given object's enumerable property names.                                                                                                                         |
|            | Value Formatter         | Formats values based on specified formatting options, such as strings, date, and numerical precision.                                                                                   |
| Services   | Focus Service           | Manages focus on interactive elements, providing a way to control focus for accessibility and usability.                                                                                |
| Utils      | Array Utils             | Provides utility functions for array manipulation like sorting, filtering, and flattening.                                                                                              |
|            | Clean Utils             | Provides functionality to clean data by removing null-ish values `(i.e., [], [null], {}, ...`                                                                                           |
|            | Color Utils             | Provides functions for color conversion between different formats (e.g., RGB to Hex) as well as many more color-related functionality.                                                  |
|            | Comparison Utils        | Provides default compare functions to be used inside `aray.sort(...)`.                                                                                                                  |
|            | Date Utils              | Provides functions for date parsing.                                                                                                                                                    |
|            | Differences Utils       | Identifies differences between two arbitrary data objects.                                                                                                                              |
|            | Export Utils            | Provides functionality to export `raw data, canvas, SVG, HTML-Elements, Tables, ...`  to different formats like `SVG`, `PNG`, `JSON`, or `XLSX`.                                        |
|            | JSON Utils              | Provides functions for JSON parsing, stringifying, and manipulation.                                                                                                                    |
|            | Map Utils               | Provides utility functions for map data structures such as sorting and converting a map into an object.                                                                                 |
|            | Merge Utils             | Provides functions to merge data structures like objects or arrays.                                                                                                                     |
|            | Object Utils            | Provides utility functions for object manipulation like sorting, converting an object into a map, resolving a property chain (i.e., `prop1.nestedProp`) as well as chaining properties. |
|            | Path Utils              | Provides functions to clean, normalize, manipulate and resolve file paths.                                                                                                              |
|            | Type Checker Utils      | Provides functions to check data types.                                                                                                                                                 |
|            | Types                   | Defines custom types used across the utility functions.                                                                                                                                 |
|            | General Utils           | Provides `DeepCopy` and `isEqual` (byValue) functions.                                                                                                                                  |

## Required Libraries

Tested with [Angular](https://angular.io/) (`v16.2.7`) and the following dependencies:

```text
"dependencies": {
  // Clone arbitrary objects (cloneDeepWith) and compare by value (isEqual)
  "lodash-es": "^4.17.0",
  // Convert HTML elements to images
  "dom-to-image": "^2.6.0",
}
```

## Usage

Most of the utilities have their own `@example` documentation or test cases, so just look up the usage over there!


# Detailed Explanation of Type Checking Utilities

In the following, we introduce additional details of the helper class TypeCheckerUtil, which enables dynamic type checking of variables at runtime. This section provides an overview of data type classification and presents a tabular description of all methods for type validation.

## Classification of Data Types
The enumeration below lists all enum elements used for classifying data types. The identification of all these types is supported.

```
export enum TypeEnum {
  DATE = "Date",
  STRING = "String",
  NUMBER = "Number",
  BOOLEAN = "Boolean",
  UNDEFINED = "Undefined",
  NULL = "Null",
  SYMBOL = "Symbol",
  ARRAY = "Array",
  REG_EXP = "RegExp",
  MAP = "Map",
  SET = "Set",
  CLASS = "Class",
  CLASS_INSTANCE = "ClassInstance",
  FUNCTION = "Function",
  OBJECT = "Object",
  UNKNOWN = "Unknown",
}
```


## Methods for Data Type Validation

The following table provides a comprehensive overview of the methods in the `TypeCheckerUtil` class, which enable detailed analysis and validation of data types.

| Method | Description |
| ------ | ----------- |
| `areAllExpectedType` | Checks if the actual type matches one of the expected types. |
| `areAllObjectValuesPrimitive` | Checks if all values in an object are primitive. |
| `areAllOfType` | Checks if all values are of a specified type, ignoring nullish values. |
| `areAllPrimitives` | Checks if all values are primitive data types. |
| `getType` | Returns the data type of the input value as an enum (see listing of all type enums). |
| `isArray` | Checks if a value is an array. |
| `isArrayOfPrimitives` | Checks if all elements in an array are primitive. |
| `isBoolean` | Checks if a value is of type Boolean. |
| `isClass` | Checks if a value is a class. |
| `isClassInstance` | Checks if a value is an instance of a class. |
| `isDate` | Checks if a value is a date or a string representation of a date. Uses the `isDate` method from the `DateUtil` class. |
| `isEventEmitter` | Checks if a value is an `EventEmitter`. |
| `isFunction` | Checks if a value is a function. |
| `isMap` | Checks if a value is a map. |
| `isNull` | Checks if a value is `null`. |
| `isNullOrUndefined` | Checks if a value is `null` or `undefined`. |
| `isNullish` | Checks if a value is nullish (null, undefined, empty string, empty array, empty object, empty set, or empty map). |
| `isNumber` | Checks if a value is numeric. |
| `isObject` | Checks if a value is an object, defined here as a date with key-value pairs, excluding array, date, RegExp, Set, Map, and Function, which have their own recognition functions. |
| `isObjectOfPrimitives` | Checks if an object consists only of values with primitive data types. |
| `isPrimitive` | Checks if a value is a primitive type, including string, number, boolean, null, undefined, and symbol. |
| `isPropertyKey` | Checks if a value is a valid key for an object, i.e., a string, number, or symbol. |
| `isRegExp` | Checks if a value is a regular expression. |
| `isSet` | Checks if a value is a set. |
| `isString` | Checks if a value is a string. |
| `isSymbol` | Checks if a value is a symbol. |
| `isTypePrimitive` | Checks if an enum type is primitive. |
| `isUndefined` | Checks if a value is `undefined`. |




# Authors

- Niklas Büchel

See also the list
of [contributors](https://git.rwth-aachen.de/monticore/artifacts/artviz/artviz-dashboard/-/graphs/main?ref_type=heads)
who participated in
this project.


