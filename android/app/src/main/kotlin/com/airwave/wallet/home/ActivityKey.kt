/**
 * Activity's name for the stored Helius key.
 * The rule lives in settings so holdings can use the same reader.
 */
package com.airwave.wallet.home

import com.airwave.wallet.settings.heliusApiKeyFromStored

/** The stored Helius field as an API key, or null when it is not one. */
fun activityHeliusKey(raw: String): String? = heliusApiKeyFromStored(raw)
