/* (c) https://github.com/MontiCore/monticore */

import { OverlayModule } from '@angular/cdk/overlay';
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, OverlayModule],
  selector: 'gem-chart-options',
  styleUrls: ['./GemChartOptions.scss'],
  templateUrl: './GemChartOptions.html'
})
export class GemChartOptions {
  public buttonClicked: boolean = false;
}
