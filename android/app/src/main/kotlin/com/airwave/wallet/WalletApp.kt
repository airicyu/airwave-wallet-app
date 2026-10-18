package com.airwave.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airwave.wallet.vault.BurnerKeys

@Composable
fun WalletApp(model: WalletModel) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (model.route) {
                Route.Setup -> SetupScreen(model)
                Route.Unlock -> UnlockScreen(model)
                Route.Lead -> LeadScreen(model)
                Route.Generate -> GenerateScreen(model)
                Route.Home -> HomeScreen(model)
                Route.Approval -> ApprovalScreen(model)
            }
        }
    }
}

@Composable
private fun SetupScreen(model: WalletModel) {
    var password by rememberSaveable { mutableStateOf("") }
    var again by rememberSaveable { mutableStateOf("") }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Airwave", style = MaterialTheme.typography.headlineMedium)
        Text("Set a password to lock this wallet.")
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        OutlinedTextField(
            again,
            { again = it },
            label = { Text("Re-enter") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        model.error?.let { Text(it) }
        Button(onClick = { model.setup(password, again) }, modifier = Modifier.fillMaxWidth()) { Text("Get started") }
    }
}

@Composable
private fun UnlockScreen(model: WalletModel) {
    var password by rememberSaveable { mutableStateOf("") }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Unlock", style = MaterialTheme.typography.headlineMedium)
        Text("Wallet locked.")
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        model.error?.let { Text(it) }
        Button(onClick = { model.unlock(password) }, modifier = Modifier.fillMaxWidth()) { Text("Unlock") }
    }
}

@Composable
private fun LeadScreen(model: WalletModel) {
    Column(Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Airwave", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { model.lock() }) { Text("Lock") }
        }
        Text("No wallet account yet.")
        Button(onClick = { model.startGenerate() }, modifier = Modifier.fillMaxWidth()) { Text("Create burner wallet") }
    }
}

@Composable
private fun GenerateScreen(model: WalletModel) {
    val address = model.preview?.address.orEmpty()
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Create burner wallet", style = MaterialTheme.typography.headlineMedium)
        Text("A new key on this device. The secret is not shown.")
        Text("Solana address")
        Row {
            Text(BurnerKeys.short(address), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            CopyButton(address)
        }
        OutlinedButton(onClick = { model.roll() }) { Text("Roll again") }
        Button(onClick = { model.commitGenerate() }, modifier = Modifier.fillMaxWidth()) { Text("Generate") }
    }
}

@Composable
private fun HomeScreen(model: WalletModel) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (model.tab == Tab.Assets) "Assets" else "Activity", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = { model.lock() }) { Text("Lock") }
            }
            val address = model.address.orEmpty()
            Text("Wallet")
            Row {
                Text(BurnerKeys.short(address), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                CopyButton(address)
            }
            Text(if (model.tab == Tab.Assets) "No assets yet." else "No activity yet.")
        }
        NavigationBar {
            NavigationBarItem(model.tab == Tab.Assets, { model.tab = Tab.Assets }, icon = {}, label = { Text("Assets") })
            NavigationBarItem(model.tab == Tab.Activity, { model.tab = Tab.Activity }, icon = {}, label = { Text("Activity") })
        }
    }
}

@Composable
private fun ApprovalScreen(model: WalletModel) {
    val pending = model.pending
    Column(Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(pending?.title ?: "", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { model.lock() }) { Text("Lock") }
        }
        pending?.lines?.forEach { (label, value) ->
            Text(label)
            Text(value)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { model.reject() }, modifier = Modifier.weight(1f)) { Text("Reject") }
            Button(onClick = { model.approve() }, modifier = Modifier.weight(1f)) { Text("Approve") }
        }
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
    ) { Text("Copy") }
}
