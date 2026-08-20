import { Directive, ElementRef, Input, NgZone, OnDestroy, OnInit } from "@angular/core";
import { fromEvent, Subscription } from "rxjs";

/**
 * CopyDirective - Directive to handle copying text or objects to the clipboard.
 *
 * Inspired by https://netbasal.com/create-reusable-copy-to-clipboard-directive-in-angular-fc1139b9e755
 */
@Directive({
  selector: "[copy]",
  standalone: true,
})
export class CopyDirective implements OnInit, OnDestroy {
  /** The content to be copied to the clipboard. */
  @Input() copy: unknown;

  /** Subscription for click event on the host element. */
  private clickSubscription: Subscription | null = null;

  /**
   * @param {ElementRef<HTMLElement>} host - Reference to the host element.
   * @param {NgZone} zone - Angular's execution context.
   */
  constructor(
    private host: ElementRef<HTMLElement>,
    private zone: NgZone
  ) {}

  /** Initialize the directive. Subscribe to the click event of the host element. */
  ngOnInit(): void {
    this.zone.runOutsideAngular((): void => {
      this.clickSubscription = fromEvent(this.host.nativeElement, "click").subscribe((): void => {
        const contentToCopy: string = typeof this.copy === "string" ? this.copy : JSON.stringify(this.copy, null, 2);
        navigator.clipboard.writeText(contentToCopy);
      });
    });
  }

  /** Cleanup. Unsubscribe from the click event to prevent memory leaks. */
  ngOnDestroy(): void {
    // For the sake of independence this deliberately does not use the @AutoUnsubscribe
    this.clickSubscription?.unsubscribe();
  }
}
