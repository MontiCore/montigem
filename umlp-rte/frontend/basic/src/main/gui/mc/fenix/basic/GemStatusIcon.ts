/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component} from '@angular/core';
import { config, GemStatusIconComponent } from '../../../../../../build/gui2java2ts/mc/fenix/basic/GemStatusIconComponent';
import { MatIconModule } from '@angular/material/icon';

/** Mapping used if no `states` are given: a traffic light based on the theme colors. */
const DEFAULT_STATES = ['OK', 'WARNING', 'ERROR'];
const DEFAULT_COLORS = [
  'var(--color-success, #2e7d32)',
  'var(--color-warning, #f9a825)',
  'var(--color-error, #c62828)',
];

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, MatIconModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  styles: config.styles,
  templateUrl: config.templateUrl,
})
export class GemStatusIcon extends GemStatusIconComponent {

  /** Position of the current status within the states, or -1 if it is unknown. */
  protected _statusIndex(): number {
    const status = this.getStatus();
    if (status === undefined || status === null) {
      return -1;
    }
    const wanted = String(status).toLowerCase();
    return this._effectiveStates().findIndex(s => String(s).toLowerCase() === wanted);
  }

  protected _effectiveStates(): string[] {
    const states = this.getStates();
    return states && states.length > 0 ? states : DEFAULT_STATES;
  }

  protected _effectiveColors(): string[] {
    const states = this.getStates();
    return states && states.length > 0 ? (this.getColors() ?? []) : DEFAULT_COLORS;
  }

  protected _currentColor(): string {
    const i = this._statusIndex();
    return (i >= 0 && this._effectiveColors()[i]) || this.getDefaultColor() || 'gray';
  }

  protected _currentIcon(): string {
    const i = this._statusIndex();
    const icons = this.getIcons() ?? [];
    return (i >= 0 && icons[i]) || this.getIcon() || 'circle';
  }

  protected _iconSize(): string {
    return this.getSize() || '24px';
  }
}
