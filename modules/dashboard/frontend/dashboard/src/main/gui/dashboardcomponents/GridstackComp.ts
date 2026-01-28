import { CellConfig } from '../../../../build/umlpJava2ts/dashboardmodule/CellConfig';
import { Component, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { ComponentObserverManager } from '@umlp/commonj2ts';
import { Cell } from '../../../../build/gui2java2ts/dashboardcomponents/Cell';
import { GridstackCompComponent, config } from '../../../../build/gui2java2ts/dashboardcomponents/GridstackCompComponent';
import { OnDestroy, Input , OnChanges, OnInit} from '@angular/core';
import { ApplicationRef, createComponent, ElementRef, EnvironmentInjector, inject } from '@angular/core';
import { ComponentRef } from '@angular/core';
import { GemListUtil } from '@umlp/commonj2ts';
import {NgGridStackWidget, GridstackComponent, BaseWidget, NgCompInputs, NgGridStackOptions} from "gridstack/dist/angular"

@Component({
  standalone: true,
  imports: [...config.imports, GridstackComponent],
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
  private prevCells: CellConfig[] = [];
  private prevEdit = false;
  private prevCreate = false;

  public gridOptions: NgGridStackOptions = {
    margin: 4,
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
    const height = await size.getLength();
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
    const deleteWidget = () => {
      this.currentWidgets.delete(Number(widget.id));
      this.gridComp.grid.removeWidget(widget);
      compRef.destroy();
    };
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

  public async synchronizeWidgetToCell(cell: CellConfig): Promise<void> {

    const widget = this.currentWidgets.get(cell.getGemId());
    const activeNode = widget?.gridstackNode;
    if (!activeNode) return;

    const size = await cell.getSize();
    const sizeW = await size.getWidth();
    const sizeL = await size.getLength();

    const pos = await cell.getPosition();
    const posX = await pos.getX();
    const posY = await pos.getY();

    const safeX = typeof activeNode.x === 'number' ? activeNode.x : 0;
    const safeY = typeof activeNode.y === 'number' ? activeNode.y : 0;
    const safeW = typeof activeNode.w === 'number' ? activeNode.w : 3;
    const safeH = typeof activeNode.h === 'number' ? activeNode.h : 3;

    //if there was any changes to the cells parameters update them
    if (sizeW != safeW || sizeL != safeH) {
      size.setWidth(safeW);
      size.setLength(safeH);
    }
    if (safeX !== posX || safeY !== posY) {
      pos.setX(safeX);
      pos.setY(safeY);
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
  //-------------------------------------------------------------------------------------
  //Function synchronizes considering 3 main cases/critical points:
  //1) first time opening the current view
  //2) creating a new cell (specifically create boolean flag changing true -> false)
  //3) finishing editing cells (specifically edit boolean flag changing true -> false)
  //--------------------------------------------------------------------------------------
  // Inputs now arrive in several steps (undefined -> null -> value), each triggering an observer call.
  // Serialize the calls so concurrent runs do not see an empty prevCells and create widgets twice.
  private syncQueue: Promise<void> = Promise.resolve();

  public reactOnViewInputChange() : Promise<void> {
    this.syncQueue = this.syncQueue
      .then(() => this.doReactOnViewInputChange())
      .catch(e => console.error('GridstackComp synchronization failed', e));
    return this.syncQueue;
  }

  private async doReactOnViewInputChange() : Promise<void> {
    // view not yet passed down or grid not yet created -> a later input change / ngAfterViewInit retriggers
    if (!this.getView() || !this.gridComp?.grid) {
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
    this.hostResizeObserver = new ResizeObserver(() => this.fitCellHeightToPage());
    this.hostResizeObserver.observe(this.host.nativeElement);
  }

  override ngOnDestroy() {
    this.hostResizeObserver?.disconnect();
    super.ngOnDestroy();
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
    if (this.prevCells.length == 0) {
      for (const cell of cells ) {
        await this.createWidget(cell);
      }
    } else {
      if (this.prevCreate && !this.create) {
       const prevCellsSet = new Set(this.prevCells.map(c => c.getGemId()));
       const extra: CellConfig = cells.find(c => !prevCellsSet.has(c.getGemId()));
       if (extra != undefined) {
         await this.createWidget(extra);
       }
      } else if (this.prevEdit && !this.isEdit()) {
        for (const cell of cells) {
          await this.synchronizeWidgetToCell(cell);
        }
      }
      const grid = this.gridComp.grid.save() as NgGridStackWidget[];
      for (const widget of grid) {
        await this.updateWidget(Number(widget.id));
      }
    }
    this.gridComp.grid.disable();
    if (this.isEdit()) {
      this.gridComp.grid.enable();
    }
    this.prevCells = [...cells];
    this.prevCreate = this.create;
    this.prevEdit = this.isEdit();
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


