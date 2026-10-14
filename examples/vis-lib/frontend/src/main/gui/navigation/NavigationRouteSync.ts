/* (c) https://github.com/MontiCore/monticore */

import { DestroyRef, Directive, OnInit, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Navigation } from 'navigation/Navigation';

/** First url segment -> category of the navigation bar. Unknown segments keep the current category. */
const CATEGORY_BY_SEGMENT: { [segment: string]: string } = {
  example_basic: 'Basic',
  example_input: 'Input',
  example_arrange: 'Arrange',
  example_table: 'Arrange',
  example_charts: 'Charts',
  example_network: 'Charts',
  manual: 'Test',
  common: 'Other',
  person: 'Other',
  example_monaco: 'Other',
  fischertechnik: 'Other',
  example_cad: 'Other',
  example_functiondisplay: 'Other',
  example_advanced: 'Other',
};

const HOME_URL = '/common/LandingPage';

/**
 * Sets the active category of the navigation bar to the category of the current page,
 * so the opened page is highlighted in both rows of the bar.
 */
@Directive({
  standalone: true,
  selector: 'navigation',
})
export class NavigationRouteSync implements OnInit {
  private navigation = inject(Navigation, { self: true });
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.sync(this.router.url);
    const subscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(event => this.sync((event as NavigationEnd).urlAfterRedirects));
    this.destroyRef.onDestroy(() => subscription.unsubscribe());
  }

  private sync(url: string): void {
    const path = url.split(/[?#]/)[0];
    const category = path === HOME_URL ? 'Home' : CATEGORY_BY_SEGMENT[path.split('/')[1]];
    if (category != null && category !== this.navigation.getActiveCategory()) {
      this.navigation.setActiveCategory(category);
    }
  }
}
