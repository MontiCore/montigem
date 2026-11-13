/* (c) https://github.com/MontiCore/monticore */

import {Component, ElementRef, Input, ViewChild} from "@angular/core";
import { CommonModule } from '@angular/common';
import { config, FunctionDisplayComponent } from '../../../../../../build/gui2java2ts/mc/fenix/functiondisplay/FunctionDisplayComponent';
import ELK from 'elkjs/lib/elk.bundled';
import { Function4Display } from "../../../../../../build/gui2java2ts/mc/fenix/functiondisplay/functionstructure4fenix/Function4Display";
import { Function4DisplayBuilder } from "../../../../../../build/gui2java2ts/mc/fenix/functiondisplay/functionstructure4fenix/Function4DisplayBuilder";
import { Port4Display } from "../../../../../../build/gui2java2ts/mc/fenix/functiondisplay/functionstructure4fenix/Port4Display";
import { Channel4Display } from "../../../../../../build/gui2java2ts/mc/fenix/functiondisplay/functionstructure4fenix/Channel4Display";
import { Port4Elk, Funktion4Elk } from "./FunctionStructure4Fenix";
import { FunctionAction } from "../../../../../../build/gui2java2ts/mc/fenix/functiondisplay/functionstructure4fenix/FunctionAction";
import {GemListUtil, AsyncList, IUMLPList} from "@umlp/commonj2ts";

/**
 * Component for displaying and interacting with C&C (Component & Connector) models.
 * Supports ELK-based layouting, zooming, panning, and a minimap.
 */
@Component({
  standalone: true,
  imports: [...config.imports, CommonModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class FunctionDisplay extends FunctionDisplayComponent {
  /** The currently displayed function data. */
  public _data: Function4Display;

  /** ELK layout engine instance. */
  private elk = new ELK();
  /** The internal representation of the function tree for ELK. */
  private elkdata: Funktion4Elk;

  /** Maps port connection strings to their logical channels. */
  private connectionMap = new Map<string, Channel4Display>();
  /** Maps generated IDs to channel objects. */
  private channelIdMap = new Map<string, Channel4Display>();
  /** Maps CD ports to one or more ELK port representations. */
  private portElkMap = new Map<Port4Display, Port4Elk[]>();
  /** Maps CD functions to their rendered SVG elements. */
  private functionSvgMap = new Map<Function4Display, Element>();
  /** Maps logical channel IDs to their SVG element sets and the original channel. */
  private channelSvgMap = new Map<string, {elements: Set<Element>, channel: Channel4Display}>();
  /** Logical channel IDs that are currently hovered. */
  private hoveredChannelIds = new Set<string>();
  /** Maps CD ports to their rendered SVG elements. */
  private portSvgMap = new Map<Port4Display, Element>();

  /** Current pan/zoom state. */
  private isPanning = false;
  private startPanClient = { x: 0, y: 0 };
  private startViewBox = { x: 0, y: 0 };
  private currentViewBox = { x: 0, y: 0, w: 0, h: 0 };
  private originalViewBox = { x: 0, y: 0, w: 0, h: 0 };
  private hasDragged = false;
  private panListenersAttached = false;

  /** Context menu visibility and position. */
  public contextMenuVisible = false;
  public contextMenuX = 0;
  public contextMenuY = 0;
  /** The function on which the context menu was opened. */
  private contextMenuTargetFunction: Function4Display | null = null;

  @ViewChild('container', { static: false }) container!: ElementRef;

  /** Internal ID for the root wrapper node. */
  private static wrapperId = '----------------'

  constructor() {
    super();
  }

  /** Cache for logical channel identification. */
  private channelIdentityMap: Channel4Display[] = [];

  /**
   * Generates a stable logical ID for a channel based on its ports.
   * This is necessary because the same channel can be represented by multiple edges in ELK.
   */
  private async getLogicalChannelId(channel: Channel4Display): Promise<string> {
    if (!channel) return "";

    for (let i = 0; i < this.channelIdentityMap.length; i++) {
       let existing = this.channelIdentityMap[i];
       if (existing === channel) return "ch_" + i;

       let ports1 = (await GemListUtil.toArraySync(await existing.getPort4DisplayList())) || [];
       let ports2 = (await GemListUtil.toArraySync(await channel.getPort4DisplayList())) || [];
       if (ports1.length === ports2.length && ports1.length > 0) {
          let allMatch = true;
          for (let p of ports1) {
             if (!ports2.includes(p)) { allMatch = false; break; }
          }
          if (allMatch) {
             return "ch_" + i;
          }
       }
    }

    this.channelIdentityMap.push(channel);
    return "ch_" + (this.channelIdentityMap.length - 1);
  }

  /**
   * Main entry point for the component. When data is set, the entire diagram is
   * recalculated using ELK and then rendered as SVG.
   */
  @Input()
  public set data(data: Function4Display) {
    if (data == null) {return;}

    // Avoid unnecessary re-layouts if data didn't change
    if (this._data === data) {
      return;
    }

    // Reset internal state for the new model
    this.channelIdentityMap = [];
    this.connectionMap = new Map<string, Channel4Display>();
    this.channelIdMap = new Map<string, Channel4Display>();
    this.portElkMap = new Map<Port4Display, Port4Elk[]>();
    this.functionSvgMap = new Map<Function4Display, Element>();
    this.channelSvgMap = new Map<string, {elements: Set<Element>, channel: Channel4Display}>();
    this.hoveredChannelIds = new Set<string>();
    this.portSvgMap = new Map<Port4Display, Element>();
    this._data = data;

    (async () => {
      // 1. Transform CD model to ELK format (wrapped in a root node)
      this.elkdata = await this.funk2elkfunk(await this.addWrapper(data), 1);
      let transformed = this.transform(this.elkdata);

      // 2. Perform ELK layouting
      let layouted = this.elk.layout(transformed, {
        layoutOptions: {
          'elk.port.borderOffset': "-10",
          'elk.nodeLabels.placement': "[H_CENTER, V_CENTER, INSIDE]",
          "elk.layered.spacing.baseValue": "40",
          "elk.spacing.portPort": "10",
          "elk.spacing.portsSurrounding": "[top=10,left=10,bottom=10,right=10]",
          "elk.nodeLabels.padding": "[top=30,left=10,bottom=10,right=10]",
          "elk.layered.considerModelOrder.portModelOrder": "true",
          "elk.portLabels.placement": "INSIDE",
          "elk.portAlignment.west": "BEGIN",
          "elk.portAlignment.east": "END",
          "elk.alignment": "AUTOMATIC",
          "elk.padding": "[top=30,left=30,bottom=30,right=30]",
          "elk.layered.nodePlacement.strategy": "SIMPLE",
        }
      });

      // 3. Render the resulting layout as SVG
      layouted.then(async v => {
        const containerElement = this.container.nativeElement;

        if (containerElement.firstChild) {
          containerElement.innerHTML = '';
        }

        containerElement.appendChild(await this.renderNode(v));

        // Setup viewbox for pan/zoom
        this.currentViewBox = {x: 0, y: 0, w: v.width, h: v.height};
        this.originalViewBox = {x: 0, y: 0, w: v.width, h: v.height};
        containerElement.setAttribute("viewBox", `${this.currentViewBox.x} ${this.currentViewBox.y} ${this.currentViewBox.w} ${this.currentViewBox.h}`);
        containerElement.setAttribute('preserveAspectRatio', 'xMidYMid meet');

        // Styling the container
        containerElement.style.width = `100%`;
        containerElement.style.height = `100%`;
        containerElement.style.maxHeight = `500px`;
        containerElement.style.minHeight = `400px`;
        containerElement.style.border = `1px solid #ccc`;
        containerElement.style.borderRadius = `4px`;
        containerElement.style.backgroundColor = `#fdfdfd`;
        containerElement.style.cursor = `default`;

        this.attachPanListeners();
        this.addOutsideClickListener();

        // Handle Minimap rendering if requested
        const parent = containerElement.parentElement;
        if (parent) {
          if (getComputedStyle(parent).position === 'static') {
            parent.style.position = 'relative';
          }

          const oldMinimap = parent.querySelector('.function-minimap-container');
          if (oldMinimap) {
            parent.removeChild(oldMinimap);
          }

          if (this.isShowMinimap() !== false) {
            this.initMinimap(containerElement, parent);
          }
        }
      });

      layouted.catch(e => console.error(e));
    })();
  }

  /**
   * Initializes and renders the minimap overlay.
   */
  private initMinimap(containerElement: SVGSVGElement, parent: HTMLElement) {
    const minimapContainer = document.createElement('div');
    minimapContainer.className = 'function-minimap-container';
    minimapContainer.style.position = 'absolute';
    minimapContainer.style.bottom = '20px';
    minimapContainer.style.left = '20px';
    minimapContainer.style.width = '150px';
    minimapContainer.style.height = '100px';
    minimapContainer.style.backgroundColor = 'rgba(255, 255, 255, 0.9)';
    minimapContainer.style.border = '1px solid #aaa';
    minimapContainer.style.borderRadius = '4px';
    minimapContainer.style.boxShadow = '0 2px 4px rgba(0,0,0,0.1)';
    minimapContainer.style.zIndex = '9';
    minimapContainer.style.overflow = 'hidden';
    minimapContainer.style.cursor = 'pointer';

    const svgNS = "http://www.w3.org/2000/svg";
    const minimapSvg = document.createElementNS(svgNS, 'svg') as unknown as SVGSVGElement;
    minimapSvg.style.width = '100%';
    minimapSvg.style.height = '100%';
    minimapSvg.style.display = 'block';
    minimapSvg.setAttribute("viewBox", `${this.originalViewBox.x} ${this.originalViewBox.y} ${this.originalViewBox.w} ${this.originalViewBox.h}`);
    minimapSvg.setAttribute("preserveAspectRatio", "xMidYMid meet");

    // Copy the main SVG content to the minimap
    minimapSvg.innerHTML = containerElement.innerHTML;

    const indicator = document.createElement('div');
    indicator.style.position = 'absolute';
    indicator.style.border = '2px solid rgba(0, 120, 215, 0.8)';
    indicator.style.backgroundColor = 'rgba(0, 120, 215, 0.2)';
    indicator.style.boxSizing = 'border-box';
    indicator.style.pointerEvents = 'none';
    indicator.style.transition = 'all 0.1s ease-out';

    minimapContainer.appendChild(minimapSvg);
    minimapContainer.appendChild(indicator);
    parent.appendChild(minimapContainer);

    /**
     * Updates the minimap highlight rectangle based on the current viewbox.
     * Complex math is needed to account for preserveAspectRatio scaling (letterboxing/pillarboxing).
     */
    const updateMinimapIndicator = () => {
      const mapW = 150;
      const mapH = 100;
      const rect = containerElement.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) return;

      const viewRatio = this.currentViewBox.w / this.currentViewBox.h;
      const containerRatio = rect.width / rect.height;
      let visibleW = this.currentViewBox.w;
      let visibleH = this.currentViewBox.h;

      // Account for pillarboxing or letterboxing
      if (containerRatio > viewRatio) {
          visibleW = this.currentViewBox.h * containerRatio;
      } else {
          visibleH = this.currentViewBox.w / containerRatio;
      }

      const origW = this.originalViewBox.w;
      const origH = this.originalViewBox.h;
      if (origW === 0 || origH === 0) return;

      const mapRatio = mapW / mapH;
      const origRatio = origW / origH;
      let minimapScale;
      let xOffset = 0;
      let yOffset = 0;

      if (mapRatio > origRatio) {
          minimapScale = mapH / origH;
          xOffset = (mapW - (origW * minimapScale)) / 2;
      } else {
          minimapScale = mapW / origW;
          yOffset = (mapH - (origH * minimapScale)) / 2;
      }

      const visibleX = this.currentViewBox.x - (visibleW - this.currentViewBox.w) / 2;
      const visibleY = this.currentViewBox.y - (visibleH - this.currentViewBox.h) / 2;
      const indX = xOffset + ((visibleX - this.originalViewBox.x) * minimapScale);
      const indY = yOffset + ((visibleY - this.originalViewBox.y) * minimapScale);
      const indW = visibleW * minimapScale;
      const indH = visibleH * minimapScale;

      indicator.style.left = `${indX}px`;
      indicator.style.top = `${indY}px`;
      indicator.style.width = `${indW}px`;
      indicator.style.height = `${indH}px`;
    };

    updateMinimapIndicator();

    // Minimap interaction: jump to clicked area
    minimapContainer.addEventListener('mousedown', (e: MouseEvent) => {
        e.preventDefault();
        const rect = minimapContainer.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        const clickY = e.clientY - rect.top;
        const mapW = 150;
        const mapH = 100;
        const origW = this.originalViewBox.w;
        const origH = this.originalViewBox.h;
        const effectiveScale = Math.min(mapW / origW, mapH / origH);
        const xOffset = (mapW - (origW * effectiveScale)) / 2;
        const yOffset = (mapH - (origH * effectiveScale)) / 2;
        const targetSvgX = this.originalViewBox.x + ((clickX - xOffset) / effectiveScale);
        const targetSvgY = this.originalViewBox.y + ((clickY - yOffset) / effectiveScale);
        this.currentViewBox.x = targetSvgX - (this.currentViewBox.w / 2);
        this.currentViewBox.y = targetSvgY - (this.currentViewBox.h / 2);
        this.constrainViewBox();
        containerElement.setAttribute('viewBox', `${this.currentViewBox.x} ${this.currentViewBox.y} ${this.currentViewBox.w} ${this.currentViewBox.h}`);
        updateMinimapIndicator();
    });

    // Hook into attribute changes to keep the minimap synced
    const originalSetAttribute = containerElement.setAttribute.bind(containerElement);
    containerElement.setAttribute = function(name: string, value: string) {
        originalSetAttribute(name, value);
        if (name === 'viewBox') {
            updateMinimapIndicator();
        }
    };
  }


  /**
   * Input for programmatically selecting functions. Highlights them in the SVG.
   */
  public override setSelectedFunctions(f: Function4Display[]){
    if (f == null){
      return;
    }
    this.functionSvgMap.forEach((value, key) => {
      if (f.includes(key)){
        this.markElement(value, key.getBorderColorHover());
      } else {
        this.unmarkElement(value, key.getBorderColor());
      }
    })
    super.setSelectedFunctions(f);
  }

  /**
   * Input for programmatically selecting channels. Highlights them in the SVG.
   */
  public override setSelectedChannels(c: Channel4Display[]){
    (async () => {
      let selectedIds = new Set<string>();
      if (c != null) {
        for (const ch of c) {
          if (ch) {
            selectedIds.add(await this.getLogicalChannelId(ch));
          }
        }
      }
      this.channelSvgMap.forEach((data, id) => {
        let isHighlighted = selectedIds.has(id) || this.hoveredChannelIds.has(id);
        data.elements.forEach(el => {
          if (isHighlighted) {
            this.markElement(el, data.channel.getLineColorHover());
          } else {
            this.unmarkElement(el, data.channel.getLineColor());
          }
        });
      });
      super.setSelectedChannels(c);
    })();
  }

  /**
   * Reconciles temporary hover highlighting with persistent channel selection.
   */
  private async refreshChannelHighlight(channelId: string) {
    let isSelected = false;
    if (!this.hoveredChannelIds.has(channelId)) {
      for (const channel of (this.getSelectedChannels() || [])) {
        if (await this.getLogicalChannelId(channel) === channelId) {
          isSelected = true;
          break;
        }
      }
    }

    const data = this.channelSvgMap.get(channelId);
    const isHighlighted = this.hoveredChannelIds.has(channelId) || isSelected;
    data?.elements.forEach(element => {
      if (isHighlighted) {
        this.markElement(element, data.channel.getLineColorHover());
      } else {
        this.unmarkElement(element, data.channel.getLineColor());
      }
    });
  }

  /**
   * Input for programmatically selecting ports. Highlights them in the SVG.
   */
  public override setSelectedPorts(p: Port4Display[]){
    if (p == null){
      return;
    }
    this.portSvgMap.forEach((value, key) => {
      if (p.includes(key)){
        this.markElement(value, key.getBorderColorHover());
      } else {
        this.unmarkElement(value, key.getBorderColor());
      }
    })
    super.setSelectedPorts(p);
  }

  /**
   * Ensures the current viewbox stays within the boundaries of the original diagram.
   */
  private constrainViewBox() {
    if (this.currentViewBox.w > this.originalViewBox.w) {
      this.currentViewBox.w = this.originalViewBox.w;
    }
    if (this.currentViewBox.h > this.originalViewBox.h) {
      this.currentViewBox.h = this.originalViewBox.h;
    }
    if (this.currentViewBox.x < this.originalViewBox.x) {
      this.currentViewBox.x = this.originalViewBox.x;
    } else if (this.currentViewBox.x + this.currentViewBox.w > this.originalViewBox.x + this.originalViewBox.w) {
      this.currentViewBox.x = this.originalViewBox.x + this.originalViewBox.w - this.currentViewBox.w;
    }
    if (this.currentViewBox.y < this.originalViewBox.y) {
      this.currentViewBox.y = this.originalViewBox.y;
    } else if (this.currentViewBox.y + this.currentViewBox.h > this.originalViewBox.y + this.originalViewBox.h) {
      this.currentViewBox.y = this.originalViewBox.y + this.originalViewBox.h - this.currentViewBox.h;
    }
  }

  /**
   * Attaches mouse and wheel listeners to the SVG for panning and zooming.
   * Also creates the zoom instruction overlay and the reset home button.
   */
  private attachPanListeners() {
    if (this.panListenersAttached) return;
    const svg = this.container.nativeElement;
    const parent = svg.parentElement;

    // Zoom instruction overlay
    const overlay = document.createElement('div');
    overlay.style.position = 'absolute';
    overlay.style.top = '20px';
    overlay.style.left = '50%';
    overlay.style.transform = 'translateX(-50%)';
    overlay.style.padding = '8px 16px';
    overlay.style.borderRadius = '20px';
    overlay.style.backgroundColor = 'rgba(0, 0, 0, 0.7)';
    overlay.style.color = 'white';
    overlay.style.fontSize = '14px';
    overlay.style.fontFamily = 'sans-serif';
    overlay.style.pointerEvents = 'none';
    overlay.style.opacity = '0';
    overlay.style.transition = 'opacity 0.3s ease';
    overlay.style.zIndex = '10';
    overlay.innerText = 'Use "Ctrl" + scroll to zoom';

    // Optional Home/Reset Button
    let resetBtn = parent?.querySelector('.reset-pan-btn') as HTMLButtonElement;
    if (this.isShowResetButton() !== false) {
      if (parent && !resetBtn) {
        if (getComputedStyle(parent).position === 'static') {
          parent.style.position = 'relative';
        }
        resetBtn = document.createElement('button');
        resetBtn.className = 'reset-pan-btn';
        resetBtn.innerHTML = '&#8962;';
        resetBtn.title = 'Reset view';
        resetBtn.style.position = 'absolute';
        resetBtn.style.bottom = '20px';
        resetBtn.style.right = '20px';
        resetBtn.style.width = '32px';
        resetBtn.style.height = '32px';
        resetBtn.style.borderRadius = '4px';
        resetBtn.style.border = '1px solid #ccc';
        resetBtn.style.backgroundColor = 'white';
        resetBtn.style.cursor = 'pointer';
        resetBtn.style.boxShadow = '0 2px 4px rgba(0,0,0,0.1)';
        resetBtn.style.zIndex = '10';
        resetBtn.style.display = 'flex';
        resetBtn.style.alignItems = 'center';
        resetBtn.style.justifyContent = 'center';
        resetBtn.style.fontSize = '18px';

        resetBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          this.currentViewBox = { ...this.originalViewBox };
          svg.setAttribute('viewBox', `${this.currentViewBox.x} ${this.currentViewBox.y} ${this.currentViewBox.w} ${this.currentViewBox.h}`);
        });
        parent.appendChild(resetBtn);
      }
    } else if (resetBtn && parent) {
      parent.removeChild(resetBtn);
    }

    if (parent) {
      if (getComputedStyle(parent).position === 'static') {
        parent.style.position = 'relative';
      }
      parent.appendChild(overlay);
    }

    let overlayTimeout: any;
    const showOverlay = () => {
      overlay.style.opacity = '1';
      clearTimeout(overlayTimeout);
      overlayTimeout = setTimeout(() => {
        overlay.style.opacity = '0';
      }, 800);
    };

    // Panning logic
    svg.addEventListener('mousedown', (e: MouseEvent) => {
      e.preventDefault();
      this.isPanning = true;
      this.hasDragged = false;
      this.startPanClient = { x: e.clientX, y: e.clientY };
      this.startViewBox = { x: this.currentViewBox.x, y: this.currentViewBox.y };
      svg.style.cursor = 'grabbing';
    });

    svg.addEventListener('mousemove', (e: MouseEvent) => {
      if (!this.isPanning) return;
      const dx = e.clientX - this.startPanClient.x;
      const dy = e.clientY - this.startPanClient.y;

      // Minimum threshold to distinguish click from drag
      if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
        this.hasDragged = true;
      }

      if (this.hasDragged) {
        const rect = svg.getBoundingClientRect();
        const ratio = Math.max(this.currentViewBox.w / rect.width, this.currentViewBox.h / rect.height);
        this.currentViewBox.x = this.startViewBox.x - (dx * ratio);
        this.currentViewBox.y = this.startViewBox.y - (dy * ratio);
        this.constrainViewBox();
        svg.setAttribute('viewBox', `${this.currentViewBox.x} ${this.currentViewBox.y} ${this.currentViewBox.w} ${this.currentViewBox.h}`);
      }
    });

    const endPan = () => {
      if (this.isPanning) {
        this.isPanning = false;
        svg.style.cursor = 'default';
        // Delay resetting hasDragged to prevent triggering click events right after drag
        setTimeout(() => { this.hasDragged = false; }, 50);
      }
    };

    svg.addEventListener('mouseup', endPan);
    svg.addEventListener('mouseleave', endPan);

    // Zooming logic
    svg.addEventListener('wheel', (e: WheelEvent) => {
      if (!e.ctrlKey && !e.metaKey) {
        showOverlay();
        return;
      }
      e.preventDefault();
      const zoomFactor = e.deltaY > 0 ? 1.1 : 0.9;
      const rect = svg.getBoundingClientRect();
      const ratio = Math.max(this.currentViewBox.w / rect.width, this.currentViewBox.h / rect.height);
      const mouseSvgX = this.currentViewBox.x + (e.clientX - rect.left) * ratio;
      const mouseSvgY = this.currentViewBox.y + (e.clientY - rect.top) * ratio;

      this.currentViewBox.w *= zoomFactor;
      this.currentViewBox.h *= zoomFactor;
      this.currentViewBox.x += (mouseSvgX - this.currentViewBox.x) * (1 - zoomFactor);
      this.currentViewBox.y += (mouseSvgY - this.currentViewBox.y) * (1 - zoomFactor);
      this.constrainViewBox();
      svg.setAttribute('viewBox', `${this.currentViewBox.x} ${this.currentViewBox.y} ${this.currentViewBox.w} ${this.currentViewBox.h}`);
    });

    this.panListenersAttached = true;
  }

  ngOnInit() {
  }

  private generateID (): string {
    return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
  }

  /**
   * Adds a transparent wrapper around the root function to ensure it is layouted
   * correctly as a child of a containing system.
   */
  private async addWrapper(funk: Function4Display): Promise<Function4Display> {
    const builder = (await new Function4DisplayBuilder().__Function4DisplayBuilderConstructor())
      .name(FunctionDisplay.wrapperId);
    await builder.subFunction(await new AsyncList<Function4Display>().__AsyncListConstructor([funk]));
    return builder.build();
  }

  /**
   * Recursively transforms the CD function model into an intermediate ELK-friendly format.
   * Calculates ports and logical connections.
   */
  private async funk2elkfunk(funk: Function4Display, depth: number): Promise<Funktion4Elk> {
    let ret : Funktion4Elk = {name: funk.getName(), parts: null, connections: null, ports:null, self:funk};
    let ports: Port4Elk[] = await this.getTransformedPorts(funk, ret);

    // Base case for recursion
    if (depth < 0){
      return {name: funk.getName(), parts: [], connections: [], ports:ports, self:funk};
    }

    let subports : Port4Elk[] = [];
    let subfunkElks : Funktion4Elk[] = [];
    if (await funk.getSubFunctionList() != null) {
      for await (let sf of await funk.getSubFunctionList()) {
        let sfelk = await this.funk2elkfunk(sf, depth - 1);
        subfunkElks.push(sfelk);
        subports.push(...(await this.getTransformedPorts(sf, sfelk)));
      }
    }

    // Calculate logical connections between ports in this level
    let connections : [Port4Elk,Port4Elk][] = [];
    for(let port of ports){
      if (port.self.getChannel4Display()) {
        for await (let innerPort of await port.self.getChannel4Display().getPort4DisplayList()){
          if (!this.portElkMap.has(innerPort)) continue;
          for (let innerPortElk of this.portElkMap.get(innerPort)) {
            // Check for connections between this function's ports and its children's ports
            if (innerPortElk.isIn && subports.includes(innerPortElk)){
              connections.push([port, innerPortElk]);
              this.connectionMap.set(this.portsToString(port, innerPortElk), port.self.getChannel4Display());
            }
            if (!innerPortElk.isIn && subports.includes(innerPortElk) && innerPortElk != port){
              connections.push([innerPortElk,port])
              this.connectionMap.set(this.portsToString(innerPortElk, port), port.self.getChannel4Display());
            }
          }
        }
      }

      // Connections between ports of the same level (sibling connections)
      for(let toPort of ports){
        if (toPort != port && port.isIn && !toPort.isIn && toPort.self.getChannel4Display() &&
          await (await toPort.self.getChannel4Display().getPort4DisplayList()).contains(port.self)){
          connections.push([port,toPort]);
          this.connectionMap.set(this.portsToString(port, toPort), port.self.getChannel4Display());
        }
      }
    }

    // Connections from children's output ports to other sibling children's input ports
    for(let port of subports){
      if (!port.isIn && port.self.getChannel4Display()){
        for await (let toPort of await port.self.getChannel4Display().getPort4DisplayList()){
          if (!this.portElkMap.has(toPort)){
            continue;
          }
          for (let toPortElk of this.portElkMap.get(toPort)) {
            if (toPortElk.isIn && subports.includes(toPortElk)){
              connections.push([port, toPortElk]);
              this.connectionMap.set(this.portsToString(port, toPortElk), port.self.getChannel4Display())
            }
          }
        }
      }
    }
    ret.ports = ports;
    ret.parts = subfunkElks;
    ret.connections = connections
    return ret;
  }

  /**
   * Extracts and transforms input/output ports from a CD function.
   */
  private async getTransformedPorts(funk : Function4Display, elkfunk: Funktion4Elk) : Promise<Port4Elk[]> {
    let ports : Port4Elk[] = [];
    if (await funk.getInList() != null){
      ports.push(...(await GemListUtil.toArraySync(await funk.getInList())).map(p => {
        let ret = {id:funk.getName() + '.' + p.getName(), isIn: true, funktion:elkfunk, self:p}
        if (!this.portElkMap.has(p)) this.portElkMap.set(p, []);
        this.portElkMap.get(p).push(ret);
        return ret;
      }));
    }
    if (await funk.getOutList() != null) {
      ports.push(...(await GemListUtil.toArraySync(await funk.getOutList())).map(p => {
        let ret = {id: funk.getName() + '.' + p.getName(), isIn: false, funktion: elkfunk, self: p}
        if (!this.portElkMap.has(p)) this.portElkMap.set(p, []);
        this.portElkMap.get(p).push(ret);
        return ret;
      }));
    }
    return ports;
  }

  /**
   * Final transformation of the intermediate model into the ELK JS format
   * which includes specific layouting hints.
   */
  public transform(part: Funktion4Elk){
    return {
      id: part.name,
      labels: [{
        text: part.name, height: 8, width: part.name.length * 7, layoutOptions: {
          ...(part.parts.length > 0 ? { 'elk.nodeLabels.placement': "[H_CENTER, V_TOP, INSIDE]" } : {})
        },
      }],
      ports: part.ports.map(p => ({
        layoutOptions: {
          'elk.port.side': p.isIn  ? 'WEST' : 'EAST',
        },
        id: p.id,
        width: 20,
        height: 20,
        title: `${p.id}`,
        direction: p.isIn? 'IN': 'OUT',
        labels: [{ text: p.id.split('.').pop(), height: 17.5, width: p.id.length * 7.4, hasChildren: part.parts.length > 0, }],
        port4display: p.self
      })),
      edges: part.connections.map(c => ({
        id: `${this.generateIDAndLinkToChannel(c[0], c[1])}`, sources: [c[0].id], targets: [c[1].id],
        channel4Display: c[0].self.getChannel4Display()
      })),
      children: part.parts.map(p => this.transform(p)),
      width: 240,
      height: 180,
      layoutOptions: {
        'org.eclipse.elk.portConstraints': 'FIXED_SIDE',
        "elk.alignment": "TOP",
        ...(part.parts.length > 0 ? { 'elk.nodeLabels.placement': "[H_CENTER, V_TOP, INSIDE]" } : {})
      },
      funktion4display: part.self
    }
  }

  /**
   * Recursively renders the layouted ELK nodes as SVG elements.
   * This includes the boxes, labels, ports, images, burger menus, and connection lines.
   */
  public async renderNode(node) {
    let svgNS = "http://www.w3.org/2000/svg";
    let g = document.createElementNS(svgNS, "g");
    if (!node.funktion4display.isVisible()) {
      return g;
    }

    let isWrapper = node.id === FunctionDisplay.wrapperId;
    g.setAttribute("transform", `translate(${node.x || 0}, ${node.y || 0})`);
    g.setAttribute("opacity", "1");

    // Main rectangle for the component
    let rect = document.createElementNS(svgNS, "rect");
    rect.setAttribute("id", `function-${node.id}`);
    rect.setAttribute("data-system", node.id);
    rect.setAttribute("x", "0");
    rect.setAttribute("y", "0");
    rect.setAttribute("width", `${node.width}`);
    rect.setAttribute("height", `${node.height}`);

    if (isWrapper) {
      rect.setAttribute("fill", "transparent");
      rect.setAttribute("stroke", "transparent");
      g.appendChild(rect);
    } else {
      rect.setAttribute("fill", `${node.funktion4display.getBackgroundColor()}`);
      rect.setAttribute("stroke", `${node.funktion4display.getBorderColor()}`);

      // Interaction listeners
      rect.addEventListener('click', () => this.functionClicked(node.funktion4display));
      rect.addEventListener('contextmenu', (e) => this.showContextMenu(e as MouseEvent, node.funktion4display));
      rect.addEventListener('mouseover', () => this.functionHovered(node.funktion4display));
      rect.addEventListener('mouseout', () => this.functionMouseouted(node.funktion4display));

      // Handle programmatic and mouse selection highlighting
      if (this.getSelectedFunctions() && this.getSelectedFunctions().includes(node.funktion4display)){
        this.markElement(rect, node.funktion4display.getBorderColorHover());
      }
      rect.addEventListener('mouseover', () => {
        this.markElement(rect, node.funktion4display.getBorderColorHover());
      })
      rect.addEventListener('mouseout', () => {
        if (!this.getSelectedFunctions() || !this.getSelectedFunctions().includes(node.funktion4display)){
          this.unmarkElement(rect, node.funktion4display.getBorderColor());
        }
      })

      this.functionSvgMap.set(node.funktion4display, rect);
      g.appendChild(rect);

      // Render optional Image/Icon - Rendered before labels so labels are on top
      if (!isWrapper && node.funktion4display.getImageUrl()) {
        const img = document.createElementNS(svgNS, "image");
        img.setAttribute("href", node.funktion4display.getImageUrl());

        // Increase size: 75% of node height
        const imgSize = node.height * 0.75;
        // Center horizontally
        img.setAttribute("x", `${(node.width - imgSize) / 2}`);
        // Position slightly lower to leave space for the title at the top
        img.setAttribute("y", `${(node.height - imgSize) / 2 + 15}`);
        img.setAttribute("width", `${imgSize}`);
        img.setAttribute("height", `${imgSize}`);
        img.setAttribute("style", "pointer-events: none; opacity: 0.9;");
        g.appendChild(img);
      }

      // Render optional Burger Menu Icon for context actions
      const actions = this.getContextMenuActions() || [];
      if (!isWrapper && this.isShowBurgerMenu() !== false && actions.length > 0) {
        const burgerG = document.createElementNS(svgNS, "g");
        burgerG.setAttribute("class", "burger-menu-icon");
        burgerG.setAttribute("style", "cursor: pointer;");

        const hitArea = document.createElementNS(svgNS, "rect");
        hitArea.setAttribute("x", `${node.width - 25}`);
        hitArea.setAttribute("y", "5");
        hitArea.setAttribute("width", "20");
        hitArea.setAttribute("height", "20");
        hitArea.setAttribute("fill", "transparent");
        burgerG.appendChild(hitArea);

        for (let i = 0; i < 3; i++) {
          const line = document.createElementNS(svgNS, "line");
          line.setAttribute("x1", `${node.width - 22}`);
          line.setAttribute("y1", `${10 + i * 5}`);
          line.setAttribute("x2", `${node.width - 8}`);
          line.setAttribute("y2", `${10 + i * 5}`);
          line.setAttribute("stroke", "#666");
          line.setAttribute("stroke-width", "2");
          line.setAttribute("stroke-linecap", "round");
          burgerG.appendChild(line);
        }

        burgerG.addEventListener('click', (e) => {
          e.stopPropagation();
          this.showContextMenu(e as MouseEvent, node.funktion4display);
        });
        burgerG.addEventListener('mouseover', () => {
          burgerG.querySelectorAll('line').forEach(l => l.setAttribute('stroke', '#000'));
        });
        burgerG.addEventListener('mouseout', () => {
          burgerG.querySelectorAll('line').forEach(l => l.setAttribute('stroke', '#666'));
        });

        g.appendChild(burgerG);
      }

      // Component Labels
      for (let label of node.labels ?? []) {
        let text = document.createElementNS(svgNS, "text");
        text.setAttribute("x", `${label.x}`);
        text.setAttribute("y", `${label.y}`);
        text.setAttribute("font-size", "12");
        // Add white outline for better readability on images
        text.setAttribute("style", "paint-order: stroke; stroke: white; stroke-width: 3px; stroke-linecap: round; stroke-linejoin: round; font-weight: bold;");
        text.textContent = label.text;
        g.appendChild(text);
      }

      // Port rendering
      let incomingPortsG = document.createElementNS(svgNS, "g");
      incomingPortsG.setAttribute("class", "incoming-ports");
      let outgoingPortsG = document.createElementNS(svgNS, "g");
      outgoingPortsG.setAttribute("class", "outgoing-ports");
      for (let port of node.ports ?? []) {
        if (!port.port4display.isVisible()) continue;
        let portG = document.createElementNS(svgNS, "g");
        portG.setAttribute("transform", `translate(${port.x}, ${port.y})`);
        let portRect = document.createElementNS(svgNS, "rect");
        portRect.setAttribute("class", "port");
        portRect.setAttribute("data-port", port.id);
        portRect.setAttribute("x", "0");
        portRect.setAttribute("y", "0");
        portRect.setAttribute("width", `${port.width}`);
        portRect.setAttribute("height", `${port.height}`);
        portRect.setAttribute("fill", `${port.port4display.getBackgroundColor()}`);
        portRect.setAttribute("stroke", `${port.port4display.getBorderColor()}`);

        portRect.addEventListener('click', () => this.portClicked(port.port4display));
        portRect.addEventListener('mouseover', () => this.portHovered(port.port4display));
        portRect.addEventListener('mouseout', () => this.portMouseouted(port.port4display));

        if (this.getSelectedPorts() && this.getSelectedPorts().includes(port.port4display)) {
          this.markElement(portRect, port.port4display.getBorderColorHover());
        }
        portRect.addEventListener('mouseover', () => {
          this.markElement(portRect, port.port4display.getBorderColorHover());
        });
        portRect.addEventListener('mouseout', () => {
          if (!this.getSelectedPorts() || !this.getSelectedPorts().includes(port.port4display)) {
            this.unmarkElement(portRect, port.port4display.getBorderColor());
          }
        });
        this.portSvgMap.set(port.port4display, portRect);
        portG.appendChild(portRect);

        let title = document.createElementNS(svgNS, "title");
        title.textContent = port.title;
        portG.appendChild(title);

        // Arrow indicator inside port
        let path = document.createElementNS(svgNS, "path");
        path.setAttribute("fill", "none");
        path.setAttribute("stroke", `${port.port4display.getBorderColor()}`);
        path.setAttribute("stroke-miterlimit", "10");
        path.setAttribute("pointer-events", "none");
        path.setAttribute("d", "M 0 -5 L 5 0 L 0 5 M 5 0 L -5 0");
        path.setAttribute("transform", "translate(10, 10)");
        portG.appendChild(path);

        // Port Labels
        for (let label of port.labels ?? []) {
          if (!port.port4display.isLabelVisible()) continue;
          let text = document.createElementNS(svgNS, "text");
          if (port.direction === "OUT") {
            text.setAttribute("text-anchor", "end");
            text.setAttribute("x", `${port.x - 235}`);
          } else {
            text.setAttribute("text-anchor", "start");
            text.setAttribute("x", `${port.x + 35}`);
          }
          text.setAttribute("y", `${label.hasChildren ? label.y : 14}`);
          text.setAttribute("font-family", "Menlo, Monaco, Courier New, monospace");
          text.setAttribute("font-size", "12");
          text.textContent = label.text;
          portG.appendChild(text);
        }
        if (port.direction === "OUT") outgoingPortsG.appendChild(portG);
        else incomingPortsG.appendChild(portG);
      }
      g.appendChild(incomingPortsG);
      g.appendChild(outgoingPortsG);
    }

    // Edge (Channel) rendering
    for (let edge of node.edges ?? []) {
      if (!edge.channel4Display.isVisible()) continue;
      let channelId = await this.getLogicalChannelId(edge.channel4Display);
      if (!this.channelSvgMap.has(channelId)) {
        this.channelSvgMap.set(channelId, { elements: new Set<Element>(), channel: edge.channel4Display });
      }
      for (let section of edge.sections) {
        let edgeGroup = document.createElementNS(svgNS, "g");

        // Visible line
        let polyline = document.createElementNS(svgNS, "polyline");
        polyline.setAttribute("class", "channel-line");
        polyline.setAttribute("id", edge.id);
        polyline.setAttribute("points", `${section.startPoint.x},${section.startPoint.y} ${(section.bendPoints ?? []).map(p => `${p.x},${p.y}`).join(" ")} ${section.endPoint.x},${section.endPoint.y}`);
        polyline.setAttribute("stroke", `${edge.channel4Display.getLineColor()}`);
        polyline.setAttribute("stroke-width", "1");
        polyline.setAttribute("fill", "none");
        polyline.setAttribute("pointer-events", "none");
        this.channelSvgMap.get(channelId)?.elements.add(polyline);

        // Transparent thick hit area for easier clicking
        let hiddenPolyline = document.createElementNS(svgNS, "polyline");
        hiddenPolyline.setAttribute("class", "channel-hit-area");
        hiddenPolyline.setAttribute("points", `${section.startPoint.x},${section.startPoint.y} ${(section.bendPoints ?? []).map(p => `${p.x},${p.y}`).join(" ")} ${section.endPoint.x},${section.endPoint.y}`);
        hiddenPolyline.setAttribute("stroke", "transparent");
        hiddenPolyline.setAttribute("stroke-width", "15");
        hiddenPolyline.setAttribute("fill", "none");
        hiddenPolyline.setAttribute("pointer-events", "stroke");
        hiddenPolyline.setAttribute("style", "cursor: pointer;");

        hiddenPolyline.addEventListener('click', (event) => {
          event.stopPropagation();
          this.channelClicked(edge.channel4Display);
        });
        hiddenPolyline.addEventListener('mouseover', () => {
          this.hoveredChannelIds.add(channelId);
          void this.refreshChannelHighlight(channelId);
          this.channelHovered(edge.channel4Display);
        });
        hiddenPolyline.addEventListener('mouseout', () => {
          this.hoveredChannelIds.delete(channelId);
          void this.refreshChannelHighlight(channelId);
          this.channelMouseouted(edge.channel4Display);
        });

        edgeGroup.appendChild(polyline);
        edgeGroup.appendChild(hiddenPolyline);

        // Arrow head
        let path = document.createElementNS(svgNS, "path");
        path.setAttribute("d", "M 0 0 L -7 -3.5 L -5.25 0 L -7 3.5 Z");
        path.setAttribute("transform", `translate(${section.endPoint.x - 1},${section.endPoint.y})`);
        path.setAttribute("fill", `${edge.channel4Display.getLineColor()}`);
        path.setAttribute("stroke", `${edge.channel4Display.getLineColor()}`);
        path.setAttribute("pointer-events", "none");
        edgeGroup.appendChild(path);

        g.appendChild(edgeGroup);
      }
    }

    // Recursively render children
    for (let child of node.children ?? []) {
      g.appendChild(await this.renderNode(child));
    }
    return g;
  }

  /**
   * Listens for clicks on the background wrapper to trigger the outsideClick event.
   */
  public addOutsideClickListener(){
    let element = document.getElementById('function-' + FunctionDisplay.wrapperId);
    if (element) {
      element.addEventListener('click', () => this.outsideClicked())
    }
  }

  // --- Event Dispatchers ---

  public functionClicked(f: Function4Display){
    if (this.hasDragged) return;
    this.closeContextMenu();
    if (this.getFunctionClick()) this.getFunctionClick()(f);
  }

  public functionHovered(f: Function4Display){
    if (this.getFunctionMouseover()) this.getFunctionMouseover()(f);
  }

  public functionMouseouted(f: Function4Display){
    if (this.getFunctionMouseout()) this.getFunctionMouseout()(f);
  }

  /**
   * Opens the context menu at the current mouse position.
   */
  private showContextMenu(event: MouseEvent, f: Function4Display) {
    const actions = this.getContextMenuActions() || [];
    if (actions.length === 0) return;

    event.preventDefault();
    event.stopPropagation();

    this.setSelectedFunctions([f]);
    this.contextMenuTargetFunction = f;

    const rect = this.container.nativeElement.getBoundingClientRect();
    this.contextMenuX = event.clientX - rect.left;
    this.contextMenuY = event.clientY - rect.top;
    this.contextMenuVisible = true;
  }

  public closeContextMenu() {
    this.contextMenuVisible = false;
  }

  /**
   * Executes the specific callback of a context menu action.
   */
  public handleContextMenuClick(action: FunctionAction) {
    this.closeContextMenu();
    if (action.getFunctionClick() && this.contextMenuTargetFunction) {
        action.getFunctionClick()(this.contextMenuTargetFunction);
    }
  }

  public channelHovered(channel: Channel4Display){
    if (this.getChannelMouseover()) this.getChannelMouseover()(channel);
  }

  public channelClicked(channel: Channel4Display){
    if (this.hasDragged) return;
    this.closeContextMenu();
    if (this.getChannelClick()) this.getChannelClick()(channel);
  }

  public channelMouseouted(channel: Channel4Display){
    if (this.getChannelMouseout()) this.getChannelMouseout()(channel)
  }

  private outsideClicked(){
    if (this.hasDragged) return;
    this.closeContextMenu();
    if (this.getOutsideClick()) this.getOutsideClick()();
  }

  private portClicked(p: Port4Display){
    if (this.hasDragged) return;
    this.closeContextMenu();
    if (this.getPortClick()) this.getPortClick()(p)
  }

  private portHovered(p: Port4Display){
    if (this.getPortMouseover()) this.getPortMouseover()(p);
  }

  private portMouseouted(p: Port4Display){
    if (this.getPortMouseout()) this.getPortMouseout()(p)
  }

  // --- Helper Methods ---

  /**
   * Links a new ELK edge ID to its logical CD channel.
   */
  private generateIDAndLinkToChannel(p1: Port4Elk, p2: Port4Elk) {
    let id = this.generateID();
    this.channelIdMap.set(id, this.connectionMap.get(this.portsToString(p1, p2)));
    return id;
  }

  private portsToString(p1: Port4Elk, p2: Port4Elk) {
    return `${p1.id} -> ${p2.id}`;
  }

  /**
   * Visually marks an element (thickness + color).
   */
  private markElement(element: Element, markedColor: string) {
    element.setAttribute('stroke', markedColor);
    element.setAttribute('stroke-width', '5');
  }

  /**
   * Resets the visual marking of an element.
   */
  private unmarkElement(element: Element, unmarkedColor: string) {
    element.setAttribute('stroke', unmarkedColor);
    element.setAttribute('stroke-width', '1');
  }
}
