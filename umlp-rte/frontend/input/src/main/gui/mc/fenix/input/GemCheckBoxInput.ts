/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, EventEmitter, HostBinding, Input, Output} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { config, GemCheckBoxInputComponent } from '../../../../../../build/gui2java2ts/mc/fenix/input/GemCheckBoxInputComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, FormsModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemCheckBoxInput extends GemCheckBoxInputComponent {
  @HostBinding("attr.id") componentId: string;
  rightPosition: boolean = true;

  @Output()
  checkedChange = new EventEmitter<boolean>();

  onCheckedChange(event: boolean) {
    this.checkedChange.emit(event);
  }

  override get labelPosition(): string {
    return this._labelPosition;
  }

  @Input()
  override set labelPosition(labelPosition: string) {
    if (labelPosition === 'left') {
      this.rightPosition = false;
    } else {
      this.rightPosition = true;
    }
  }

  ngOnInit(): void { }
}
