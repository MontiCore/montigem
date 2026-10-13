/* (c) https://github.com/MontiCore/monticore */

import { ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from "@angular/router";
import { Subscription } from 'rxjs';
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

  private routerSubscription: Subscription;

  public constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected cdRef: ChangeDetectorRef
  ) {
    super();
  }

  ngOnInit() {
    // items can be created after the navigation ended (e.g. inside a @guard), so check the current url as well
    this.updateActiveState(this.router.url);
    this.routerSubscription = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(event => this.updateActiveState((event as NavigationEnd).urlAfterRedirects));
  }

  public override ngOnDestroy(): void {
    super.ngOnDestroy();
    this.routerSubscription?.unsubscribe();
  }

  private updateActiveState(url: string): void {
    if (this.target == null) {
      this.isActive = false;
    } else {
      // ignore query parameters and fragments
      this.isActive = url.split(/[?#]/)[0] === this.target;
    }
    this.cdRef.markForCheck();
  }

  public navigateOrAct() {
    if (this.target != null) {
      this.router.navigate([this.target]);
    } else {
      this.leftClick.emit();
    }
  }
}
