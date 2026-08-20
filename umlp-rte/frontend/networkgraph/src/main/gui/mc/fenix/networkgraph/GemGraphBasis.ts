/* (c) https://github.com/MontiCore/monticore */

import {
  afterEveryRender,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  effect,
  ElementRef,
  EventEmitter,
  inject,
  NgZone,
  SimpleChange,
  SimpleChanges,
} from "@angular/core";
import { downloadChartAsPNG } from "./export/graph.exporter";
import { Utils } from "./shared/utils/utils";
import { Observable, Subscriber, switchMap } from "rxjs";
import { Sigma } from "sigma";
import { SigmaEvents } from "sigma/types";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { GraphManagerService } from "./data/graph-manager.service";
import { MouseCaptorEvents } from "sigma/types";
import { TouchCaptorEvents } from "sigma/types";
import { AutoUnsubscribe } from "./shared/decorators/auto-unsubscribe.decorator";
import {
  EventParameter,
  GraphEventEmitter,
  LazyEventType,
  MouseCaptorEventEmitter,
  TouchCaptorEventEmitter,
} from "./types/graph-events.types";
import { SerializedGraph } from "graphology-types";

/**
 * The `GraphBasisComponent` serves as an abstract base component for rendering graphs utilizing Sigma.js. It provides
 * basic functionalities for managing graph container, handling graph events, and interacting with the
 * `GraphManagerService` to facilitate graph rendering and manipulation.
 */
@Component({
  template: "",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
@AutoUnsubscribe()
export abstract class GemGraphBasis {
  /** Service for managing graph data and interactions. */
  public graphManager: GraphManagerService = inject(GraphManagerService);

  /** Reference to the change detector for the component. */
  public cdReference: ChangeDetectorRef = inject(ChangeDetectorRef);

  /** Service for executing work inside or outside of the Angular zone. */
  protected ngZone: NgZone = inject(NgZone);

  /** Indicates whether the environment supports WebGL. */
  protected hasWebGLSupport!: boolean;

  /** Reference to the HTML element containing the graph. */
  protected abstract container: ElementRef<HTMLDivElement> | undefined;

  /** Indicates whether the graph container has been initialized. */
  private containerInitialized: boolean = false;

  constructor() {
    this.checkWebGLSupport();
    afterEveryRender((): void => {
      if (this.isContainerReadyForInit()) {
        this.containerInitialized = true;
        this.graphManager.setContainer(this.container?.nativeElement);
      }
    });

    effect((): void => {
      this.graphManager.needsInitializationSignalTracked;
      this.cdReference.detectChanges();
    });
  }

  /**
   * Returns the graph data that is currently being rendered.
   *
   * @returns {Partial<SerializedGraph>} The graph data.
   */
  public getGraphData(): Partial<SerializedGraph> {
    return this.graphManager.graphData.getGraphData();
  }

  /** Requests a resize operation through the `GraphManagerService`. */
  public resize(): void {
    this.graphManager.resize();
  }

  /**
   * Initiates a download of the graph as a PNG image.
   *
   * @param {string} fileName - The name of the file.
   */
  public downloadAsPNG(fileName: string): void {
    this.ngZone.runOutsideAngular(() => {
      if (this.graphManager.renderer) downloadChartAsPNG(this.graphManager.renderer, fileName);
    });
  }

  /**
   * Utility function that checks if a simple change actually has a modified the value.
   *
   * @param {SimpleChanges} changes - Angular changes object.
   * @param {keyof this} key - The property key to check.
   * @param {boolean} compareValues - Whether to compare the previous and current values.
   * @returns {boolean} - Whether any change is a first change or has a real value change.
   */
  protected hasChanged(changes: SimpleChanges, key: keyof this, compareValues: boolean = true): boolean {
    if (!(key in changes)) return false;
    if (!compareValues) return true;
    const change: SimpleChange = changes[key as string];
    return change.firstChange || !Utils.isEqual(change.previousValue, change.currentValue);
  }

  /**
   * Creates a lazy graph event emitter.
   *
   * @template K - The key of the event to emit.
   * @param {K} eventName - The name of the event to emit.
   * @returns {GraphEventEmitter<K>} - The event emitter.
   */
  protected createLazyGraphEvent<K extends keyof SigmaEvents>(eventName: K): GraphEventEmitter<K> {
    return this.createLazyEvent<SigmaEvents, K>(
      (graph: Sigma, handler: SigmaEvents[K]) => graph.on(eventName, handler),
      (graph: Sigma, handler: SigmaEvents[K]) => graph.off(eventName, handler)
    );
  }

  /**
   * Creates a lazy mouse captor event emitter.
   *
   * @template K - The key of the event to emit.
   * @param {K} eventName - The name of the event to emit.
   * @returns {MouseCaptorEventEmitter<K>} - The event emitter.
   */
  protected createLazyMouseEvent<K extends keyof MouseCaptorEvents>(eventName: K): MouseCaptorEventEmitter<K> {
    return this.createLazyEvent<MouseCaptorEvents, K>(
      (graph: Sigma, handler: MouseCaptorEvents[K]) => graph.getMouseCaptor().on(eventName, handler),
      (graph: Sigma, handler: MouseCaptorEvents[K]) => graph.getMouseCaptor().off(eventName, handler)
    );
  }

  /**
   * Creates a lazy touch captor event emitter.
   *
   * @template K - The key of the event to emit.
   * @param {K} eventName - The name of the event to emit.
   * @returns {TouchCaptorEventEmitter<K>} - The event emitter.
   */
  protected createLazyTouchEvent<K extends keyof TouchCaptorEvents>(eventName: K): TouchCaptorEventEmitter<K> {
    return this.createLazyEvent<TouchCaptorEvents, K>(
      (graph: Sigma, handler: TouchCaptorEvents[K]) => graph.getTouchCaptor().on(eventName, handler),
      (graph: Sigma, handler: TouchCaptorEvents[K]) => graph.getTouchCaptor().off(eventName, handler)
    );
  }

  /** Checks for WebGL support in the environment. */
  private checkWebGLSupport(): void {
    const canvas: HTMLCanvasElement = document.createElement("canvas");
    const gl: RenderingContext | null = canvas.getContext("webgl") || canvas.getContext("experimental-webgl");
    this.hasWebGLSupport = gl instanceof WebGLRenderingContext;
  }

  /**
   * Initializes the graph container if not already initialized.
   *
   * @returns {boolean} - True when the container exists, has not yet been initialized, and has a valid width and
   *   height.
   */
  private isContainerReadyForInit(): this is { container: ElementRef<HTMLDivElement> } {
    return (
      !this.containerInitialized &&
      !!this.container &&
      this.container.nativeElement.offsetWidth > 0 &&
      this.container.nativeElement.offsetHeight > 0
    );
  }

  /**
   * Creates a lazy event emitter.
   *
   * @template TEventTypes - The type of the events.
   * @template K - The key of the event to emit.
   * @param {(graph: Sigma, handler: TEventTypes[K]) => void} onEvent - The function to call to attach the event
   *   handler.
   * @param {(graph: Sigma, handler: TEventTypes[K]) => void} offEvent - The function to call to detach the event
   *   handler.
   * @returns {EventEmitter<EventParameter<TEventTypes[K]>>} - The event emitter.
   */
  private createLazyEvent<TEventTypes extends LazyEventType, K extends keyof TEventTypes>(
    onEvent: (graph: Sigma, handler: TEventTypes[K]) => void,
    offEvent: (graph: Sigma, handler: TEventTypes[K]) => void
  ): EventEmitter<EventParameter<TEventTypes[K]>> {
    return this.graphManager.onSigmaInit$.pipe(
      switchMap(
        (graph: Sigma) =>
          new Observable((obs: Subscriber<EventParameter<TEventTypes[K]>>) => {
            const handler: TEventTypes[K] = ((...arguments_: EventParameter<TEventTypes[K]>[]): void => {
              this.ngZone.run(() => {
                for (const arg of arguments_) {
                  obs.next(arg);
                }
              });
            }) as TEventTypes[K];
            onEvent(graph, handler);
            return () => offEvent(graph, handler);
          })
      ),
      takeUntilDestroyed()
    ) as EventEmitter<EventParameter<TEventTypes[K]>>;
  }
}
