/**
 * Top-level Compose tree for the wallet shell.
 * It switches among setup, unlock, lead, generate, home, and MWA approval from WalletModel.route. It does not talk to walletlib or encrypt the vault.
 */
package com.airwave.wallet
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.Route

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airwave.wallet.R
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.theme.AirwaveTheme
import com.airwave.wallet.ui.AirwavePrimaryButton
import com.airwave.wallet.ui.AirwaveUnlockBackdrop
import com.airwave.wallet.ui.HomeShell
import com.airwave.wallet.ui.mwa.MwaApprovalScreen
import com.airwave.wallet.ui.airwaveUnlockFieldColors
import com.airwave.wallet.vault.BurnerKeys

@Composable
fun WalletApp(model: WalletModel) {
    AirwaveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { model.bumpUserActivity() })
                },
            color = AirwaveColors.Bg,
        ) {
            when (model.route) {
                Route.Setup -> SetupScreen(model)
                Route.Unlock -> UnlockScreen(model)
                Route.Lead -> LeadScreen(model)
                Route.Generate -> GenerateScreen(model)
                Route.Home -> HomeShell(model)
                Route.Approval -> MwaApprovalScreen(model)
            }
        }
    }
}

@Composable
internal fun BrandMarkImage() {
    Image(
        painterResource(R.drawable.brand_mark),
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun SetupScreen(model: WalletModel) {
    var password by rememberSaveable { mutableStateOf("") }
    var again by rememberSaveable { mutableStateOf("") }
    val fieldColors = airwaveUnlockFieldColors()
    Box(Modifier.fillMaxSize()) {
        AirwaveUnlockBackdrop(centerContent = false) {
            BrandMarkImage()
            Spacer(Modifier.height(18.dp))
            Text(
                "Airwave",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = AirwaveColors.Text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                model.t("setup.lead"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = AirwaveColors.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                password,
                { password = it },
                placeholder = { Text(model.t("setup.password")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                again,
                { again = it },
                placeholder = { Text(model.t("setup.reenter")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
            )
            model.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = AirwaveColors.Danger, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            AirwavePrimaryButton(model.t("setup.submit"), { model.setup(password, again) })
        }
    }
}

@Composable
internal fun UnlockScreen(model: WalletModel) {
    var password by rememberSaveable { mutableStateOf("") }
    val fieldColors = airwaveUnlockFieldColors()
    Box(Modifier.fillMaxSize()) {
        AirwaveUnlockBackdrop {
            BrandMarkImage()
            Spacer(Modifier.height(18.dp))
            Text(
                "Airwave",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = AirwaveColors.Text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                model.t("unlock.lead"),
                style = MaterialTheme.typography.bodyMedium,
                color = AirwaveColors.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                password,
                { password = it },
                placeholder = { Text(model.t("unlock.password")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
            )
            model.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = AirwaveColors.Danger, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            AirwavePrimaryButton(model.t("unlock.submit"), { model.unlock(password) })
        }
    }
}

@Composable
private fun LeadScreen(model: WalletModel) {
    Column(Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Airwave", style = MaterialTheme.typography.headlineMedium, color = AirwaveColors.Text)
        Text("No wallet account yet.", color = AirwaveColors.TextMuted)
        AirwavePrimaryButton("Create burner wallet", { model.startGenerate() })
    }
}

@Composable
private fun GenerateScreen(model: WalletModel) {
    val address = model.preview?.address.orEmpty()
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Create burner wallet", style = MaterialTheme.typography.headlineMedium, color = AirwaveColors.Text)
        Text("A new key on this device. The secret is not shown.", color = AirwaveColors.TextMuted)
        Text("Solana address", color = AirwaveColors.TextMuted)
        Row {
            Text(BurnerKeys.short(address), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = AirwaveColors.Text)
            CopyButton(address)
        }
        OutlinedButton(onClick = { model.roll() }) { Text("Roll again") }
        AirwavePrimaryButton("Generate", { model.commitGenerate() })
    }
}

@Composable
private fun CopyButton(text: String) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("address", text))
        },
        modifier = Modifier.semantics { contentDescription = "Copy address" },
    ) { Text("Copy", color = AirwaveColors.Accent) }
}
