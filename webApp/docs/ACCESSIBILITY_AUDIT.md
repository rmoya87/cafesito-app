# WebApp Accessibility Audit (2026-02-26)

**Estado:** vigente (referencia).  
**Última actualización:** 2026-02-26  

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
- Global `:focus-visible` ring is deterministic and consistent.
- Sheets use focus trap (`Tab`/`Shift+Tab`) and `Escape` close via `SheetOverlay`.
- Top-level auth guards are centralized and reused by domain hooks.

## High-priority fixes closed
- Auth popup and scanner sheet render as proper modal dialogs.
- Protected actions route through auth guard instead of silent no-op.
- Top-level guest access guard centralized in `core/guards.ts`.
- Modal focus management moved to reusable UI primitive (`SheetOverlay`).

## Remaining risks (low / iterative)
- Drag/swipe interactions still rely mainly on pointer; keyboard parity is partial.
- Contrast should be periodically re-checked when new themes/styles are added.
- Some legacy sheets still need incremental migration to shared modal primitives.

## Regression protection
- Playwright smoke tests added in `webApp/e2e/`:
  - `accessibility-smoke.spec.ts`
  - `auth-and-routing.spec.ts`
