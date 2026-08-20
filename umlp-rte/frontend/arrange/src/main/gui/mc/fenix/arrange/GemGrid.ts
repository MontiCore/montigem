/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, HostBinding, Input, TemplateRef, ViewChild} from "@angular/core";
import { config, GemGridComponent } from '../../../../../../build/gui2java2ts/mc/fenix/arrange/GemGridComponent';
import { GemGridCell } from "./GemGridCell";

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, GemGridCell],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemGrid extends GemGridComponent {
  @HostBinding('style.height')
  _height: string = '100%';

  @HostBinding('style.width')
  _width: string = '100%';

  @HostBinding('style.row-gap')
  _rowGap: string = "0";

  @HostBinding('style.column-gap')
  _colGap: string = "0";

  @HostBinding('style.grid-template-rows')
  _gridTemplateRows: string = "auto";
  _maxRows: number = 0;

  @HostBinding('style.grid-template-columns')
  _gridTemplateCols: string = "auto";
  _maxCols: number = 0;

  @HostBinding('style.justify-content')
  _hAlign: string = "stretch";

  @HostBinding('style.align-content')
  @HostBinding('style.align-items')
  _vAlign: string = "stretch";

  @ViewChild('_gemGridEmpty', { static: true })
  emptyTpl: TemplateRef<any>;

  _x: string[];
  _y: string[];

  _rows: TemplateRef<any>[][] = [];
  _cols: TemplateRef<any>[][] = [];
  _cells: TemplateRef<any>[] = [];

  public content: TemplateRef<any>[] = [];

  public get rows(): TemplateRef<any>[][] {
    return this._rows;
  }

  @Input()
  public set rows(val: TemplateRef<any>[][]) {
    if (val == null) {
      return;
    }
    this._rows = val;
    this.update();
  }

  public get cols(): TemplateRef<any>[][] {
    return this._cols;
  }

  @Input()
  public set cols(val: TemplateRef<any>[][]) {
    if (val == null || this._cells.length > 0) {
      return;
    }
    this._cols = val;
    this.update();
  }

  public get cells(): TemplateRef<any>[] {
    return this._cells;
  }

  @Input()
  public set cells(val: TemplateRef<any>[]) {
    if (val == null) {
      return;
    }
    this._cells = val;
    this.update();
  }

  public override setX(val: string[]){
    this._x = val;
    this.update();
  }

  public override setY(val: string[]){
    this._y = val;
    this.update();
  }

  public get hAlign(): string {
    return this._hAlign;
  }

  @Input()
  public set hAlign(hAlign: string) {
    if (hAlign === 'left') {
      this._hAlign = 'flex-start'
    }
    if (hAlign === 'right') {
      this._hAlign = 'flex-end'
    }
    else {
      this._hAlign = hAlign;
    }
  }

  public get vAlign(): string {
    return this._vAlign;
  }

  @Input()
  public set vAlign(vAlign: string) {
    if (vAlign === 'top') {
      this._vAlign = 'flex-start'
    }
    if (vAlign === 'bottom') {
      this._vAlign = 'flex-end'
    }
    else {
      this._vAlign = vAlign;
    }
  }

  public get rowGap(): string {
    return this._rowGap;
  }

  @Input()
  public set rowGap(val: string) {
    this._rowGap = val;
  }

  public get colGap(): string {
    return this._colGap;
  }

  @Input()
  public set colGap(val: string) {
    this._colGap = val;
  }

  update() {
    if (this._x != null) {
      this._maxCols = this._x.length;
      this._gridTemplateCols = this._x.join(" ");
    } else if (this._rows.length > 0) {
      let maxCols: number = 1;
      for (const v of this._rows) {
        if (v.length > maxCols) {
          maxCols = v.length;
        }
      }
      this._maxCols = maxCols;
    } else if (this._cols.length > 0) {
      this._maxCols = this._cols.length;
    }

    if (this._y != null) {
      this._maxRows = this._y.length;
    } else if (this._rows.length > 0) {
      this._maxRows = this._rows.length;
    } else if (this._cols.length > 0) {
      let maxRows: number = 1;
      for (const v of this._cols) {
        if (v.length > maxRows) {
          maxRows = v.length;
        }
      }
      this._maxRows = maxRows;
    }

    if (this._x != null) {
      this._gridTemplateCols = this._x.join(" ");
    } else {
      this._gridTemplateCols = `repeat(${this._maxCols}, auto)`;
    }

    if (this._y != null) {
      this._gridTemplateRows = this._y.join(" ");
    } else {
      this._gridTemplateRows = `repeat(${this._maxRows}, auto)`;
    }

    this.updateContent();
  }

  updateContent() {
    this.content = [];
    if (this._cells.length > 0) {
      for (const c of this._cells) {
        this.content.push(c);
      }
    }
    else if (this._rows.length > 0) {
      const res: TemplateRef<any>[][] = [];
      for (let row of this._rows) {
        const r: TemplateRef<any>[] = [];
        for (let col of row) {
          r.push(col);
        }
        if (row.length < this._maxCols) {
          for (let i = row.length; i < this._maxCols; ++i) {
            r.push(this.emptyTpl);
          }
        }
        res.push(r);
      }
      this.content = res.flat();
    }
    else if (this._cols.length > 0) {
      const res: TemplateRef<any>[][] = [];
      for (let col of this._cols) {
        const c: TemplateRef<any>[] = [];
        for (let row of col) {
          c.push(row);
        }
        if (col.length < this._maxRows) {
          for (let i = col.length; i < this._maxRows; ++i) {
            c.push(this.emptyTpl);
          }
        }
        res.push(c);
      }
      // transpose
      this.content = res[0].map((_, i) => res.map(row => row[i])).flat();
    }
  }

  public ngOnInit(): void { }

}
