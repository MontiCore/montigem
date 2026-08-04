# MontiGem Theming

This article documents the revision of the design system used across MontiGem applications. It describes what changed, how the new theming system is structured, how it is consumed by a MontiGem app, how to create a new theme, and how to make a GemComponent compatible with the system.

## 1. What changed?

The design system for MontiGem apps was revised as a whole. As part of this revision, most SCSS files belonging to the Gem components were moved into a single, centralized folder:

```
umlp-rte/frontend/theme/src/main/gui/mc/fenix/theme/src/components
```

Previously, these files were scattered; they are now collected in one place, which makes the styling of the Gem component library easier to navigate and maintain.

In addition, all of these SCSS files were renamed to follow the same PascalCase naming convention already used by their corresponding GUI, HTML, and TypeScript files. A component's stylesheet now carries the exact same name as its component, for example `GemAudio.scss` or `GemButton.scss`, instead of a name that diverges from the rest of the component's files.

The revision also introduces a new component: the `DarkModeSwitchComponent` (`GemDarkModeSwitch`). As its name suggests, it is a switch that toggles between dark mode and light mode. It relies on the CSS `light-dark()` color function together with the `color-scheme` property, meaning the switch does not need to duplicate every color value itself — it flips the mode that the underlying CSS variables resolve against.

The following chapters explain how the resulting system works and how it can be used.

## 2. Using different themes

The theming system is built around two central files and a folder of interchangeable color schemes.

### The component mixin: `_index.scss`

The file

```
umlp-rte/frontend/theme/src/main/gui/mc/fenix/theme/src/_index.scss
```

first imports every Gem component's SCSS file from the `src/components` folder (as well as a few other shared partials, such as `ActionButtons`, `Dialog`, and `Table`). It then defines a mixin, `apply-all-gem-components`, which combines all of these component styles into a single mixin call. Including `apply-all-gem-components()` therefore includes the theme-aware styling for every Gem component at once, so an app author does not have to import and include each component's theme mixin individually.

### The app-level mixin: `_theme.scss`

The `apply-all-gem-components` mixin is, in turn, consumed by another mixin, `montigem-theme`, defined in

```
umlp-rte/frontend/theme/src/main/gui/mc/fenix/theme/_theme.scss
```

`_theme.scss` is also where the color scheme ("theme design") for an app is selected. Near the top of the file there is an import statement that currently reads:

```scss
@import "./base/theme-designs/rwth-blue";
```

This import determines which color scheme is active. If a developer wants to switch an app to a different, already existing color scheme, they can look inside

```
umlp-rte/frontend/theme/src/main/gui/mc/fenix/theme/base/theme-designs
```

which contains every available theme design (for example `_regensburg-red.scss` or `_pinky-unicorn.scss`) alongside the structural `_tokens.scss` file (see chapter 4). Changing the import in `_theme.scss` to point at a different design — e.g. `@import "./base/theme-designs/regensburg-red";` — switches the whole app to that color scheme.

### Activating the system in a MontiGem app

Adopting this system in a MontiGem app is intentionally simple. It is already in use in the vis-lib example app. To activate it, an app's global stylesheet only needs two lines. In `examples/vis-lib/frontend/config/src/styles.scss`:

```scss
@import "../../../../../../umlp-rte/frontend/theme/src/main/gui/mc/fenix/theme/theme";

@include montigem-theme();
```

The first line imports the theme functionality (the `_theme.scss` file, together with everything it in turn imports — the selected theme design, the tokens, and all Gem component styles). The second line, `@include montigem-theme();`, actually applies it, styling the app's own chrome (auth screens, navigation, header, sidebar) as well as every Gem component via the nested `apply-all-gem-components` call.

## 3. Creating a new theme

At the time of writing there are three theme designs — `rwth-blue`, `regensburg-red`, and `pinky-unicorn` — but developers are free to add further designs of their own. To add a new theme design, the following steps should be followed:

1. **Add a new SCSS file** in
   ```
   umlp-rte/frontend/theme/src/main/gui/mc/fenix/theme/base/theme-designs
   ```
   using the same PascalCase-derived, kebab-case file naming convention as the existing designs (e.g. `_my-new-theme.scss`).

2. **Copy the content of an existing theme design** (e.g. `_rwth-blue.scss`) as a starting point, so that all of the CSS custom properties the system expects are present. Change the color values as desired, but do **not** rename any of the CSS variables — renaming them will break the components and app chrome that reference them by name.

3. **Only put color-wise variables into the new theme file.** Structural variables (spacing, radii, padding, font family, and similar non-color values) live in `_tokens.scss` and are shared across all theme designs; they should not be duplicated or overridden inside a theme design file.

4. **Update the import in `_theme.scss`** to point at the new theme design file, e.g.:
   ```scss
   @import "./base/theme-designs/my-new-theme";
   ```
   Since the whole system is built on CSS custom properties that are resolved at runtime, the new theme becomes visible as soon as Angular has rebuilt — no further wiring is required.

Because of the newly added Dark Mode Switch component, every theme — existing and future — is expected to support both dark mode and light mode. The theme design files already use the `light-dark()` function for their color variables (see `_rwth-blue.scss`, where variables such as `--color-bg` or `--color-primary` each declare both a light and a dark value), and any new theme design should follow that same pattern.

## 4. Make components compatible with the theme system

To make an existing or newly created GemComponent compatible with the theme system, the following distinction should be kept in mind:

- **`_tokens.scss`** holds structural variables — sizes, paddings, radii, and similar non-color values that stay constant across all theme designs.
- **The theme design files** (`_rwth-blue.scss`, `_regensburg-red.scss`, `_pinky-unicorn.scss`, ...) hold the color-wise variables, each defined via `light-dark()` so it resolves correctly in both light and dark mode.

To keep the system consistent, a component's SCSS file should rely on existing variables from these two sources wherever possible. If a new variable is genuinely needed, it should be added to the correct place: color values go into the theme design files, and structural values go into `_tokens.scss`. The component's own SCSS file then simply consumes these variables via `var(--…)`, as demonstrated by `GemButton.scss` (using `--color-primary`, `--text-on-primary`, `--btn-padding-y`/`--btn-padding-x`) and `GemImage.scss`.
