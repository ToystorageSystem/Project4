# ToyStorage personal workspace

## Direction
A calm, personal operations workspace with a clear orange brand. The user's name and the time of day orient the dashboard; the workload list leads to actual tasks. Interface controls stay familiar for repeated use.

## Tokens
| Purpose | Value |
| --- | --- |
| Brand / original logo | #f25d19 |
| Accessible primary action | #b54012 |
| Canvas | #f6f6f2 |
| Navigation / toolbar | #fdfdfb |
| Content surface | #ffffff |
| Text | #292c2b |
| Secondary text | #656b65 |
| Border | #e1e3dc |
| Priority surface | #faeadd |
| Morning time panel | #f6eed7 |
| Afternoon time panel | #ffeadc |
| Evening time panel | #e9e9f1 |

## Typography and components
One system sans family: Segoe UI, platform sans fallbacks. Dashboard heading 32px, page heading 28px, section headings 17px, controls and table content 12–14px. Tabular numerals for quantities and clocks. SVG icons share a 24px viewBox and 1.65 stroke. Avoid decorative numbering, heading kickers, fake charts and icon glyphs in new layout components.

Panels have a 12px radius and a single border, no shadow. Inputs and buttons use 7px corners. Primary actions use dark orange for readable white labels. Green is reserved for operational/status details. Focus outlines use the primary action color with a 4px offset.

## Layout and behavior
248px scrollable navigation, 72px toolbar, 36px content gutters. Dashboard: greeting and local clock, compact workload list, activity table with next-step panel, user-pinned links. Below 1150px, next-step panel moves beneath activity. Below 768px, navigation collapses behind Menu and workload becomes one column. Tables scroll within their panel. Compact view reduces table and navigation spacing.

Morning is 05:00–11:59, afternoon 12:00–17:59, evening 18:00–04:59, using the browser's local time. Update on a 30-second timer and when visibility changes. Do not simulate weather, shift schedules, notification counts or business trends.

## State and accessibility
Keep loading, unavailable and empty separate. Every page remains subject to the existing role checks. Native details is used for shortcut editing; native checkbox controls remain keyboard usable. Respect reduced motion; no staged entrance animation. Preference storage failures fall back to memory.

## Source references
The three functional areas share MainLayout, Sidebar, WorkspaceWelcome and the personal-workspace tokens. Imported Admin CSS is scoped; integration-theme.css bridges the Admin and Business components into the orange identity without replacing their operational controls or state logic.

Design guidance reviewed from https://github.com/pbakaus/impeccable, especially Operate mode and craft floor. The reference launcher could not download its engine here; runtime design checks were not claimed. Production data integration is unchanged; browser visual checks use labeled test fixtures, not a live backend.
