// Datei: umlp-rte/frontend/basic/src/main/gui/mc/fenix/basic/GemLoadingSpinner.ts
import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import { MatProgressSpinnerModule, ProgressSpinnerMode } from '@angular/material/progress-spinner';
import { config, GemLoadingSpinnerComponent } from '../../../../../../build/gui2java2ts/mc/fenix/basic/GemLoadingSpinnerComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, MatProgressSpinnerModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  styles: config.styles,
  templateUrl: config.templateUrl,
})
export class GemLoadingSpinner extends GemLoadingSpinnerComponent {

  protected _matDiameter: number = 50;
  protected _matMode: ProgressSpinnerMode = 'determinate';
  protected _matValue: number = 0;
  public _color: string = '#3f51b5';

  @Input()
  override set size(val: string) {
    this._matDiameter = parseInt(val, 10) || 50;
  }
  override get size(): string {
    return this._matDiameter + 'px';
  }

  @Input()
  override set color(val: string) {
    this._color = val;
  }
  override get color(): string {
    return this._color;
  }

  @Input()
  override set intermediate(val: boolean) {
    this._matMode = val ? 'indeterminate' : 'determinate';
  }
  override get intermediate(): boolean {
    return this._matMode === 'indeterminate';
  }

  @Input()
  override set progress(val: number) {
    this._matValue = Math.max(0, Math.min(1, val)) * 100;
  }
  override get progress(): number {
    return this._matValue / 100;
  }
}
