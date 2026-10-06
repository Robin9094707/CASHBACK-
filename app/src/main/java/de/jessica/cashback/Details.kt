package de.jessica.cashback

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DetailSheet(deal:Deal,entry:Entry,onSave:(Entry)->Unit,onDismiss:()->Unit,onPermission:()->Unit) {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    var note by remember(deal.id,entry.note) {mutableStateOf(entry.note)}
    var amount by remember(deal.id,entry.amountCents) {mutableStateOf(if(entry.amountCents==0) "" else "%.2f".format(java.util.Locale.GERMANY,entry.amountCents/100.0))}
    var amountError by remember {mutableStateOf(false)}
    val receipt=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) scope.launch {
        val path=withContext(Dispatchers.IO) { runCatching { val dir=File(context.filesDir,"receipts").apply {mkdirs()}; val dest=File(dir,UUID.randomUUID().toString()+".jpg"); context.contentResolver.openInputStream(uri)?.use { input -> dest.outputStream().use {out -> input.copyTo(out)} } ?: error("Beleg nicht erreichbar"); dest.absolutePath }.getOrNull() }
        if(path!=null) { if(entry.receipt.isNotBlank()) File(entry.receipt).delete(); onSave(entry.copy(receipt=path)) } else Toast.makeText(context,"Beleg konnte nicht gespeichert werden",Toast.LENGTH_SHORT).show()
    } }
    fun datePicker() { val date=runCatching {LocalDate.parse(entry.reminder)}.getOrDefault(LocalDate.now().plusDays(1)); val picker=DatePickerDialog(context,{_,year,month,day -> onSave(entry.copy(reminder=LocalDate.of(year,month+1,day).toString()));onPermission()},date.year,date.monthValue-1,date.dayOfMonth);picker.datePicker.minDate=System.currentTimeMillis()-1000;picker.show() }
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=40.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) { AssistChip(onClick={},label={Text(deal.kind)});Spacer(Modifier.weight(1f));IconButton(onClick={onSave(entry.copy(favorite=!entry.favorite))}) {Icon(if(entry.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"Favorit ändern")};IconButton(onClick={context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,deal.title+"\n"+deal.url),"Aktion teilen"))}) {Icon(Icons.Rounded.Share,"Aktion teilen")} }
            Text(deal.title,style=MaterialTheme.typography.headlineMedium)
            Text(if(deal.amountCents>0) "${deal.reward} · maximal ${money(deal.amountCents)} laut Quelle" else deal.reward,style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
            if(deal.url.isNotBlank()) Button(onClick={context.openUrl(deal.url)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {Text("Zur Aktion / Teilnahme starten");Spacer(Modifier.width(8.dp));Icon(Icons.AutoMirrored.Rounded.OpenInNew,null,Modifier.size(20.dp))}
            Notice("Vor dem Kauf auf der Aktionsseite prüfen: genaues Produkt, zugelassener Händler, Aktionspackung, Kontingent und Teilnahmeberechtigung. Der Link kann einen Anbieter- oder Affiliate-Redirect der Quelle enthalten.",Icons.Rounded.VerifiedUser)
            Text("Zeitraum & Bedingungen",style=MaterialTheme.typography.titleLarge)
            if(deal.purchasePeriod.isNotBlank()) Text(deal.purchasePeriod) else Text("Kaufzeitraum: nicht bekannt")
            Text("Einreichungsfrist: ${dateText(deal.deadline)}",fontWeight=FontWeight.SemiBold)
            if(deal.facts.isNotEmpty()) deal.facts.forEach { Text("• $it",style=MaterialTheme.typography.bodyMedium) } else Text("Keine weiteren Bedingungen hinterlegt. Bitte die Aktionsseite lesen.")
            Text("Kontingent: Verfügbarkeit beim Anbieter prüfen",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(deal.conditionsUrl.isNotBlank()) OutlinedButton(onClick={context.openUrl(deal.conditionsUrl)},modifier=Modifier.fillMaxWidth()) {Text("Teilnahmebedingungen öffnen")}
            if(deal.sourceUrl.isNotBlank()) TextButton(onClick={context.openUrl(deal.sourceUrl)}) {Text("Quelle: ${deal.source}")}
            Text("Wo einkaufen?",style=MaterialTheme.typography.titleLarge)
            if(deal.stores.isEmpty()) { Text("Keine Händlerzuordnung bestätigt. Die Aktionsseite nennt, welche Geschäfte zugelassen sind.");ChipRow(listOf("dm in der Nähe", "Rossmann in der Nähe", "REWE in der Nähe"), "") {context.findStore(it.substringBefore(" in der Nähe"))} } else {Text("In der Quelle erwähnte Händler. Das ist keine Bestätigung für jede Filiale oder jeden Einkauf.",style=MaterialTheme.typography.bodySmall);deal.stores.forEach {name -> OutlinedButton(onClick={context.findStore(name)},modifier=Modifier.fillMaxWidth()) {Icon(Icons.Rounded.NearMe,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("$name in der Nähe")}}}
            HorizontalDivider()
            Text("Deine Teilnahme",style=MaterialTheme.typography.titleLarge)
            Text("Den Status trägst du selbst ein. Die App erhält keine Rückmeldung vom Veranstalter.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            ChipRow(statuses,entry.status) {onSave(entry.copy(status=it))}
            OutlinedTextField(amount,{amount=it;amountError=false},label={Text("Dein Cashback-Betrag in €")},supportingText={Text(if(amountError) "Bitte einen gültigen Betrag eingeben" else "Erwarteten oder tatsächlich erstatteten Betrag eintragen")},isError=amountError,singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(note,{note=it},label={Text("Notiz / Referenznummer")},minLines=2,maxLines=5,modifier=Modifier.fillMaxWidth())
            Button(onClick={val value=amount.replace(',','.').toBigDecimalOrNull(); if(amount.isNotBlank() && (value==null || value.signum()<0 || value>java.math.BigDecimal("100000"))) amountError=true else {onSave(entry.copy(amountCents=value?.multiply(java.math.BigDecimal(100))?.toInt() ?: 0,note=note));Toast.makeText(context,"Teilnahme gespeichert",Toast.LENGTH_SHORT).show()}},modifier=Modifier.fillMaxWidth()) {Text("Betrag & Notiz speichern")}
            Text("Dein Beleg",style=MaterialTheme.typography.titleLarge)
            if(entry.receipt.isNotBlank() && File(entry.receipt).exists()) { AsyncImage(File(entry.receipt),"Gespeicherter Kassenbon",modifier=Modifier.fillMaxWidth().heightIn(max=350.dp));TextButton(onClick={val uri=androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".files",File(entry.receipt));context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Beleg teilen / speichern"))}) {Text("Beleg teilen / separat sichern")};TextButton(onClick={File(entry.receipt).delete();onSave(entry.copy(receipt=""))}) {Text("Beleg entfernen")} }
            OutlinedButton(onClick={receipt.launch(arrayOf("image/*"))},modifier=Modifier.fillMaxWidth()) {Icon(Icons.Rounded.AddPhotoAlternate,null);Spacer(Modifier.width(8.dp));Text(if(entry.receipt.isBlank()) "Belegfoto hinzufügen" else "Belegfoto ersetzen")}
            Text("Der Beleg wird lokal gespeichert. Über Teilen kannst du ihn separat sichern. Auf der Aktionsseite wählst du das Original oder eine gesicherte Kopie aus.",style=MaterialTheme.typography.bodySmall)
            Text("Deine Erinnerung",style=MaterialTheme.typography.titleLarge)
            if(entry.reminder.isNotBlank()) Text("Erinnern am ${dateText(entry.reminder)}")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {OutlinedButton(onClick={onSave(entry.copy(reminder=LocalDate.now().plusDays(1).toString()));onPermission()}) {Text("Morgen")};OutlinedButton(onClick={datePicker()}) {Text("Datum wählen")}}
            if(entry.reminder.isNotBlank()) TextButton(onClick={onSave(entry.copy(reminder=""))}) {Text("Erinnerung entfernen")}
            Text("Erinnerungen werden regelmäßig geprüft; im Energiesparmodus können sie später erscheinen.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick={onSave(entry.copy(hidden=!entry.hidden));onDismiss()},modifier=Modifier.fillMaxWidth()) {Icon(if(entry.hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,null);Spacer(Modifier.width(8.dp));Text(if(entry.hidden) "Aktion wieder einblenden" else "Aktion ausblenden")}
        }
    }
}
@Composable fun AddDealDialog(onDismiss:()->Unit,onSave:(String,String,String,Int,String,String)->Unit) {
    var title by remember {mutableStateOf("")};var url by remember {mutableStateOf("")};var store by remember {mutableStateOf("")};var amount by remember {mutableStateOf("")};var deadline by remember {mutableStateOf("")};var kind by remember {mutableStateOf("Cashback")};var error by remember {mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Deine eigene Aktion")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Cashback oder Coupon gefunden? Hier kannst du ihn selbst verwalten.")
        ChipRow(listOf("Cashback","Gratis testen","Coupons"),kind) {kind=it}
        OutlinedTextField(title,{title=it},label={Text("Produkt / Aktion *")},singleLine=true)
        OutlinedTextField(url,{url=it},label={Text("Aktionslink (https://)")},singleLine=true)
        OutlinedTextField(store,{store=it},label={Text("Händler")},singleLine=true)
        OutlinedTextField(amount,{amount=it},label={Text("Cashback in €")},singleLine=true)
        OutlinedTextField(deadline,{deadline=it},label={Text("Einreichungsfrist: JJJJ-MM-TT")},singleLine=true)
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(onClick={val number=amount.replace(',','.').toBigDecimalOrNull(); val validDate=deadline.isBlank() || runCatching {LocalDate.parse(deadline)}.isSuccess
        error=when {title.isBlank()->"Bitte einen Namen eingeben";url.isNotBlank() && (Uri.parse(url).scheme!="https" || Uri.parse(url).host.isNullOrBlank())->"Bitte einen gültigen HTTPS-Link eingeben";amount.isNotBlank() && (number==null || number.signum()<0 || number>java.math.BigDecimal("100000"))->"Bitte einen gültigen Betrag eingeben";!validDate->"Datum bitte im Format JJJJ-MM-TT";else->null}
        if(error==null) onSave(title.trim(),url.trim(),store.trim(),number?.multiply(java.math.BigDecimal(100))?.toInt() ?: 0,deadline.trim(),kind)
    }) {Text("Speichern")}},dismissButton={TextButton(onClick=onDismiss) {Text("Abbrechen")}})
}
data class Provider(val name:String,val detail:String,val url:String)
val providers=listOf(
    Provider("dm","dm-App, PAYBACK & aktuelle Angebote","https://www.dm.de/services/dm-app"),
    Provider("Rossmann","Rossmann-App & Coupons","https://www.rossmann.de/de/rossmann-app"),
    Provider("Lidl Plus","Digitale Coupons & Angebote","https://www.lidl.de/c/lidl-plus/s10007720"),
    Provider("ALDI","Aktuelle Angebote beim Händler","https://www.aldi-sued.de/"),
    Provider("Netto Marken-Discount","Angebote & Netto-App","https://www.netto-online.de/"),
    Provider("REWE","Angebote & REWE Bonus","https://www.rewe.de/"),
    Provider("EDEKA","App & Angebote","https://www.edeka.de/"),
    Provider("Kaufland","Kaufland Card & Angebote","https://www.kaufland.de/"),
    Provider("Müller","App & Coupons","https://www.mueller.de/"),
    Provider("Amazon","Aktuelle Angebote; Cashback ist aktionsabhängig","https://www.amazon.de/deals"),
    Provider("eBay","Angebote; Cashback bei Partnerportalen prüfen","https://www.ebay.de/deals"),
    Provider("Shoop","Online-Cashback; Konto beim Anbieter nötig","https://www.shoop.de/"),
    Provider("TopCashback","Online-Cashback & Aktionen","https://www.topcashback.de/"),
    Provider("PAYBACK","Punkte & eCoupons","https://www.payback.de/"),
    Provider("Scondoo","Produkt-Cashback über die Anbieter-App","https://scondoo.de/"),
    Provider("Shopmium","Produkt-Cashback über die Anbieter-App","https://www.shopmium.com/de/"),
    Provider("SPARWELT","Produkt-Cashback & Gratis testen","https://www.sparwelt.de/gratis/cashback"),
    Provider("mydealz","Community-Hinweise & Gutscheine","https://www.mydealz.de/")
)
@Composable fun ProviderRow(provider:Provider) { val context=LocalContext.current; Card(onClick={context.openUrl(provider.url)},modifier=Modifier.fillMaxWidth()) {Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {Column(Modifier.weight(1f)) {Text(provider.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text(provider.detail,style=MaterialTheme.typography.bodySmall)};Icon(Icons.AutoMirrored.Rounded.OpenInNew,null,Modifier.size(20.dp))}} }
