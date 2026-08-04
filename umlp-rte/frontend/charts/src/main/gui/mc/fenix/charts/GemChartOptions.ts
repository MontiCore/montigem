/* (c) https://github.com/MontiCore/monticore */

import { OverlayModule } from '@angular/cdk/overlay';
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
  standalone: true,
  imports: [CommonModule, OverlayModule],
  selector: 'gem-chart-options',
  styleUrls: [],
  templateUrl: './GemChartOptions.html'
})
export class GemChartOptions {
  public buttonClicked: boolean = false;
}
