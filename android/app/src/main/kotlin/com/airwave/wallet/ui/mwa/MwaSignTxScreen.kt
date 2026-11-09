/**
 * Draws the sign-transaction approval: changes, fee editor, failure, and network mismatch.
 * It does not submit the transaction or start the MWA session.
 */
package com.airwave.wallet.ui.mwa
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.approve
import com.airwave.wallet.session.reject
import com.airwave.wallet.session.WalletOperationRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.R
import com.airwave.wallet.holdings.BaseUnits
import com.airwave.wallet.mwa.MwaSession
import com.airwave.wallet.mwa.tx.SignTxDelta
import com.airwave.wallet.mwa.tx.SignTxIx
import com.airwave.wallet.mwa.tx.SignTxMath
import com.airwave.wallet.mwa.tx.SignTxUi
import com.airwave.wallet.mwa.tx.SignTxWire
import com.airwave.wallet.ui.HoldingShield
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.BurnerKeys
import java.math.BigInteger

@Composable
internal fun SignTxBody(model: WalletModel, pending: WalletOperationRequest, session: MwaSession) {
    val ui = session.signUi
    var openFee by remember { mutableStateOf(false) }
    var openIx by remember { mutableStateOf(false) }
    var openFail by remember { mutableStateOf(true) }
    var limitDraft by remember(ui?.cuLimit) { mutableStateOf(ui?.cuLimit.orEmpty()) }
    var secondDraft by remember(ui?.secondField) { mutableStateOf(ui?.secondField.orEmpty()) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val mismatch = ui?.mismatch
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AccountChip(model, pending)
            }
            if (mismatch != null && ui?.devnetStrip == true) {
                Text(
                    "Devnet",
                    color = AirwaveColors.Warn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF3A2E14)).padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SiteLine(model, pending)
            Text(
                model.t("mwa.signTx.title"),
                color = AirwaveColors.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (mismatch != null) {
                val parts = mismatch.split(":")
                Text(model.t("mwa.signTx.mismatchTitle"), color = AirwaveColors.Text, fontWeight = FontWeight.SemiBold)
                Text(
                    model.t("mwa.signTx.mismatchBody").replace("{wallet}", parts.getOrElse(0) { "" }).replace("{other}", parts.getOrElse(1) { "" }),
                    color = AirwaveColors.TextMuted,
                )
            } else {
                Column(Modifier.elevatedCard(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(model.t("mwa.signTx.changes"), color = AirwaveColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ToolButton(R.drawable.ic_stroke_refresh, model.t("assets.refresh"), explorer = false) { session.refreshSignTx(null, null, null) }
                        ToolButton(R.drawable.ic_stroke_external, model.t("mwa.signTx.inspector"), inspectorUrl(session, model) != null, explorer = true) {
                            val url = inspectorUrl(session, model) ?: return@ToolButton
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                        }
                    }
                    when {
                        ui == null || ui.loading -> Text(model.t("mwa.signTx.estimating"), color = AirwaveColors.TextMuted)
                        ui.failureTitle -> {
                            Text(model.t("mwa.signTx.failTitle"), color = AirwaveColors.Danger, fontWeight = FontWeight.SemiBold)
                            Text(ui.failureReason.orEmpty(), color = AirwaveColors.Text)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    model.t("mwa.signTx.failDetails"),
                                    color = AirwaveColors.TextMuted,
                                    modifier = Modifier.weight(1f).clickable { openFail = !openFail },
                                )
                                IconButton(onClick = { clipboard.setText(AnnotatedString(ui.failureReason.orEmpty())) }) {
                                    Icon(painterResource(R.drawable.ic_stroke_copy), model.t("accounts.copy"), tint = AirwaveColors.Text)
                                }
                            }
                            if (openFail) {
                                Text(ui.failureReason.orEmpty(), color = AirwaveColors.TextMuted, modifier = Modifier.heightIn(max = 320.dp))
                            }
                            Text(model.t("mwa.signTx.failHint"), color = AirwaveColors.TextMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                        ui.deltas.isEmpty() -> Unit
                        else -> ui.deltas.forEach { DeltaRow(it) }
                    }
                }
                if (ui != null) {
                    Column(Modifier.elevatedCard(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { openFee = !openFee },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(AirwaveColors.Fill),
                                contentAlignment = Alignment.Center,
                            ) { DisclosureChevron(openFee) }
                            Text(model.t("mwa.signTx.fee"), color = AirwaveColors.TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            val total = when {
                                ui.cuWriteError -> model.t("mwa.signTx.cuWriteError")
                                ui.feeTotal != null -> formatLamports(ui.feeTotal) + " SOL"
                                else -> model.t("mwa.signTx.estimating")
                            }
                            Text(total, color = AirwaveColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                        if (openFee) FeeEditor(model, session, ui, limitDraft, secondDraft, { limitDraft = it }, { secondDraft = it })
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clickable { openIx = !openIx }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DisclosureChevron(openIx)
                    Text(model.t("mwa.signTx.instructions"), color = AirwaveColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (openIx) InstructionCard(model, session)
            }
            }
        }
        BottomPair(
            ghost = model.t("mwa.reject"),
            primary = model.t("mwa.approve"),
            enabled = !model.approving && !model.approvalSettled,
            primaryEnabled = mismatch == null && !model.approving && !model.approvalSettled,
            onGhost = { model.reject() },
            onPrimary = { model.approve() },
        )
    }
}

@Composable
private fun DeltaRow(row: SignTxDelta) {
    val shown = (if (row.positive) "+" else "−") + BaseUnits.formatBaseUnits(BigInteger(row.raw), row.decimals) + " " + row.symbol
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            if (row.verified) HoldingShield(row.organicScore)
        }
        if (row.mint.isEmpty()) {
            Box(Modifier.size(28.dp).background(AirwaveColors.Fill, CircleShape), contentAlignment = Alignment.Center) {
                Text("SOL", color = AirwaveColors.Text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Box(Modifier.size(28.dp))
        }
        Text(row.name, color = AirwaveColors.Text, modifier = Modifier.weight(1f))
        Text(shown, color = if (row.positive) AirwaveColors.Ok else AirwaveColors.Danger, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatLamports(value: BigInteger): String = BaseUnits.formatBaseUnits(value, 9)

@Composable
private fun DisclosureChevron(open: Boolean) {
    Icon(
        painterResource(R.drawable.ic_stroke_chevron),
        contentDescription = null,
        tint = AirwaveColors.TextMuted,
        modifier = Modifier.size(16.dp).rotate(if (open) 90f else 0f),
    )
}

private val CardShape = RoundedCornerShape(12.dp)

private fun Modifier.elevatedCard(): Modifier =
    fillMaxWidth().background(AirwaveColors.BgElevated, CardShape).border(1.dp, AirwaveColors.StrokeSoft, CardShape).padding(12.dp)

@Composable
private fun ToolButton(icon: Int, label: String, enabled: Boolean = true, explorer: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val border = if (explorer) AirwaveColors.Accent else AirwaveColors.Stroke
    val fill = if (explorer) Color(0xFF1A2838) else AirwaveColors.Fill
    val tint = when {
        !enabled -> AirwaveColors.TextDim
        explorer -> Color(0xFF7EC4F8)
        else -> AirwaveColors.Text
    }
    Box(
        Modifier
            .size(34.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FeeLine(label: String, amount: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AirwaveColors.TextMuted, fontSize = 12.sp)
        Text(amount, color = AirwaveColors.TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun RowScope.CuField(label: String, value: String, enabled: Boolean, onValue: (String) -> Unit) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = AirwaveColors.TextDim, fontSize = 11.sp)
        BasicTextField(
            value,
            onValue,
            enabled = enabled,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = AirwaveColors.Text, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth().background(AirwaveColors.Fill, RoundedCornerShape(8.dp)).border(1.dp, AirwaveColors.StrokeSoft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FeeEditor(
    model: WalletModel,
    session: MwaSession,
    ui: SignTxUi,
    limitDraft: String,
    secondDraft: String,
    onLimit: (String) -> Unit,
    onSecond: (String) -> Unit,
) {
    FeeLine(model.t("mwa.signTx.sigFee"), ui.signatureFee?.let { formatLamports(it) + " SOL" } ?: model.t("mwa.signTx.estimating"))
    FeeLine(model.t("mwa.signTx.priorityFee"), ui.priorityFee?.let { formatLamports(it) + " SOL" } ?: model.t("mwa.signTx.estimating"))
    val limit = limitDraft.toLongOrNull()
    val second = secondDraft.toBigIntegerOrNull()
    val dirty = limitDraft != ui.cuLimit || secondDraft != ui.secondField
    val ok = limit != null && SignTxMath.limitInRange(limit) && second != null &&
        if (ui.v1) SignTxMath.priorityInRange(second) else SignTxMath.priceInRange(second.toLong())
    val canApply = dirty && ok && ui.editable && !ui.loading
    val invalid = dirty && !ok && ui.editable
    val shape = RoundedCornerShape(8.dp)
    val border = when {
        canApply -> AirwaveColors.AccentDim
        invalid -> AirwaveColors.Stroke
        else -> Color.Transparent
    }
    val fill = if (canApply) Color(0xFF1A2838) else Color.Transparent
    val tint = when {
        canApply -> AirwaveColors.Accent
        invalid -> AirwaveColors.TextMuted
        else -> AirwaveColors.TextDim
    }
    val alpha = when {
        ui.loading -> 0.75f
        canApply -> 1f
        invalid -> 0.45f
        else -> 0.35f
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CuField(model.t("mwa.signTx.cuLimit"), limitDraft, ui.editable && !ui.loading, onLimit)
        CuField(if (ui.v1) model.t("mwa.signTx.priorityFee") else model.t("mwa.signTx.cuPrice"), secondDraft, ui.editable && !ui.loading, onSecond)
        Box(
            Modifier
                .size(32.dp)
                .alpha(alpha)
                .clip(shape)
                .background(fill)
                .border(1.dp, border, shape)
                .clickable(enabled = canApply) {
                    session.refreshSignTx(limit, if (ui.v1) null else second?.toLong(), if (ui.v1) second else null)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (ui.loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AirwaveColors.TextMuted, strokeWidth = 2.dp)
            } else {
                Icon(painterResource(R.drawable.ic_stroke_check), model.t("mwa.signTx.apply"), tint = tint, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun InstructionCard(model: WalletModel, session: MwaSession) {
    val tx = SignTxWire.compiled(session.signBytes.working)
    val rows = SignTxIx.show(tx)
    var openRaw by remember { mutableStateOf(false) }
    Column(Modifier.elevatedCard(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (rows.isEmpty()) {
            Text(model.t("mwa.signTx.noIx"), color = AirwaveColors.TextMuted)
        }
        rows.forEachIndexed { index, row ->
            Text("${index + 1}. ${row.program}", color = AirwaveColors.TextDim, fontSize = 11.sp)
            if (row.kind != null) Text(model.t("mwa.ix." + row.kind), color = AirwaveColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            row.fields.forEach { (role, value) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(model.t("mwa.ix.role.$role"), color = AirwaveColors.TextDim, fontSize = 11.sp, modifier = Modifier.width(72.dp))
                    Text(value, color = AirwaveColors.TextMuted, fontSize = 12.sp)
                }
            }
            row.accounts.forEach { Text(it, color = AirwaveColors.TextMuted, fontSize = 12.sp) }
            if (row.dataHex != null) Text(row.dataHex.ifEmpty { model.t("mwa.signTx.emptyData") }, color = AirwaveColors.TextMuted, fontSize = 11.sp)
            if (index < rows.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(AirwaveColors.StrokeSoft))
            }
        }
        tx.keys.firstOrNull()?.let { payer ->
            Text(model.t("mwa.signTx.feePayer").replace("{payer}", BurnerKeys.short(payer)), color = AirwaveColors.TextMuted, fontSize = 12.sp)
        }
        Row(Modifier.clickable { openRaw = !openRaw }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DisclosureChevron(openRaw)
            Text(model.t("mwa.signTx.rawTx"), color = AirwaveColors.TextMuted, fontSize = 12.sp)
        }
        if (openRaw) {
            Text(session.signBytes.working.joinToString("") { "%02x".format(it) }, color = AirwaveColors.TextMuted, fontSize = 11.sp)
        }
    }
}

private fun inspectorUrl(session: MwaSession, model: WalletModel): String? {
    val message = SignTxWire.messageBytes(session.signBytes.working) ?: return null
    val cluster = if (model.settings.cluster.name == "Devnet") "devnet" else "mainnet-beta"
    val b64 = android.util.Base64.encodeToString(message, android.util.Base64.NO_WRAP)
    return "https://explorer.solana.com/tx/inspector?cluster=$cluster&message=" + java.net.URLEncoder.encode(b64, "UTF-8")
}
