import { ChangeDetectionStrategy, Component, HostBinding } from '@angular/core';
import { config, GemToggleComponent } from '../../../../../../build/gui2java2ts/mc/fenix/advanced/GemToggleComponent';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  styles: config.styles,
  templateUrl: config.templateUrl,
})
export class GemToggle extends GemToggleComponent {
  @HostBinding('style.--gem-toggle-on-color')
  _onColor: string = '#2563eb';

  @HostBinding('style.--gem-toggle-off-color')
  _offColor: string = '#9ca3af';

  @HostBinding('style.--gem-toggle-width')
  _width: string = '44px';

  @HostBinding('style.--gem-toggle-height')
  _height: string = '24px';

  public toggle(): void {
    if (this.isDisabled()) {
      return;
    }

    this.switchedOn = !this.isSwitchedOn();
    this.switchedOnChange.emit(this.isSwitchedOn());
    this.toggled.emit(this.isSwitchedOn());
  }
}
