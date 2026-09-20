/* (c) https://github.com/MontiCore/monticore */

import { ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, Scroll } from "@angular/router";
import { config, GemNavItemComponent } from '../../../../../../build/gui2java2ts/mc/fenix/navigation/GemNavItemComponent';
import { filter } from 'rxjs/operators';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  imports: [...config.imports, MatIconModule],
  selector: config.selector,
  styles: [`:host { display: flex; width: 100%; }`],
  styleUrls: config.styleUrls,
  templateUrl: config.templateUrl,
})
export class GemNavItem extends GemNavItemComponent {

  public isActive: boolean = false;

  public constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected cdRef: ChangeDetectorRef
  ) {
    super();
  }

  ngOnInit() {
    this.router.events.pipe(
      filter(event => event instanceof Scroll)
    ).subscribe(event => {
      if ((event as Scroll).routerEvent instanceof NavigationEnd) {
        const url = ((event as Scroll).routerEvent as NavigationEnd).urlAfterRedirects;
        this.updateActiveState(url);
      }
    });
  }

  private updateActiveState(url: string): void {
    if (this.target == null) {
      this.isActive = false;
      return;
    }
    this.isActive = url === this.target;
  }

  public navigateOrAct() {
    if (this.target != null) {
      this.router.navigate([this.target]);
    } else {
      this.leftClick.emit();
    }
  }
}
