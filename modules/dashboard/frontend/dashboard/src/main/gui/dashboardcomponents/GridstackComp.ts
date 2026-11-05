import { CellConfig } from '../../../../build/umlpJava2ts/dashboardmodule/CellConfig';
import { Component, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { ComponentObserverManager, GemIdentifiable } from '@umlp/commonj2ts';
import { Cell } from '../../../../build/gui2java2ts/dashboardcomponents/Cell';
import { GridstackCompComponent, config } from '../../../../build/gui2java2ts/dashboardcomponents/GridstackCompComponent';
import { OnDestroy, Input , OnChanges, OnInit} from '@angular/core';
import { ApplicationRef, createComponent, ElementRef, EnvironmentInjector, inject } from '@angular/core';
import { ComponentRef } from '@angular/core';
import { GemListUtil } from '@umlp/commonj2ts';
import { GemIcon } from '@umlp/basic';
import {NgGridStackWidget, GridstackComponent, BaseWidget, NgCompInputs, NgGridStackOptions} from "gridstack/dist/angular"

type TileGeometry = { id: string; x: number; y: number; w: number; h: number };

@Component({
  standalone: true,
  imports: [...config.imports, GridstackComponent, GemIcon],
  selector: config.selector,
  templateUrl: "./GridstackComp.html",
  styles: config.styles,
  styleUrls: [...config.styleUrls],
  changeDetection: ChangeDetectionStrategy.Eager
})
export class GridstackComp extends GridstackCompComponent implements OnDestroy {
  constructor() {
    super();
    // register all our dynamic components types created by the grid
    console.log('Constructor runs');
    GridstackComponent.addComponentToSelectorType([CellWrapper]);
  }
  private envInjector = inject(EnvironmentInjector);
  private appRef = inject(ApplicationRef);
  private host: ElementRef<HTMLElement> = inject(ElementRef);
  private hostResizeObserver?: ResizeObserver;
  // number of grid rows that exactly fill the available page height; the row height only depends on the
  // page size, so cells keep their size and a layout with more rows grows beyond the page (scrolls)
  private static readonly ROWS_PER_PAGE = 8;
  //save previous states
  private wrappers = new Map<number, ComponentRef<CellWrapper>>();
  private currentWidgets = new Map<number, HTMLElement & { gridstackNode?: any }>();
  private geometryObserverKey = '';
  private geometryUpdateTimer?: number;
  private gestureActive = false;
  private destroyed = false;

  public gridOptions: NgGridStackOptions = {
    margin: 4,
    float: true,
    minRow: GridstackComp.ROWS_PER_PAGE,
    children: []
  };

  // .. therefore we have to use this gridComp variable where we can manipulate the Widgets?
  @ViewChild(GridstackComponent)
  gridComp!: GridstackComponent;


  // We use this method instead of ngOnInit which is handled in a extended class
  public override async init() {
    // With this mechanism we use MontiGem's Update Check i.e. when view changes it will call
    // this.reactOnViewInputChange -> so all our logic is handled there
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_view",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_0",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_edit",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_1",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_create",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_2",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_changeRunning",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_3",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_editCell",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_4",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_editCellFlag",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_5",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_tutorialStep",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_6",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_changeTutorialStep",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_7",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_delete",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_8",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_changeDelete",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_9",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_changeDownload",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_10",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    ComponentObserverManager.addStateVariableObserver(
        this,
        "_download",
        this.reactOnViewInputChange.bind(this),
        "myCustomSubscription_11",
        "dashboardcomponents.GridstackComp" + this.uniqueIdentifier
    );
    super.init()
  }

  public async createWidget(cell: CellConfig): Promise<void> {

    //create the widget component from cell data
    const size = await cell.getSize();
    const pos = await cell.getPosition();
    const x = await pos.getX();
    const y = await pos.getY();
    const width = await size.getWidth();
    const height = await size.getHeight();
    const id = await cell.getGemId();
    const widget = this.gridComp.grid.addWidget({
      id: String(id),
      x: x ?? 0,
      y: y ?? 0,
      w: width ?? 3,
      h: height ?? 3,
      minW: 3,
      minH: 3
    });
    //create corresponding CellWrapper and save context
    const container = widget.querySelector('.grid-stack-item-content');
    if (!container) return;
    const compRef = createComponent(CellWrapper, {
      environmentInjector: this.envInjector
    });
    this.wrappers.set(id, compRef);
    this.currentWidgets.set(id, widget);
    //widget delete function
    const deleteWidget = () => this.removeWidget(id);
    //pass down arguments
    compRef.instance.cell = cell;
    compRef.instance.edit = this.isEdit();
    compRef.instance.editCellFlag = this.isEditCellFlag();
    compRef.instance.create = this.isCreate();
    compRef.instance.delete = this.isDelete();
    compRef.instance.download = this.isDownload();
    compRef.instance.deleteWidget = deleteWidget;
    compRef.instance.changeDelete = (value: boolean) => {
      this.changeDelete.emit(value);
    };
    compRef.instance.editCell = (id: number) => {
      this.editCell.emit(id);
    };
    compRef.instance.tutorialStep = this.tutorialStep;
    compRef.instance.changeTutorialStep = (numberOfSteps: number) => {
      this.changeTutorialStep.emit(numberOfSteps);
    };
    compRef.instance.changeDownload = (value: boolean) => {
      this.changeDownload.emit(value);
    };
    compRef.instance.viewGemId = this.getView().getGemId();

    this.appRef.attachView(compRef.hostView);
    const domElement = compRef.location.nativeElement as HTMLElement;
    domElement.id = 'cell-container-' + id;
    container.appendChild(compRef.location.nativeElement);
  }

  private removeWidget(id: number): void {
    const widget = this.currentWidgets.get(id);
    const compRef = this.wrappers.get(id);
    this.currentWidgets.delete(id);
    this.wrappers.delete(id);
    if (widget) this.gridComp.grid.removeWidget(widget);
    if (compRef) {
      this.appRef.detachView(compRef.hostView);
      compRef.destroy();
    }
  }

  public async synchronizeWidgetToCell(cell: CellConfig): Promise<void> {

    const widget = this.currentWidgets.get(cell.getGemId());
    const activeNode = widget?.gridstackNode;
    if (!activeNode) return;

    const size = await cell.getSize();
    const sizeW = await size.getWidth();
    const sizeH = await size.getHeight();

    const pos = await cell.getPosition();
    const posX = await pos.getX();
    const posY = await pos.getY();

    const safeX = typeof activeNode.x === 'number' ? activeNode.x : 0;
    const safeY = typeof activeNode.y === 'number' ? activeNode.y : 0;
    const safeW = typeof activeNode.w === 'number' ? activeNode.w : 3;
    const safeH = typeof activeNode.h === 'number' ? activeNode.h : 3;

    //if there was any changes to the cells parameters update them
    if (sizeW != safeW || sizeH != safeH) {
      await size.setWidth(safeW);
      await size.setHeight(safeH);
    }
    if (safeX !== posX || safeY !== posY) {
      await pos.setX(safeX);
      await pos.setY(safeY);
    }
  }

  public async updateWidget(widgetId: number): Promise<void> {
    //get the ref of the CellWrapper
    const compRef = this.wrappers.get(widgetId);
    if (!compRef) return;
    //update arguments
    compRef.instance.edit = this.isEdit();
    compRef.instance.editCellFlag = this.editCellFlag;
    compRef.instance.create = this.create;
    compRef.instance.delete = this.delete;
    compRef.instance.download = this.isDownload();
    compRef.instance.changeDelete = (value: boolean) => {
      this.changeDelete.emit(value);
    };
    compRef.instance.editCell = (id: number) => {
      this.editCell.emit(id);
    };
    compRef.instance.tutorialStep = this.tutorialStep;
    compRef.instance.changeTutorialStep = (numberOfSteps: number) => {
      this.changeTutorialStep.emit(numberOfSteps);
    };
    compRef.instance.changeDownload = (value: boolean) => {
      this.changeDownload.emit(value);
    };
    compRef.instance.viewGemId = this.getView().getGemId();

  }
  // Inputs and domain notifications can arrive in several steps. Serialize synchronization
  // so concurrent runs cannot create the same widget twice.
  private syncQueue: Promise<void> = Promise.resolve();

  public reactOnViewInputChange() : Promise<void> {
    this.syncQueue = this.syncQueue
      .then(() => this.doReactOnViewInputChange())
      .catch(e => console.error('GridstackComp synchronization failed', e));
    return this.syncQueue;
  }

  private async doReactOnViewInputChange() : Promise<void> {
    // view not yet passed down or grid not yet created -> a later input change / ngAfterViewInit retriggers
    if (this.destroyed || !this.getView() || !this.gridComp?.grid) {
      return;
    }
    this.changeRunning.emit(true);
    try {
      await this.synchronize();
    } finally {
      this.changeRunning.emit(false);
    }
  }

  ngAfterViewInit() {
    this.reactOnViewInputChange();
    this.gridComp.grid.on('dragstart resizestart', () => { this.gestureActive = true; });
    // Persist all affected tiles, including those pushed by a drag.
    this.gridComp.grid.on('dragstop resizestop', () => {
      this.gestureActive = false;
      const view = this.getView();
      this.syncQueue = this.syncQueue.then(async () => {
        if (this.destroyed || !view || view !== this.getView()) return;
        const cells = await GemListUtil.toArraySync(view.getCellsList());
        for (const cell of cells) {
          await this.synchronizeWidgetToCell(cell);
        }
        this.scheduleGeometryUpdate();
      }).catch(e => console.error('GridstackComp layout save failed', e));
    });
    this.hostResizeObserver = new ResizeObserver(() => this.fitCellHeightToPage());
    this.hostResizeObserver.observe(this.host.nativeElement);
  }

  override ngOnDestroy() {
    this.destroyed = true;
    window.clearTimeout(this.geometryUpdateTimer);
    ComponentObserverManager.removeObservers(this.geometryObserverOwner);
    this.hostResizeObserver?.disconnect();
    super.ngOnDestroy();
  }

  private get geometryObserverOwner(): string {
    return 'dashboardcomponents.GridstackComp.geometry' + this.uniqueIdentifier;
  }

  private scheduleGeometryUpdate(): void {
    if (this.destroyed) return;
    window.clearTimeout(this.geometryUpdateTimer);
    // A layout arrives as several attribute updates. Coalesce them before loading the whole grid.
    this.geometryUpdateTimer = window.setTimeout(() => {
      this.geometryUpdateTimer = undefined;
      void this.reactOnViewInputChange();
    }, 40);
  }

  private async observeGeometry(cells: CellConfig[]): Promise<void> {
    // Observe membership too: backend tools can add cells without opening the creation dialog.
    const objects: GemIdentifiable[] = [this.getView()];
    for (const cell of cells) {
      objects.push(cell, await cell.getPosition(), await cell.getSize());
    }
    if (this.destroyed) return;
    const key = objects.map(object => object.getGemId()).join(',');
    if (key === this.geometryObserverKey) return;
    ComponentObserverManager.removeObservers(this.geometryObserverOwner);
    this.geometryObserverKey = key;
    for (const object of objects) {
      ComponentObserverManager.addDomainObjectObserver(object, () => this.scheduleGeometryUpdate(),
        'geometry', this.geometryObserverOwner);
    }
  }

  private async applyModelGeometry(cells: CellConfig[]): Promise<void> {
    if (this.destroyed || this.gestureActive) return;
    const layout: TileGeometry[] = [];
    for (const cell of cells) {
      const position = await cell.getPosition();
      const size = await cell.getSize();
      layout.push({id: String(cell.getGemId()), x: position.getX(), y: position.getY(),
        w: size.getWidth(), h: size.getHeight()});
    }
    // Attribute notifications can span frames. Do not let GridStack resolve a temporary overlap
    // halfway through a swap; a later geometry notification will supply the remaining positions.
    for (let i = 0; i < layout.length; i++) {
      const a = layout[i];
      for (let j = i + 1; j < layout.length; j++) {
        const b = layout[j];
        if (a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h) return;
      }
    }
    if (this.destroyed || this.gestureActive) return;
    const changed = layout.some(item => {
      const node = this.currentWidgets.get(Number(item.id))?.gridstackNode;
      return node && (node.x !== item.x || node.y !== item.y || node.w !== item.w || node.h !== item.h);
    });
    if (changed) this.gridComp.grid.load(layout, false);
  }

  // the host gets the remaining page height from the layout (not from the grid content), so this only runs when
  // the page size changes
  private fitCellHeightToPage(): void {
    const grid = this.gridComp?.grid;
    const available = this.host.nativeElement.clientHeight;
    if (!grid || available <= 0) {
      return;
    }
    const cellHeight = available / GridstackComp.ROWS_PER_PAGE;
    if (cellHeight !== grid.getCellHeight()) {
      grid.cellHeight(cellHeight);
    }
  }

  private async synchronize() : Promise<void> {
    const cells : CellConfig[] = await GemListUtil.toArraySync(this.getView().getCellsList());
    const cellIds = new Set(cells.map(cell => cell.getGemId()));
    for (const id of this.currentWidgets.keys()) {
      if (!cellIds.has(id)) this.removeWidget(id);
    }
    for (const cell of cells) {
      if (!this.currentWidgets.has(cell.getGemId())) {
        await this.createWidget(cell);
      }
    }
    const grid = this.gridComp.grid.save() as NgGridStackWidget[];
    for (const widget of grid) {
      await this.updateWidget(Number(widget.id));
    }
    await this.observeGeometry(cells);
    await this.applyModelGeometry(cells);
    this.gridComp.grid.disable();
    if (this.isEdit()) {
      this.gridComp.grid.enable();
    }
    // the first resize notification can arrive before the page layout is final
    this.fitCellHeightToPage();
  }
}

@Component({
  standalone: true,
  selector: 'app-cell',
  imports: [...config.imports, Cell],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `<cell [cell]="cell" [create]="create" [activateEditing]="edit" [tutorialStep]="tutorialStep" [editCellFlag]="editCellFlag" [viewGemId]="viewGemId" (editCell)="editCell($event)" (changeTutorialStep)="changeTutorialStep($event)" [delete]="delete" (deleteWidget)="deleteWidget()" (changeDelete)="changeDelete($event)"  [download]="download" (changeDownload)="changeDownload($event)"></cell>`,
})
export class CellWrapper extends BaseWidget implements OnDestroy {
  @Input() cell!: CellConfig;
  @Input() edit!: boolean;
  @Input() editCellFlag!: boolean;
  @Input() tutorialStep!: number;
  @Input() create!: boolean;
  @Input() viewGemId!: number;
  @Input() delete!: boolean;
  @Input() download!: boolean;
  @Input() deleteWidget!: () => void;
  @Input() editCell!: (number) => void;
  @Input() changeTutorialStep!: (number) => void;
  @Input() changeDownload!: (boolean) => void;
  @Input() changeDelete!: (boolean) => void;

  ngOnDestroy() {
  }
}


