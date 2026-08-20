/* (c) https://github.com/MontiCore/monticore */

import {ChangeDetectionStrategy, Component, EventEmitter, Output} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { config, GemRadioInputComponent } from '../../../../../../build/gui2java2ts/mc/fenix/input/GemRadioInputComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports, FormsModule],
  selector: config.selector,
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemRadioInput extends GemRadioInputComponent {

  override _id: string = Math.random() + '';

  rightPosition: boolean = true;

  @Output()
  selectedValueChange = new EventEmitter<string>();

  onSelectedValueChange(event: string) {
    this.selectedValueChange.emit(event);
  }

  public ngOnInit(): void { }

}
