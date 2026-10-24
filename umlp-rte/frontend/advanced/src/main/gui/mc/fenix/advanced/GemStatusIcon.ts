/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component} from '@angular/core';
import { config, GemStatusIconComponent } from '../../../../../../build/gui2java2ts/mc/fenix/advanced/GemStatusIconComponent';
import { MatIconModule } from '@angular/material/icon';

/** Display of a single status, independent of the generated `Status` class. */
interface StatusDisplay {
  value: string;
  color: string;
  icon?: string;
}

/** Mapping used if no `statuses` are given: a traffic light based on the theme colors. */
const DEFAULT_STATUSES: StatusDisplay[] = [
  { value: 'OK', color: 'var(--color-success, #2e7d32)' },
  { value: 'WARNING', color: 'var(--color-warning, #f9a825)' },
  { value: 'ERROR', color: 'var(--color-error, #c62828)' },
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

  /** The entry matching the current status, or undefined if it is unknown. */
  protected _matchingStatus(): StatusDisplay | undefined {
    const status = this.getCurrentStatus();
    if (status === undefined || status === null) {
      return undefined;
    }
    const wanted = String(status).toLowerCase();
    return this._effectiveStatuses().find(s => String(s.value).toLowerCase() === wanted);
  }

  protected _effectiveStatuses(): StatusDisplay[] {
    const statuses = this.getStatuses();
    if (!statuses || statuses.length === 0) {
      return DEFAULT_STATUSES;
    }
    return statuses.map(s => ({ value: s.getValue(), color: s.getColor(), icon: s.getIcon() }));
  }

  protected _currentColor(): string {
    return this._matchingStatus()?.color || this.getDefaultColor() || 'gray';
  }

  protected _currentIcon(): string {
    return this._matchingStatus()?.icon || this.getIcon() || 'circle';
  }

  protected _iconSize(): string {
    return this.getSize() || '24px';
  }
}
