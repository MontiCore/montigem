---
name: dashboard-operations
description: Inspect dashboard cells, place new cells in available space, rearrange tiles, and edit chart or table configurations in the selected view.
---

# Dashboard operations

Use this skill for questions about the selected view, layout changes, and edits
to existing cells. For interpreter scripts and new charts or tables, follow the
`dashboard-chart-data` skill.
Refer to cells by name in user-facing answers. Use IDs only in tool calls; they are not visible in the dashboard.

## Inspect and rearrange the view

1. Call `getCurrentLayout` to read the view, cell IDs, positions, and sizes.
2. For a layout change, call `setCurrentLayout` with every tile in the view.
   Preserve each ID and keep positions and sizes that the user did not ask to
   change. Follow the grid bounds and overlap rules in the tool description.
3. Report the applied change only after the tool succeeds.

## Place new cells in available space

Use this workflow when creating or duplicating cells. `createCell` and
`duplicateCell` initially append the cell below the other tiles; finish its placement before
reporting the request as complete. Follow an explicit placement requested by the
user first. Otherwise:

1. Call `getCurrentLayout` before creating or removing cells. When removing cells,
   note their positions and sizes so you can reuse the cleared space. Use only
   cleared locations whose geometry is known from tool results or the conversation;
   if it is unavailable, find empty space in the current layout.
2. Choose a size that suits the new cell. Start with its created size (normally
   6 columns by 4 rows), or reuse a cleared cell's size when appropriate. Respect
   the minimum width and height of 3 and the 12-column grid.
3. Prefer space freed by the most recently removed cells in this view, provided
   the full new rectangle fits and the space is still empty. Reuse several cleared
   areas for several new cells where possible.
4. If no suitable cleared area is known or available, search empty positions in
   row order: smallest `y` first, then smallest `x`. Test the whole rectangle at
   each integer position, with `x + width <= 12`, against all other tiles. A gap
   must fit the full cell without overlap. For example, a free 6-by-4 area at
   `(0, 0)` takes priority over one at `(6, 0)` or `(0, 4)`.
5. After creation, fetch `getCurrentLayout` again. Exclude the new cell's temporary
   position when checking available space, and recheck the chosen area against
   the latest layout. For several new cells, reserve each chosen rectangle before
   placing the next one.
6. Call `setCurrentLayout` with every current tile exactly once, including the
   returned ID of the new cell. Preserve the positions and sizes of existing tiles
   unless the user requested changes. Fill a suitable gap above before extending
   the dashboard downwards; append at the bottom only when no earlier area fits.
7. Check the layout tool's result. If placement fails, refresh the layout and
   repair the proposal. If you cannot place the cell, distinguish its successful
   creation from the failed placement in your reply.

## Edit existing cells

1. Find the target ID with `getCurrentLayout`, then call `getCell` to inspect its
   script, visualization, options, and geometry.
2. Choose the operation the user requested:
   - `updateCell` renames a cell or changes its script or visualization.
   - `setCellOptions` changes chart options or table column filters and render
     scripts. Follow `dashboard-chart-data` for column expressions.
   - `duplicateCell` copies a cell into the current view.
   - `removeCells` removes the requested cells from the current view.
3. In JSON patches, include only fields that should change. A visualization
   change resets chart options; set any requested options afterwards.
4. When changing a script, including replacing sample rows with live data, follow
   `dashboard-chart-data`: read the domain model, use the diagram manager's static
   collection accessors, and validate the revised script with `checkInterpreterCode`
   before passing that exact script to `updateCell`. Keep the existing cell, column
   headers, options, and geometry unless the request requires changing them. Do not
   create a replacement cell for a data-only edit or submit a script that already
   failed validation.
5. Use `previewCell` after a script update to inspect the computed data, and when
   you need to inspect interpreter errors.
   It executes the stored script. Scripts must only read domain data and
   construct visualization payloads.
6. Check each tool result before reporting the edit as complete.
