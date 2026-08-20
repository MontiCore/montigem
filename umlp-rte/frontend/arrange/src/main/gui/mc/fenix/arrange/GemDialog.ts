/* (c) https://github.com/MontiCore/monticore */

import {
  HorizontalConnectionPos,
  Overlay,
  OverlayConfig,
  OverlayRef,
  VerticalConnectionPos
} from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  TemplateRef,
  ViewContainerRef
} from '@angular/core';
import { config, GemDialogComponent } from '../../../../../../build/gui2java2ts/mc/fenix/arrange/GemDialogComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  template: ``
})
export class GemDialog extends GemDialogComponent {
  // ###################### Variables ###################################

  overlayRef!: OverlayRef;
  config!: OverlayConfig;
  templatePortal!: TemplatePortal<any>;

  // ###################### Inputs ######################################
  _enableBackdrop: boolean = true;
  // Positioning Strategy can be "global" | "flexible" (default: "global")
  @Input()
  positionStrategy: string = "global";
  // Interface for opening / closing the overlay.
  _isOpen: boolean = false;

  @Input()
  set isOpen(value: boolean) {
    this._isOpen = value;
    this.toggleOverlay()
  }

  get isOpen(): boolean {
    return this._isOpen;
  }

  @Output() isOpenChange: EventEmitter<boolean> = new EventEmitter();

  // Relevant if positionStrategy = "global"
  // Vertical distance between top of page and overlay. Value can be in % or px (default: Overlay is centered).
  _top: string = "auto";
  // Horizontal distance between left of page and overlay. Value can be in % or px (default: Overlay is centered).
  _left: string = "auto";

  // Relevant if positionStrategy = "flexible"
  // HTML Id of an element as anchor. Positioning of the overlay is relative to the anchor.
  @Input()
  overlayOriginId!: string;
  // Horizontal position of the anchor point on the anchor element. Value can be "start" | "center" | "end" (default: "center").
  @Input()
  originX: HorizontalConnectionPos = "center";
  // Vertical position of the anchor point on the anchor element. Value can be "top" | "center" | "bottom" (default: "center").
  @Input()
  originY: VerticalConnectionPos = "center";
  // Horizontal position of the overlay relative to the anchor point. Value can be "start" | "center" | "end" (default: "center").
  @Input()
  overlayX: HorizontalConnectionPos = "center";
  // Vertical position of the overlay relative to the anchor point. Value can be "top" | "center" | "bottom" (default: "center").
  @Input()
  overlayY: VerticalConnectionPos = "center";

  // ###################### Functions ###################################

  constructor(public overlay: Overlay, public viewContainerRef: ViewContainerRef) {
    super();
  }

  ngAfterViewInit() { }

  /**
   * Opens / closes the overlay when the input parameter isOpen changes
   */
  toggleOverlay() {
    this.isOpenChange.emit(this._isOpen);
    if (this._isOpen) {
      this.openOverlay()
    }
    else if (!this._isOpen) {
      this.closeOverlay()
    }
  }

  /**
   * Opens the overlay and sets its config
   */
  openOverlay() {
    try {
      this.config = new OverlayConfig();

      if (this.positionStrategy == "flexible") {

        // Get HTML element and set it as the anchor needed for flexible postions strategy
        const overlayOrigin = document.getElementById(this.overlayOriginId);

        if (overlayOrigin) {
          this.config.positionStrategy = this.overlay
            .position()
            .flexibleConnectedTo(overlayOrigin)
            .withPositions([
              {
                originX: this.originX,
                originY: this.originY,
                overlayX: this.overlayX,
                overlayY: this.overlayY,
              },
            ]);
        } else {
          // If no anchor is found, the position strategy defaults to "global"
          this.positionStrategy = "global";
        }
      }

      if (this.positionStrategy == "global") {
        // Select correct case dependent on availability of positionTop / positionLeft
        // If positionTop / positionLeft is not set, default to center
        let top = this.top === "auto" ? null : this.top;
        let left = this.left === "auto" ? null : this.left;
        if (top && left) {
          this.config.positionStrategy = this.overlay
            .position()
            .global()
            .top(top)
            .left(left);
        } else if (top) {
          this.config.positionStrategy = this.overlay
            .position()
            .global()
            .top(top)
            .centerHorizontally();
        } else if (left) {
          this.config.positionStrategy = this.overlay
            .position()
            .global()
            .centerVertically()
            .left(left);
        } else {
          this.config.positionStrategy = this.overlay
            .position()
            .global()
            .centerVertically()
            .centerHorizontally();
        }
      }

      this.config.hasBackdrop = this.enableBackdrop;

      this.overlayRef = this.overlay.create(this.config);

      // Template with content is attached to an overlay with a TemplatePortal
      this.templatePortal = new TemplatePortal(this.component, this.viewContainerRef);
      this.overlayRef.attach(this.templatePortal);

      // Configure actions on backdrop
      this.overlayRef.backdropClick().subscribe(() => {
        // Close overlay and update isOpen
        this.isOpen = false;
      });
    } catch (error) {
      this.isOpen = false;
    }
  }

  /**
   * Closes the overlay
   */
  closeOverlay() {
    // Check if there is an overlay
    if (this.overlayRef) {
      // Close overlay
      this.overlayRef.dispose();
    }
  }

  public ngOnInit(): void { }
}
