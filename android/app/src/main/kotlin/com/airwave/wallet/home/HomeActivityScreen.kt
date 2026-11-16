/**
 * Draws the Activity tab: heading, guide, empty and error lines, and the transaction rows.
 * It does not fetch. ActivityModel owns the list.
 */
package com.airwave.wallet.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.R
import com.airwave.wallet.holdings.TokenIconLoader
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.theme.AirwaveColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ColumnScope.HomeActivityScreen(model: WalletModel) {
    val settings = model.settings
    val endpoint = settings.rpcByCluster[settings.cluster]?.active.orEmpty()
    LaunchedEffect(model.address, settings.cluster, endpoint, settings.heliusApiUrl, model.holdings.snapshot.phase) {
        model.activity.enter(model.address, settings, model.holdings.snapshot.phase)
    }
    val activity = model.activity
    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            model.t("common.activity"),
            color = AirwaveColors.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        when (activity.phase) {
            ActivityPhase.Guide -> HoldingsGuide(model)
            ActivityPhase.Skipped -> HoldingsSkipped(model)
            ActivityPhase.Loading, ActivityPhase.Idle -> Text(model.t("common.loading"), color = AirwaveColors.TextMuted)
            ActivityPhase.Empty -> Text(model.t("activity.empty"), color = AirwaveColors.TextMuted)
            ActivityPhase.Error -> Text(model.t("error.activityLoad"), color = AirwaveColors.Danger)
            ActivityPhase.List -> ActivityList(model, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActivityList(model: WalletModel, modifier: Modifier) {
    val activity = model.activity
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxWidth()) {
        items(activity.rows, key = { it.signature }) { row ->
            ActivityLine(model, row) {
                if (!isOrbTxUrl(row.orbUrl)) return@ActivityLine
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(row.orbUrl)))
            }
        }
        item {
            DisposableEffect(Unit) {
                onDispose { activity.olderLeftScreen() }
            }
            LaunchedEffect(activity.rows.size) {
                activity.loadOlder()
            }
            when (activity.older) {
                ActivityModel.Older.Loading -> CircularProgressIndicator(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(18.dp)
                        .semantics { contentDescription = model.t("activity.loadingOlder") },
                    strokeWidth = 2.dp,
                    color = AirwaveColors.TextMuted,
                )
                ActivityModel.Older.Error -> Text(
                    model.t("error.activityOlderLoad"),
                    color = AirwaveColors.Danger,
                    modifier = Modifier.padding(top = 8.dp),
                )
                ActivityModel.Older.Off -> Box(Modifier)
            }
        }
    }
}

@Composable
private fun ActivityLine(model: WalletModel, row: ActivityRow, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActivityIcons(row.icons)
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(kindLabel(model, row.kind), color = AirwaveColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                val whenText = activityWhen(row.timestampSec)
                if (whenText.isNotEmpty()) {
                    Text(whenText, color = AirwaveColors.TextDim, fontSize = 11.sp)
                }
            }
            Text(
                detailText(model, row),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
            Icon(
                painterResource(R.drawable.ic_stroke_external),
                model.t("activity.openOrb"),
                tint = AirwaveColors.TextMuted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ActivityIcons(icons: List<ActivityIcon>) {
    if (icons.isEmpty()) return
    Row {
        icons.forEachIndexed { index, icon ->
            ActivityIconImage(icon.iconUrl, Modifier.offset(x = ((-6) * index).dp))
        }
    }
}

@Composable
private fun ActivityIconImage(url: String, modifier: Modifier) {
    var bitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(url) {
        val loaded = withContext(Dispatchers.IO) { TokenIconLoader.bitmap(url) }
        bitmap = loaded?.asImageBitmap()
    }
    val image = bitmap ?: return
    Image(
        image,
        contentDescription = null,
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape),
    )
}

private fun kindLabel(model: WalletModel, kind: ActivityKind): String = when (kind) {
    ActivityKind.Send -> model.t("activity.kind.send")
    ActivityKind.Receive -> model.t("activity.kind.receive")
    ActivityKind.Swap -> model.t("activity.kind.swap")
    ActivityKind.Tx -> model.t("activity.kind.tx")
}

private fun detailText(model: WalletModel, row: ActivityRow): androidx.compose.ui.text.AnnotatedString {
    val base = androidx.compose.ui.text.AnnotatedString.Builder()
    val muted = androidx.compose.ui.text.SpanStyle(color = AirwaveColors.TextMuted)
    if (row.lead == ActivityLead.Fail) {
        base.pushStyle(androidx.compose.ui.text.SpanStyle(color = AirwaveColors.Danger))
        base.append(model.t("activity.fail"))
        base.pop()
        if (row.detail.isNotEmpty()) {
            base.pushStyle(muted)
            base.append(" · ${row.detail}")
            base.pop()
        }
        return base.toAnnotatedString()
    }
    base.pushStyle(muted)
    if (row.lead == ActivityLead.Ok) {
        base.append(model.t("activity.success"))
        if (row.detail.isNotEmpty()) base.append(" · ${row.detail}")
    } else {
        base.append(row.detail)
    }
    base.pop()
    return base.toAnnotatedString()
}
