import { Directive, Input, TemplateRef } from "@angular/core";

/**
 * GraphTemplateDirective marks an `<ng-template>` as a named, content-projected template for one of the `graph-*`
 * components (e.g. `graph-select`, `graph-multi-select`). This replaces PrimeNG's `pTemplate` mechanism.
 *
 * @example
 *   <graph-select>
 *     <ng-template graphTemplate="item" let-item>{{ item.label }}</ng-template>
 *   </graph-select>
 */
@Directive({
  selector: "[graphTemplate]",
  standalone: true,
})
export class GraphTemplateDirective {
  /** The name under which the host component looks up this template (e.g. "item", "selectedItem", "footer"). */
  @Input("graphTemplate") name: string = "";

  constructor(public template: TemplateRef<any>) {}
}
