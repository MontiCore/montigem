/* (c) https://github.com/MontiCore/monticore */

import { ChangeDetectionStrategy, Component, inject, InjectionToken, OnChanges } from '@angular/core';
import type { GemCollabTextarea } from './GemCollabTextarea';
import { config, GemCollabTextCursorComponent } from '../../../../../../build/gui2java2ts/mc/fenix/collabtext/GemCollabTextCursorComponent';

// The text area draws the cursors: they are part of its text, not elements of their own
export const GEM_COLLAB_TEXT_CURSOR_HOST = new InjectionToken<GemCollabTextarea>('GemCollabTextarea');

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styles: [':host { display: none; }'],
  templateUrl: config.templateUrl,
})
export class GemCollabTextCursor extends GemCollabTextCursorComponent implements OnChanges {
  override _label: string = '';
  override _color: string = '#1976d2';
  override _anchor: number = 0;
  override _head: number = 0;

  private host = inject(GEM_COLLAB_TEXT_CURSOR_HOST, { optional: true });

  override ngOnInit(): void {
    super.ngOnInit();
    this.host?.addCursor(this);
  }

  ngOnChanges(): void {
    this.host?.cursorChanged();
  }

  override ngOnDestroy(): void {
    this.host?.removeCursor(this);
    super.ngOnDestroy();
  }
}
