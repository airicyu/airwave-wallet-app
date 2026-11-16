# 0.11.0 reasoning

Home has had an Activity tab since 0.1.0 and it has shown an empty sentence since then. Assets, accounts, and Mobile Wallet Adapter signing already work. The next user-facing slice is that tab, using the same two queries and the same row text the web wallet uses now.

The enhanced query needs a Helius key and stays on Mainnet. Devnet, and Mainnet without that key but with an allowed RPC, use the signature list. That list cannot name a send, a receive, or a swap, so every such row is kind `tx` with lead `ok` or `fail`. Falling back from a failed enhanced call to the signature list would hide an outage behind a thinner list, so this version does not do that.

Rows stay in memory. Activity is a view of a public chain, not vault state. A second store would be another source of truth the architecture bans.

The web wallet sometimes prints a Helius `tokenAmount` JSON number with a floating formatter. This version does not. A chain amount is a decimal integer string and `BigInteger` only. A missing string becomes the shortened signature instead of a rounded number.

The phone frame `home-activity` fixes the row chrome. The frame's external-link aria text differs from the web string `activity.openOrb`. The visible glyph matches the frame. The content description uses the web string. That difference does not change the row. Its sample amounts are illustrations. The frame remark that asks for green incoming and red outgoing text is older than the web stylesheet, which paints the kind in the normal text color and paints only the failure word in the danger color. This version follows the stylesheet.

No new package. `HttpURLConnection` and the JSON scanning style already used for holdings are enough. Jupiter metadata is the call holdings already makes, limited to the mints on the page.

Out of this version: sending from the app, a token detail page, address book, connected sites, about, merged balances for a combined account, and any AI card.
