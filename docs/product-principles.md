# Product principles

Shared intent with the Chrome extension; this app does not share its code.

1. **Non-custodial** — private keys stay on this device.
2. **Password-boxed vault** — encrypt key material with the user password; do not persist unlock material in plaintext.
3. **See what you sign** — show site/context, expected effects, fee, and instructions before approve when possible.
4. **No proprietary backend** — rely on user-configured public RPCs/APIs.
5. **Optional read-only AI** — may explain a transaction; never signs.
