/** Emitted by {@link GraphSelectComponent} whenever the selected value changes. */
export interface GraphSelectChangeEvent<T = any> {
  /** The newly selected value (i.e. `option[optionValue]`, or the option itself if no `optionValue` was given). */
  value: T;

  /** The original DOM event that triggered the change, if any. */
  originalEvent?: Event;
}
