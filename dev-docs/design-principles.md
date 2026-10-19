# Design principles

Screen rules for the mobile wallet app (native Android for 0.1.0+). A version **INDEX** overrides this file when it says so.

The Chrome extension popup rules (fixed 384px shell, CSS classes) live in that repo. Do not copy that chrome onto a phone layout.

## Visual contract (design demos)

For versions that link an HTML file under [`design-demos/`](design-demos/README.md):

- That file is the **Android visual contract** (layout, menu chrome, tabs, Back, theme tokens on phone).
- Extension popup code supplies **IA, copy, and token values**—not popup pixel layout.
- Change the demo **before** (or in the same Track as) Compose when mobile chrome or spacing changes; see the workflow in [`design-demos/README.md`](design-demos/README.md).
- **Only content inside the phone frame** is shipped copy and controls. Designer notes beside the mockup are **not** user-visible text.

**0.2.0:** [`wallet-020.html`](design-demos/wallet-020.html) — full-width **menu top sheet** under the status bar, Material-style **bottom tabs**, blue accent menu icon on Home; **Unlock** matches extension `.unlock-screen` (layered radials + brand mark).

## Back navigation

Every **secondary surface** must give an obvious way to go back. A secondary surface is any full-screen or modal layer that is not the primary **Home** hub (Assets / Activity with bottom tabs).

| Surface | Back control | Behavior |
|---------|----------------|----------|
| **Home** | None required | Menu opens from the top bar; tabs stay on Home. |
| **Hamburger menu** (modal sheet) | **Back** top-leading | Dismisses the menu and returns to whatever was underneath (Home or a pushed subpage). |
| **Pushed subpages** (Network, Settings hub, settings children, empty menu placeholders, etc.) | **Back** top-leading | Pops one level on the in-app stack (subpage → Home, or settings child → Settings hub). |
| **MWA approval** | Reject (and Lock) | Not a settings-style back stack; use explicit approve / reject actions. |

Conventions:

- Put **Back** at the **top-leading** corner (minimum 44×44 dp touch target). A chevron / arrow icon is preferred on phone; the accessible name is the localized **Back** string (`menu.back` in 0.2.0).
- Do not rely on the system gesture alone; always show in-app Back (or equivalent) on secondary surfaces.
- Opening the hamburger from a subpage is allowed; **Back** on the menu sheet closes only the menu, not the subpage underneath.

Status-bar and navigation-bar **safe areas** apply to menu sheets and subpage top bars so controls do not overlap system time, notifications, or gesture bars.
