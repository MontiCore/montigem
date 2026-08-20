import { QueryList, TemplateRef } from "@angular/core";
import { GraphTemplateDirective } from "../directives/graph-template.directive";

/** Utility for looking up a named, content-projected `graphTemplate` from a `@ContentChildren` query result. */
export class TemplateUtil {
  /**
   * Finds a template by its `graphTemplate` name.
   *
   * @param {QueryList<GraphTemplateDirective> | undefined} templates - The queried templates.
   * @param {string} name - The name to look for.
   * @returns {TemplateRef<any> | undefined} - The matching template, if any.
   */
  public static findTemplate(templates: QueryList<GraphTemplateDirective> | undefined, name: string): TemplateRef<any> | undefined {
    return templates?.find((template: GraphTemplateDirective): boolean => template.name === name)?.template;
  }
}
