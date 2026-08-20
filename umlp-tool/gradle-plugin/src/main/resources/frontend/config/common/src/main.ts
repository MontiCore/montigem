import { bootstrapApplication } from '@angular/platform-browser';
import { provideZoneChangeDetection, importProvidersFrom } from '@angular/core';
import {
  provideRouter,
  RouteReuseStrategy,
  BaseRouteReuseStrategy
} from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';

import { AppComponent } from 'AppComponent';
import { routes } from 'AppGemRouting';
import { config } from 'AppGemConfig';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    provideZoneChangeDetection(),
    {
      provide: RouteReuseStrategy,
      useClass: class extends BaseRouteReuseStrategy {
        override shouldReuseRoute() {
          return false;
        }
      }
    },
    ...config.providers
  ]
}).catch(err => console.error(err));
