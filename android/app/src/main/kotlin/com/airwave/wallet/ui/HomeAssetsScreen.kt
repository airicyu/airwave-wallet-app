/**
 * Draws the assets tab body, including the guide, empty skip, failure state, and the token list.
 * It does not own the snapshot and does not parse RPC JSON.
 */
package com.airwave.wallet.ui
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.HomeDestination

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.R
import com.airwave.wallet.holdings.BaseUnits
import com.airwave.wallet.holdings.HoldingPhase
import com.airwave.wallet.holdings.HoldingRow
import com.airwave.wallet.holdings.TokenIconLoader
import com.airwave.wallet.theme.AirwaveColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun HomeAssetsScreen(model: WalletModel) {
    val snapshot = model.holdings.snapshot
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (snapshot.phase) {
            HoldingPhase.Guide -> Guide(model)
            HoldingPhase.Skipped -> Skipped(model)
            HoldingPhase.Loading -> Text(model.t("assets.loading"), color = AirwaveColors.TextMuted)
            HoldingPhase.Failed -> {
                Text(model.t("assets.failed"), color = AirwaveColors.Danger)
                TextButton(onClick = { model.refreshHoldings() }) {
                    Text(model.t("assets.refresh"))
                }
            }
            HoldingPhase.Ready, HoldingPhase.Idle -> {
                TextButton(onClick = { model.refreshHoldings() }) {
                    Text(model.t("assets.refresh"))
                }
                snapshot.rows.forEach { TokenRow(it) }
            }
        }
    }
}

@Composable
private fun Guide(model: WalletModel) {
    val context = LocalContext.current
    Text(model.t("rpcGuide.title"), color = AirwaveColors.Text, style = MaterialTheme.typography.titleLarge)
    Text(model.t("rpcGuide.body"), color = AirwaveColors.TextMuted)
    TextButton(onClick = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.helius.dev/")))
    }) {
        Text(model.t("rpcGuide.link"))
    }
    Row {
        TextButton(onClick = { model.skipHoldingsGuide() }) { Text(model.t("rpcGuide.skip")) }
        TextButton(onClick = { model.push(HomeDestination.Rpc) }) { Text(model.t("rpcGuide.set")) }
    }
}

@Composable
private fun Skipped(model: WalletModel) {
    Text(model.t("home.noAssets"), color = AirwaveColors.TextMuted)
    TextButton(onClick = { model.push(HomeDestination.Rpc) }) { Text(model.t("rpcGuide.set")) }
}

@Composable
private fun TokenRow(row: HoldingRow) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (row.verified) HoldingShield(row.organicScore)
        TokenIcon(row.iconUrl)
        Column(Modifier.weight(1f)) {
            Text(row.name, color = AirwaveColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                BaseUnits.formatBaseUnits(row.raw, row.decimals) + " " + row.symbol,
                color = AirwaveColors.TextMuted,
                maxLines = 1,
            )
        }
        Text(row.usdLabel, color = AirwaveColors.Text)
    }
}

@Composable
internal fun HoldingShield(score: String?) {
    val fill = lerp(AirwaveColors.BgElevated, AirwaveColors.Ok, 0.18f)
    val badgeFill = lerp(AirwaveColors.Bg, AirwaveColors.Ok, 0.16f)
    val badgeStroke = lerp(AirwaveColors.Stroke, AirwaveColors.Ok, 0.35f)
    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(7.dp))
                .background(fill),
        )
        Icon(
            painter = painterResource(R.drawable.ic_token_trust_verified),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Unspecified,
        )
        if (!score.isNullOrBlank()) {
            Text(
                score,
                color = AirwaveColors.Ok,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeFill)
                    .border(1.dp, badgeStroke, RoundedCornerShape(4.dp))
                    .padding(horizontal = 3.dp),
            )
        }
    }
}

@Composable
private fun TokenIcon(url: String?) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = null
        if (url.isNullOrBlank() || !url.startsWith("https:")) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) { TokenIconLoader.bitmap(url)?.asImageBitmap() }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(AirwaveColors.Fill),
            contentScale = ContentScale.Crop,
        )
    }
}
