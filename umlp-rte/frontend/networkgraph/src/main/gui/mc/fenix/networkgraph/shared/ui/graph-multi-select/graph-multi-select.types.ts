/** Emitted by {@link GraphMultiSelectComponent} whenever the set of selected values changes. */
export interface GraphMultiSelectChangeEvent<T = any> {
  /** The full, updated array of selected values. */
  value: T[];

  /** The original DOM event that triggered the change, if any. */
  originalEvent?: Event;
}
