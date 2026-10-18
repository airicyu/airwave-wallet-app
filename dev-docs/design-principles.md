# Design principles

Screen rules for the mobile wallet app (native Android for 0.1.0). An INDEX overrides this file when it says so.

The Chrome extension's popup rules (fixed 384px shell, CSS classes, hamburger order) live in that repo. Do not copy them onto a phone or desktop window.

## 1. Approval

Before the user approves, show the context, the expected effects, the fee, and the instructions as clearly as practical. Optional AI may explain a transaction. It never signs and never unlocks the vault.

## 2. Icon-only controls

A control whose only content is an icon does not also show a text label that repeats the icon. The accessible name is the action (for example Copy address). The hit target is at least 44 logical pixels. A control that has a visible label is not an icon-only control.

## 3. Long strings

A single-line address, signature, or origin ellipsizes. Copy places the full string on the clipboard. Do not shrink the type size to fit a long string. Do not stretch a button to the full width unless it is the primary action.

## 4. Widget ownership

The parent creates the child and disposes it. A widget that belongs to one approval does not survive that approval. Do not keep a signing child alive across routes by holding it above the screen that owns the request.

## 5. Copy

Errors and empty states are one short sentence. Buttons are verbs (Save, Cancel, Lock). Do not use "please" or a feature essay.

## 6. Do not

- Persist an in-flight approval so it comes back after a restart.
- Put the fee or the destination only behind a disclosure on an approve screen when the app already knows them.
- Lay the screen out as a Chrome extension popup.
