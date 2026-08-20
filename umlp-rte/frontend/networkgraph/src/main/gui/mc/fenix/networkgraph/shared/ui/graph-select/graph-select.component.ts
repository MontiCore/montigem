import { ChangeDetectionStrategy, Component, ContentChildren, ElementRef, EventEmitter, Input, Output, QueryList, TemplateRef, ViewChild } from "@angular/core";
import { CommonModule } from "@angular/common";
import { GraphTemplateDirective } from "../../directives/graph-template.directive";
import { GraphOverlayPositionDirective } from "../../directives/graph-overlay-position.directive";
import { TemplateUtil } from "../../utils/template.util";
import { GraphSelectChangeEvent } from "./graph-select.types";

/**
 * GraphSelectComponent is a lightweight, dependency-free replacement for PrimeNG's `p-select`. It offers a searchable,
 * clearable single-select dropdown with support for custom item/selectedItem/footer templates (via
 * {@link GraphTemplateDirective}), used as a drop-in-ish replacement for the `search-node` dropdown.
 *
 * @example
 *   <graph-select
 *     [options]="nodes"
 *     [value]="selectedNodeKey"
 *     optionLabel="searchLabel"
 *     optionValue="nodeKey"
 *     [filter]="true"
 *     filterBy="searchLabel,nodeKey"
 *     (selectionChange)="onSelect($event)">
 *     <ng-template graphTemplate="item" let-item>{{ item.searchLabel }}</ng-template>
 *   </graph-select>
 */
@Component({
  selector: "graph-select",
  standalone: true,
  imports: [CommonModule, GraphOverlayPositionDirective],
  templateUrl: "./graph-select.component.html",
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraphSelectComponent {
  /** The list of selectable options. */
  @Input() options: any[] = [];

  /** Key used to read the display label from an option object. If omitted, the option itself is used as the label. */
  @Input() optionLabel: string | undefined;

  /** Key used to read the value from an option object. If omitted, the option itself is used as the value. */
  @Input() optionValue: string | undefined;

  /** The currently selected value. */
  @Input() value: any;

  /** Whether a search input should be shown inside the panel. */
  @Input() filter: boolean = false;

  /** Comma-separated list of option keys that the search input filters on. */
  @Input() filterBy: string = "";

  /** Whether a "clear selection" button should be shown once a value is selected. */
  @Input() showClear: boolean = false;

  /** Placeholder text shown when no value is selected. */
  @Input() placeholder: string = "";

  /** Whether the select is disabled. */
  @Input() disabled: boolean = false;

  /** CSS class applied to the host/trigger. */
  @Input() styleClass: string = "";

  /** CSS class applied to the dropdown panel. */
  @Input() panelStyleClass: string = "";

  /** Emits the new value whenever the selection changes (supports `[(value)]`). */
  @Output() valueChange: EventEmitter<any> = new EventEmitter<any>();

  /** Emits detailed change information whenever the selection changes. */
  @Output() selectionChange: EventEmitter<GraphSelectChangeEvent> = new EventEmitter<GraphSelectChangeEvent>();

  /** Emits whenever the dropdown panel is opened. */
  @Output() panelShow: EventEmitter<void> = new EventEmitter<void>();

  /** Content-projected templates, keyed by their `graphTemplate` name. */
  @ContentChildren(GraphTemplateDirective) private templates: QueryList<GraphTemplateDirective> | undefined;

  /** Reference to the trigger button, used to anchor and position the dropdown panel. */
  @ViewChild("trigger", { static: true }) private triggerRef!: ElementRef<HTMLButtonElement>;

  /** Reference to the filter input, focused automatically whenever the panel opens with {@link filter} enabled. */
  @ViewChild("filterInput") private filterInputRef: ElementRef<HTMLInputElement> | undefined;

  /** Whether the dropdown panel is currently open. */
  protected open: boolean = false;

  /** The current text typed into the filter input. */
  protected filterText: string = "";

  /** Custom template for a regular option row. */
  protected get itemTemplate(): TemplateRef<any> | undefined {
    return TemplateUtil.findTemplate(this.templates, "item");
  }

  /** Custom template for the currently selected value shown in the trigger. */
  protected get selectedItemTemplate(): TemplateRef<any> | undefined {
    return TemplateUtil.findTemplate(this.templates, "selectedItem");
  }

  /** Custom template shown at the bottom of the panel. */
  protected get footerTemplate(): TemplateRef<any> | undefined {
    return TemplateUtil.findTemplate(this.templates, "footer");
  }

  /** The trigger element, used as the anchor for positioning the panel. */
  protected get triggerElement(): HTMLElement {
    return this.triggerRef.nativeElement;
  }

  /** The option object currently matching {@link value}, if any. */
  protected get selectedOption(): any {
    return this.options.find((option: any): boolean => this.getOptionValue(option) === this.value);
  }

  /** The options remaining after applying the current filter text. */
  protected get filteredOptions(): any[] {
    if (!this.filter || !this.filterText.trim()) return this.options;

    const keys: string[] = this.filterBy.split(",").map((key: string): string => key.trim()).filter(Boolean);
    const needle: string = this.filterText.trim().toLowerCase();

    return this.options.filter((option: any): boolean =>
      keys.some((key: string): boolean => String(option?.[key] ?? "").toLowerCase().includes(needle))
    );
  }

  /**
   * Reads the display label of an option.
   *
   * @param {any} option - The option to read the label from.
   * @returns {any} - The label to display.
   */
  protected getOptionLabel(option: any): any {
    return this.optionLabel ? option?.[this.optionLabel] : option;
  }

  /**
   * Reads the value of an option.
   *
   * @param {any} option - The option to read the value from.
   * @returns {any} - The value to store/emit.
   */
  protected getOptionValue(option: any): any {
    return this.optionValue ? option?.[this.optionValue] : option;
  }

  /**
   * Toggles the dropdown panel open/closed.
   *
   * @param {MouseEvent} event - The click event that triggered the toggle.
   */
  protected toggle(event: MouseEvent): void {
    event.stopPropagation();
    if (this.disabled) return;
    this.open = !this.open;
    if (this.open) {
      this.filterText = "";
      this.panelShow.emit();
      // The filter input doesn't exist yet (it's behind *ngIf="open") until change detection runs after this
      // handler returns, so focusing it has to happen on a later tick.
      if (this.filter) setTimeout(() => this.filterInputRef?.nativeElement.focus());
    }
  }

  /**
   * Selects an option, closes the panel, and emits the resulting change.
   *
   * @param {any} option - The option that was selected.
   * @param {Event} event - The event that triggered the selection.
   */
  protected selectOption(option: any, event: Event): void {
    const value: any = this.getOptionValue(option);
    this.value = value;
    this.open = false;
    this.valueChange.emit(value);
    this.selectionChange.emit({ value, originalEvent: event });
  }

  /**
   * Clears the current selection without opening/closing the panel.
   *
   * @param {Event} event - The click event on the clear button.
   */
  protected clear(event: Event): void {
    event.stopPropagation();
    this.value = null;
    this.valueChange.emit(null);
    this.selectionChange.emit({ value: null, originalEvent: event });
  }

  /**
   * Updates the current filter text from the search input.
   *
   * @param {Event} event - The input event of the filter field.
   */
  protected onFilterInput(event: Event): void {
    this.filterText = (event.target as HTMLInputElement).value;
  }

  /**
   * Angular `trackBy` function to optimize `*ngFor` rendering of options.
   *
   * @remarks
   *   - Declared as an arrow function property (rather than a regular method) because Angular's `NgForOf` invokes the
   *       `trackBy` function directly, without preserving `this` — a regular method would lose access to
   *       {@link getOptionValue}.
   */
  protected trackByFn = (index: number, option: any): any => this.getOptionValue(option) ?? index;
}
