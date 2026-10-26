/**
 * Draws the add-account chooser, with five StrokeIcon rows and the import sub-chooser.
 * It does not write vault rows. Combined-later still writes nothing.
 */
package com.airwave.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.airwave.wallet.HomeDestination
import com.airwave.wallet.R
import com.airwave.wallet.WalletModel
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.Bip39
import com.airwave.wallet.vault.BurnerKeys

@Composable
internal fun AddAccountScreen(model: WalletModel) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SubTopBar(model, "accounts.add", showMenu = false)
        ChooserRow(
            icon = R.drawable.ic_stroke_pin,
            title = model.t("accounts.chooser.seedTitle"),
            subtitle = model.t("accounts.chooser.seedSub"),
        ) {
            model.seedGenerateDraft = Bip39.generate12(context.resources)
            model.push(HomeDestination.GenerateSeed)
        }
        ChooserRow(
            icon = R.drawable.ic_stroke_flame,
            title = model.t("accounts.chooser.burnerTitle"),
            subtitle = model.t("accounts.chooser.burnerSub"),
        ) {
            model.preview = BurnerKeys.generate()
            model.push(HomeDestination.AddBurner)
        }
        ChooserRow(
            icon = R.drawable.ic_stroke_download,
            title = model.t("accounts.chooser.importTitle"),
            subtitle = model.t("accounts.chooser.importSub"),
        ) { model.push(HomeDestination.ImportChooser) }
        ChooserRow(
            icon = R.drawable.ic_stroke_eye,
            title = model.t("accounts.chooser.watchTitle"),
            subtitle = model.t("accounts.chooser.watchSub"),
        ) { model.push(HomeDestination.WatchAddress) }
        ChooserRow(
            icon = R.drawable.ic_stroke_layers,
            title = model.t("accounts.chooser.combinedTitle"),
            subtitle = model.t("accounts.chooser.combinedSub"),
        ) { model.push(HomeDestination.CombinedLater) }
    }
}

@Composable
internal fun ImportChooserScreen(model: WalletModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SubTopBar(model, "accounts.chooser.importTitle", showMenu = false)
        ImportKindRow(model.t("accounts.importMnemonic")) { model.push(HomeDestination.ImportMnemonic) }
        ImportKindRow(model.t("accounts.importSecret")) { model.push(HomeDestination.ImportSecret) }
    }
}

@Composable
internal fun CombinedLaterScreen(model: WalletModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SubTopBar(model, "accounts.combined", showMenu = false)
        Text(model.t("accounts.combinedLater"), color = AirwaveColors.TextMuted, modifier = Modifier.padding(top = 24.dp))
    }
}

@Composable
private fun ChooserRow(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AirwaveColors.BgElevated)
            .border(1.dp, AirwaveColors.Stroke, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AirwaveColors.Fill)
                .border(1.dp, AirwaveColors.Stroke, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = AirwaveColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = AirwaveColors.Text, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = AirwaveColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ImportKindRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AirwaveColors.Fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = AirwaveColors.Text)
        Text("›", color = AirwaveColors.TextMuted, style = MaterialTheme.typography.titleLarge)
    }
}
