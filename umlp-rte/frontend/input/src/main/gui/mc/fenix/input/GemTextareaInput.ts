/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, EventEmitter, HostBinding, Output} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTooltipModule } from '@angular/material/tooltip';
import { config, GemTextareaInputComponent } from '../../../../../../build/gui2java2ts/mc/fenix/input/GemTextareaInputComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, FormsModule, MatTooltipModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemTextareaInput extends GemTextareaInputComponent {
  override _id: string = Math.random() + '';

  override _disabled: boolean = false;

  override _placeholder: string = "";

  @HostBinding("attr.id")
  componentId: string;

  @HostBinding("style.width")
  _width: string = '100%';

  @HostBinding("style.height")
  _height: string = '100%';

  @Output()
  entryChange = new EventEmitter<string>();
  @Output()
  override keyRelease = new EventEmitter<KeyboardEvent>();
  @Output()
  override keyPress = new EventEmitter<KeyboardEvent>();

  onEntryChange(event: string) {
    this.entryChange.emit(event);
  }

  ngOnInit(): void {
    this.componentId = this.id;
  }
}
