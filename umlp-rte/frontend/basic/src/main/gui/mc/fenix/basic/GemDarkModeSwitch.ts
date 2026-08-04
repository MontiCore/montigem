/* (c) https://github.com/MontiCore/monticore */

import { Component, HostBinding, OnInit } from '@angular/core';
import { config, GemDarkModeSwitchComponent } from '../../../../../../build/gui2java2ts/mc/fenix/basic/GemDarkModeSwitchComponent';

@Component({
  standalone: true,
  imports: [...config.imports],
  selector: config.selector,
  styleUrls: config.styleUrls,
  styles: config.styles,
  templateUrl: config.templateUrl,
})
export class GemDarkModeSwitch extends GemDarkModeSwitchComponent implements OnInit {
  @HostBinding("style.width")
  _width: string = "auto";

  @HostBinding("style.height")
  _height: string = "auto";

  public isDarkMode: boolean = false;

  public ngOnInit(): void {
    // Determine initial theme by checking localStorage or the user's OS preference
    const storedTheme = localStorage.getItem('theme');
    if (storedTheme === 'dark') {
      this.isDarkMode = true;
    } else if (storedTheme === 'light') {
      this.isDarkMode = false;
    } else {
      this.isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
    this.applyTheme();
  }

  public toggleTheme(): void {
    this.isDarkMode = !this.isDarkMode;
    this.applyTheme();
    localStorage.setItem('theme', this.isDarkMode ? 'dark' : 'light');

    // Fire the generated GUI callback if one is provided
    if (this.getToggled()) {
      this.getToggled()();
    }
  }

  private applyTheme(): void {
    const mode = this.isDarkMode ? 'dark' : 'light';

    document.body.classList.toggle('dark-theme', this.isDarkMode);

    document.body.style.colorScheme = mode;

    const metaThemeColor = document.querySelector('meta[name="theme-color"]');
    if (metaThemeColor) {
      metaThemeColor.setAttribute('content', this.isDarkMode ? '#1c1c1c' : '#f3f4f6');
    }
  }

  public get tooltipText(): string {
    return this.isDarkMode
      ? 'Switch to Light Mode'
      : 'Switch to Dark Mode';
  }
}
