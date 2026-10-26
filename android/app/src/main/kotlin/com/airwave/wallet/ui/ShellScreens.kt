/**
 * Draws the home shell, hamburger menu, empty placeholders, and the network cluster picker.
 * Settings and RPC screens live in sibling files in this package.
 */
package com.airwave.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airwave.wallet.HomeDestination
import com.airwave.wallet.Tab
import com.airwave.wallet.WalletModel
import com.airwave.wallet.setCluster
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.BurnerKeys

@Composable
fun HomeShell(model: WalletModel) {
    val top = model.homeStack.lastOrNull()
    if (top == null) {
        HomeTabs(model)
    } else {
        when (top) {
            HomeDestination.Accounts -> AccountsScreen(model)
            HomeDestination.AddAccount -> AddAccountScreen(model)
            HomeDestination.ImportChooser -> ImportChooserScreen(model)
            HomeDestination.AddBurner -> AddBurnerScreen(model)
            HomeDestination.GenerateSeed -> GenerateSeedScreen(model)
            HomeDestination.ImportMnemonic -> ImportMnemonicScreen(model)
            HomeDestination.ImportSecret -> ImportSecretScreen(model)
            HomeDestination.WatchAddress -> WatchAddressScreen(model)
            HomeDestination.CombinedLater -> CombinedLaterScreen(model)
            HomeDestination.EmptyAddressBook -> EmptyScreen(model, "menu.addressBook", "empty.addressBook")
            HomeDestination.EmptyConnected -> EmptyScreen(model, "menu.connectedSites", "empty.connectedSites")
            HomeDestination.EmptyAbout -> EmptyScreen(model, "menu.about", "empty.about")
            HomeDestination.Network -> NetworkScreen(model)
            HomeDestination.SettingsHub -> SettingsHubScreen(model)
            HomeDestination.Language -> LanguageScreen(model)
            HomeDestination.Rpc -> RpcScreen(model)
            HomeDestination.ApiKeys -> ApiKeysScreen(model)
            HomeDestination.AiSettings -> AiSettingsScreen(model)
            HomeDestination.CuPrice -> CuPriceScreen(model)
            HomeDestination.WalletLockSettings -> WalletLockSettingsScreen(model)
        }
    }
    if (model.menuOpen) {
        HamburgerMenu(model)
    }
}

@Composable
private fun HomeTabs(model: WalletModel) {
    Column(Modifier.fillMaxSize().background(AirwaveColors.Bg).statusBarsPadding()) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (model.tab == Tab.Assets) model.t("home.assets") else model.t("home.activity"),
                    style = MaterialTheme.typography.headlineLarge,
                    color = AirwaveColors.Text,
                )
                IconButton(
                    onClick = { model.menuOpen = true },
                    modifier = Modifier.semantics { contentDescription = model.t("menu.menu") },
                ) {
                    Icon(
                        Icons.Default.Menu,
                        model.t("menu.menu"),
                        tint = AirwaveColors.Accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { model.push(HomeDestination.Accounts) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = Color(0xFFD08B5B),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    BurnerKeys.short(model.address.orEmpty()),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = AirwaveColors.Text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                )
            }
            model.mwaError?.let { msg ->
                Text(msg, color = AirwaveColors.Danger, style = MaterialTheme.typography.bodySmall)
            }
            if (model.settings.cluster == Cluster.Devnet) {
                Text(model.t("network.devnet"), color = AirwaveColors.Warn, style = MaterialTheme.typography.bodyMedium)
            }
            if (model.tab == Tab.Assets) {
                HomeAssetsScreen(model)
            } else {
                Text(
                    model.t("home.noActivity"),
                    color = AirwaveColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        HomeBottomTabs(
            selected = model.tab,
            onAssets = { model.tab = Tab.Assets },
            onActivity = { model.tab = Tab.Activity },
            assetsLabel = model.t("home.assets"),
            activityLabel = model.t("home.activity"),
        )
    }
}

@Composable
private fun HamburgerMenu(model: WalletModel) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(AirwaveColors.Bg.copy(alpha = 0.72f))
                .clickable { model.menuOpen = false },
        )
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(AirwaveColors.BgElevated)
                .padding(bottom = 8.dp),
        ) {
            MenuSheetTopBar(model)
            HorizontalDivider(color = AirwaveColors.StrokeSoft)
            menuRow(model, "menu.accounts", Icons.Default.People) { model.push(HomeDestination.Accounts) }
            menuRow(model, "menu.addressBook", Icons.Default.MenuBook) { model.push(HomeDestination.EmptyAddressBook) }
            menuRow(model, "menu.network", Icons.Default.Public) { model.push(HomeDestination.Network) }
            menuRow(model, "menu.settings", Icons.Default.Settings) { model.push(HomeDestination.SettingsHub) }
            menuRow(model, "menu.connectedSites", Icons.Default.Link) { model.push(HomeDestination.EmptyConnected) }
            menuRow(model, "menu.about", Icons.Default.Info) { model.push(HomeDestination.EmptyAbout) }
            HorizontalDivider(color = AirwaveColors.StrokeSoft)
            menuRow(model, "menu.lock", Icons.Default.Lock) { model.lock() }
        }
    }
}

@Composable
private fun MenuSheetTopBar(model: WalletModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { model.menuOpen = false },
            modifier = Modifier.semantics { contentDescription = model.t("menu.back") },
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AirwaveColors.Accent)
        }
        Text(
            model.t("menu.menu"),
            style = MaterialTheme.typography.titleMedium,
            color = AirwaveColors.Text,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(Modifier.size(48.dp))
    }
}

@Composable
private fun menuRow(
    model: WalletModel,
    key: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AirwaveColors.TextMuted, modifier = Modifier.size(22.dp))
        Text(model.t(key), color = AirwaveColors.Text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun SubTopBar(model: WalletModel, titleKey: String, showMenu: Boolean = true) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { model.pop() },
            modifier = Modifier.semantics { contentDescription = model.t("menu.back") },
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AirwaveColors.Accent)
        }
        Text(
            model.t(titleKey),
            style = MaterialTheme.typography.titleMedium,
            color = AirwaveColors.Text,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (showMenu) {
            IconButton(onClick = { model.menuOpen = true }) {
                Icon(Icons.Default.Menu, model.t("menu.menu"), tint = AirwaveColors.Accent)
            }
        } else {
            Box(Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun EmptyScreen(model: WalletModel, titleKey: String, bodyKey: String) {
    Column(Modifier.fillMaxSize().background(AirwaveColors.Bg).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SubTopBar(model, titleKey)
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(model.t(bodyKey), color = AirwaveColors.TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun NetworkScreen(model: WalletModel) {
    Column(Modifier.fillMaxSize().background(AirwaveColors.Bg).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SubTopBar(model, "network.title")
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Cluster.entries.forEach { cluster ->
                val selected = model.settings.cluster == cluster
                PickRow(
                    selected = selected,
                    title = when (cluster) {
                        Cluster.Devnet -> model.t("network.devnet")
                        Cluster.Mainnet -> model.t("network.mainnet")
                    },
                    onClick = { model.setCluster(cluster) },
                )
            }
        }
    }
}
