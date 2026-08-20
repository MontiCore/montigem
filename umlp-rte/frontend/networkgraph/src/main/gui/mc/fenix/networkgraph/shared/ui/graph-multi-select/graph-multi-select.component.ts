import { ChangeDetectionStrategy, Component, ContentChildren, ElementRef, EventEmitter, Input, Output, QueryList, TemplateRef, ViewChild } from "@angular/core";
import { CommonModule } from "@angular/common";
import { GraphTemplateDirective } from "../../directives/graph-template.directive";
import { GraphOverlayPositionDirective } from "../../directives/graph-overlay-position.directive";
import { TemplateUtil } from "../../utils/template.util";
import { GraphMultiSelectChangeEvent } from "./graph-multi-select.types";

/**
 * GraphMultiSelectComponent is a lightweight, dependency-free replacement for PrimeNG's `p-select[multiple]`
 * (previously `p-multiselect`). It offers a checkbox-based multi-select dropdown with support for a custom item
 * template (via {@link GraphTemplateDirective}), used as a drop-in-ish replacement for the `filter-group` dropdown.
 *
 * @example
 *   <graph-multi-select
 *     [options]="groups"
 *     [(value)]="selectedGroups"
 *     optionLabel="groupKey"
 *     optionValue="groupKey"
 *     selectedItemsLabel="{0} groups selected"
 *     (selectionChange)="onSelectionChange($event)">
 *     <ng-template graphTemplate="item" let-item>{{ item.groupKey }}</ng-template>
 *   </graph-multi-select>
 */
@Component({
  selector: "graph-multi-select",
  standalone: true,
  imports: [CommonModule, GraphOverlayPositionDirective],
  templateUrl: "./graph-multi-select.component.html",
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraphMultiSelectComponent {
  /** The list of selectable options. */
  @Input() options: any[] = [];

  /** Key used to read the display label from an option object. If omitted, the option itself is used as the label. */
  @Input() optionLabel: string | undefined;

  /** Key used to read the value from an option object. If omitted, the option itself is used as the value. */
  @Input() optionValue: string | undefined;

  /** The currently selected values. */
  @Input() value: any[] | null = [];

  /** Placeholder text shown when nothing is selected. */
  @Input() placeholder: string = "";

  /** Template string (with a `{0}` placeholder for the count) used to summarize the current selection. */
  @Input() selectedItemsLabel: string = "";

  /** Whether the multi-select is disabled. */
  @Input() disabled: boolean = false;

  /** CSS class applied to the host/trigger. */
  @Input() styleClass: string = "";

  /** CSS class applied to the dropdown panel. */
  @Input() panelStyleClass: string = "";

  /** Emits the new selection array whenever it changes (supports `[(value)]`). */
  @Output() valueChange: EventEmitter<any[]> = new EventEmitter<any[]>();

  /** Emits detailed change information whenever the selection changes. */
  @Output() selectionChange: EventEmitter<GraphMultiSelectChangeEvent> = new EventEmitter<GraphMultiSelectChangeEvent>();

  /** Content-projected templates, keyed by their `graphTemplate` name. */
  @ContentChildren(GraphTemplateDirective) private templates: QueryList<GraphTemplateDirective> | undefined;

  /** Reference to the trigger button, used to anchor and position the dropdown panel. */
  @ViewChild("trigger", { static: true }) private triggerRef!: ElementRef<HTMLButtonElement>;

  /** Whether the dropdown panel is currently open. */
  protected open: boolean = false;

  /** Custom template for a regular option row. */
  protected get itemTemplate(): TemplateRef<any> | undefined {
    return TemplateUtil.findTemplate(this.templates, "item");
  }

  /** Custom template shown at the bottom of the panel. */
  protected get footerTemplate(): TemplateRef<any> | undefined {
    return TemplateUtil.findTemplate(this.templates, "footer");
  }

  /** The trigger element, used as the anchor for positioning the panel. */
  protected get triggerElement(): HTMLElement {
    return this.triggerRef.nativeElement;
  }

  /** A short summary of the current selection, shown in the trigger. */
  protected get triggerLabel(): string {
    const count: number = this.value?.length ?? 0;
    if (count === 0) return this.placeholder;
    if (this.selectedItemsLabel) return this.selectedItemsLabel.replace("{0}", String(count));
    return (this.value ?? []).map((value: any): any => this.getOptionLabelForValue(value)).join(", ");
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
   * Checks whether a given option is currently selected.
   *
   * @param {any} option - The option to check.
   * @returns {boolean} - True if the option's value is part of the current selection.
   */
  protected isSelected(option: any): boolean {
    return !!this.value?.includes(this.getOptionValue(option));
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
  }

  /**
   * Toggles the selection state of a single option and emits the resulting change.
   *
   * @param {any} option - The option that was clicked.
   * @param {Event} event - The event that triggered the toggle.
   */
  protected toggleOption(option: any, event: Event): void {
    const optionValue: any = this.getOptionValue(option);
    const current: any[] = this.value ?? [];
    const updated: any[] = current.includes(optionValue)
      ? current.filter((value: any): boolean => value !== optionValue)
      : [...current, optionValue];

    this.value = updated;
    this.valueChange.emit(updated);
    this.selectionChange.emit({ value: updated, originalEvent: event });
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

  /**
   * Resolves the display label for a previously selected value (used to build the default trigger summary).
   *
   * @param {any} value - The selected value.
   * @returns {any} - The label to display, falling back to the value itself if the option can no longer be found.
   */
  private getOptionLabelForValue(value: any): any {
    const option: any = this.options.find((candidate: any): boolean => this.getOptionValue(candidate) === value);
    return option ? this.getOptionLabel(option) : value;
  }
}
