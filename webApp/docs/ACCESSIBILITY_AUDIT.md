# WebApp Accessibility Audit (2026-02-26)

## Scope
- Timeline
- Search
- Coffee detail
- Diary
- Profile
- Auth prompt / login gate

## What was validated
- Dialogs expose `role="dialog"` and `aria-modal`.
- Major icon buttons keep `aria-label`.
- Keyboard focus can reach primary auth action.
- Guest auth prompt is reachable from protected interactions.

## High-priority fixes already in place
- Auth popup and scanner sheet are rendered as proper modal dialogs.
- Protected actions route through auth guard instead of silent no-op.
- Top-level guest access guard centralized in `core/guards.ts`.

## Remaining a11y risks (next pass)
- Add deterministic `focus-visible` styles for all interactive controls.
- Add focus trap for all sheets/modals.
- Ensure ESC closes every modal consistently.
- Add full keyboard navigation parity for swipe/drag components.
- Validate contrast ratio on all dark/light combinations with tooling.

## Regression protection
- Playwright smoke tests added in `webApp/e2e/`:
  - `accessibility-smoke.spec.ts`
  - `auth-and-routing.spec.ts`
