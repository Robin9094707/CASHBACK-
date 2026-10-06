package de.jessica.cashback

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Emerald=Color(0xFF006B59)
private val Mint=Color(0xFFCBF5A2)
private val Lavender=Color(0xFFE9DEFF)
private val stores=listOf("Alle", "dm", "Rossmann", "Lidl", "Aldi", "Netto", "REWE", "EDEKA", "Kaufland", "Müller", "Amazon", "eBay", "Apotheke")
val statuses=listOf("Merkliste","Gekauft","Eingereicht","Erstattet","Abgelehnt")
fun money(cents:Int)=NumberFormat.getCurrencyInstance(Locale.GERMANY).format(cents/100.0)
fun dateText(value:String)=runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }.getOrDefault("Unbekannt")
fun Context.openUrl(url:String) {
    runCatching { val uri=Uri.parse(url); require(uri.scheme=="https" && !uri.host.isNullOrBlank()); startActivity(Intent(Intent.ACTION_VIEW,uri)) }.onFailure { Toast.makeText(this,"Link konnte nicht geöffnet werden",Toast.LENGTH_SHORT).show() }
}
fun Context.findStore(name:String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q="+Uri.encode("$name in der Nähe")))) }.onFailure { openUrl("https://www.google.com/maps/search/?api=1&query="+Uri.encode("$name in der Nähe")) } }
class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { val vm:CashbackViewModel=viewModel(); CashbackTheme(vm.state.settings) { CashbackApp(vm,intent.getStringExtra("dealId")) } } }
}
@Composable fun CashbackTheme(settings:Settings, content:@Composable ()->Unit) {
    val context=LocalContext.current
    val dark=when(settings.dark) { "Dunkel"->true; "Hell"->false; else->isSystemInDarkTheme() }
    val scheme=if(settings.dynamic && Build.VERSION.SDK_INT>=31) { if(dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) } else if(dark) darkColorScheme(primary=Mint,onPrimary=Color(0xFF00382F),secondary=Color(0xFFCAB6F2),background=Color(0xFF101B17),surface=Color(0xFF14231E),surfaceContainer=Color(0xFF1C3028)) else lightColorScheme(primary=Emerald,onPrimary=Color.White,primaryContainer=Mint,onPrimaryContainer=Color(0xFF173322),secondary=Color(0xFF6D508F),secondaryContainer=Lavender,background=Color(0xFFF7F9F3),surface=Color(0xFFFCFDF8),surfaceContainer=Color(0xFFEEF2E9),outlineVariant=Color(0xFFDAE2D6))
    MaterialTheme(colorScheme=scheme,typography=Typography(headlineLarge=Typography().headlineLarge.copy(fontWeight=FontWeight.Bold,letterSpacing=(-1).sp),headlineMedium=Typography().headlineMedium.copy(fontWeight=FontWeight.Bold),titleLarge=Typography().titleLarge.copy(fontWeight=FontWeight.SemiBold)),shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(28.dp),extraLarge=RoundedCornerShape(36.dp)),content=content)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CashbackApp(vm:CashbackViewModel, initialId:String?) {
    val state=vm.state; val context=LocalContext.current; val scope=rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }; var selected by remember { mutableStateOf<Deal?>(null) }; var custom by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }; var retailer by remember { mutableStateOf("Alle") }; var kind by remember { mutableStateOf("Alle") }; var sort by remember { mutableStateOf("Neu") }; var listMode by remember { mutableStateOf("Favoriten") }
    val snack=remember { SnackbarHostState() }
    LaunchedEffect(initialId,state.deals) { if(initialId!=null) selected=(state.deals+state.entries.values.map { it.deal }).firstOrNull { it.id==initialId } }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(!granted) scope.launch { snack.showSnackbar("Alarme benötigen die Benachrichtigungsfreigabe. Du kannst sie später in Android erlauben.") } }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if(uri!=null) scope.launch { val success=withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(vm.store.export()) } ?: error("Datei nicht verfügbar") }.isSuccess }; snack.showSnackbar(if(success) "Sicherung gespeichert · Belegfotos separat sichern" else "Sicherung konnte nicht gespeichert werden") } }
    val importBackupLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) scope.launch { val result=withContext(Dispatchers.IO) { runCatching { val text=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Datei fehlt"); require(text.length<5_000_000); vm.store.importBackup(text) } }; vm.reload(); snack.showSnackbar(if(result.isSuccess) "Sicherung importiert" else "Diese Sicherung konnte nicht gelesen werden") } }
    Scaffold(containerColor=MaterialTheme.colorScheme.background,snackbarHost={SnackbarHost(snack)},floatingActionButton={ if(tab==1 || tab==2) ExtendedFloatingActionButton(onClick={custom=true},icon={Icon(Icons.Rounded.Add,null)},text={Text("Eigene Aktion")},containerColor=MaterialTheme.colorScheme.primaryContainer) },bottomBar={
        NavigationBar(containerColor=MaterialTheme.colorScheme.surface) {
            listOf(Triple("Entdecken",Icons.Rounded.Explore,0),Triple("Merkliste",Icons.Rounded.FavoriteBorder,1),Triple("Cashback",Icons.Rounded.AccountBalanceWallet,2),Triple("Mehr",Icons.Rounded.Tune,3)).forEach { (label,icon,index) -> NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Icon(icon,label)},label={Text(label)},colors=NavigationBarItemDefaults.colors(indicatorColor=MaterialTheme.colorScheme.primaryContainer)) }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=18.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item { Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("JESSICAS CASHBACK",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,letterSpacing=2.sp); Text(listOf("Mehr für dich.","Deine Fundstücke.","Geld zurück.","Ganz nach dir.")[tab],style=MaterialTheme.typography.headlineLarge) }; IconButton(onClick={vm.refresh()}) { if(state.loading) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp) else Icon(Icons.Rounded.Refresh,"Aktionen aktualisieren") } } }
            if(tab==0) {
                item { Hero(state.deals.count { it.kind=="Gratis testen" && (it.daysLeft()==null || it.daysLeft()!!>=0) },onClick={kind="Gratis testen"}) }
                if(state.error!=null || state.stale) item { Notice(state.error ?: "Die Quelle wurde länger nicht aktualisiert. Vor dem Kauf bitte den aktuellen Aktionsstatus prüfen.",Icons.Rounded.Info) }
                item { OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Produkt, Marke oder Händler")},leadingIcon={Icon(Icons.Rounded.Search,null)},singleLine=true,shape=RoundedCornerShape(24.dp),trailingIcon={if(search.isNotEmpty()) IconButton(onClick={search=""}) { Icon(Icons.Rounded.Close,"Suche löschen") }}) }
                item { ChipRow(listOf("Alle","Gratis testen","Cashback","Coupons"),kind) {kind=it} }
                item { ChipRow(stores,retailer) {retailer=it} }
                val own=state.entries.values.filter { it.deal.source=="Eigene Aktion" }.map { it.deal }
                var deals=(state.deals+own).distinctBy { it.id }.filter { state.entries[it.id]?.hidden!=true && (it.daysLeft()==null || it.daysLeft()!!>=0) && (kind=="Alle" || it.kind==kind) && (retailer=="Alle" || it.stores.any { s -> s.equals(retailer,true) }) && (it.title+" "+it.stores.joinToString()+" "+it.facts.joinToString()).contains(search,true) }
                deals=when(sort) { "Frist"->deals.sortedBy { it.daysLeft() ?: Long.MAX_VALUE }; "Betrag"->deals.sortedByDescending { it.amountCents }; else->deals }
                item { Row(verticalAlignment=Alignment.CenterVertically) { Text("${deals.size} Aktionen",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f)); TextButton(onClick={sort=when(sort){"Neu"->"Frist";"Frist"->"Betrag";else->"Neu"}}) { Icon(Icons.Rounded.Sort,null,Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(sort) } } }
                if(deals.isEmpty()) item { EmptyState(if(kind=="Coupons") "Coupons findest du bei den Anbietern" else "Noch kein passender Treffer",if(kind=="Coupons") "Unter Mehr → Anbieter findest du Händler-Apps und Gutscheinportale. Eigene Coupons kannst du deiner Merkliste hinzufügen." else "Versuche einen anderen Filter. Händler werden nur angezeigt, wenn die Quelle sie ausdrücklich erwähnt.") }
                items(deals,key={it.id}) { deal -> DealCard(deal,vm.entry(deal),onClick={selected=deal},onFavorite={val e=vm.entry(deal);vm.save(e.copy(favorite=!e.favorite))}) }
                item { val stamp=runCatching { Instant.parse(state.lastSync).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM. · HH:mm")) }.getOrDefault("unbekannt"); Text("Quellenstand: $stamp\nAktionshinweise von SPARWELT. Kontingente und Verfügbarkeit werden beim Anbieter bestätigt.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if(tab==1) {
                item { ChipRow(listOf("Favoriten","Einkauf","Ausgeblendet"),listMode) {listMode=it} }
                val entries=state.entries.values.filter { when(listMode) { "Ausgeblendet"->it.hidden; "Einkauf"->!it.hidden && it.favorite && it.status=="Merkliste"; else->!it.hidden && it.favorite } }.sortedBy { it.deal.title }
                if(entries.isEmpty()) item { EmptyState("Platz für deine Lieblingsaktionen","Tippe bei einer Aktion auf das Herz. Hier findest du deine persönliche Einkaufsliste und auch ausgeblendete Aktionen.") }
                items(entries,key={it.deal.id}) { e -> DealCard(e.deal,e,onClick={selected=e.deal},onFavorite={vm.save(e.copy(favorite=!e.favorite))}) }
            } else if(tab==2) {
                val active=state.entries.values.filter { it.status!="Merkliste" }
                val paid=active.filter { it.status=="Erstattet" }.sumOf { it.amountCents }; val pending=active.filter { it.status=="Gekauft" || it.status=="Eingereicht" }.sumOf { it.amountCents }
                item { Card(colors=CardDefaults.cardColors(containerColor=Emerald),shape=RoundedCornerShape(32.dp)) { Column(Modifier.fillMaxWidth().padding(24.dp)) { Text("DEIN CASHBACK",color=Mint,style=MaterialTheme.typography.labelLarge,letterSpacing=2.sp); Text(money(paid),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold,color=Color.White); Text("als erstattet markiert",color=Color.White.copy(alpha=.8f)); HorizontalDivider(Modifier.padding(vertical=20.dp),color=Color.White.copy(alpha=.2f)); Row { Column(Modifier.weight(1f)) { Text(money(pending),color=Mint,style=MaterialTheme.typography.titleLarge); Text("noch erwartet",color=Color.White) }; Column { Text("${active.size}",color=Mint,style=MaterialTheme.typography.titleLarge); Text("Teilnahmen",color=Color.White) } } } } }
                item { Text("Deine Teilnahmen",style=MaterialTheme.typography.titleLarge) }
                if(active.isEmpty()) item { EmptyState("Dein erster Cashback wartet","Öffne eine Aktion und setze den Status auf Gekauft. Danach kannst du Beleg, Einreichung und Erstattung festhalten.") }
                items(active.sortedBy { statuses.indexOf(it.status) },key={it.deal.id}) { e -> DealCard(e.deal,e,onClick={selected=e.deal},onFavorite={vm.save(e.copy(favorite=!e.favorite))}) }
            } else {
                item { Text("Deine Alarme",style=MaterialTheme.typography.titleLarge) }
                item { SettingsCard {
                    SwitchRow("Neue Cashback-Aktionen","Prüfung etwa alle 2 Stunden; Android kann sie verzögern",state.settings.newAlerts) { enabled -> vm.settings(state.settings.copy(newAlerts=enabled)); if(enabled && Build.VERSION.SDK_INT>=33 && !context.canNotify()) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    SwitchRow("Fristen im Blick","Erinnerung für gemerkte oder gekaufte Aktionen",state.settings.expiryAlerts) {enabled -> vm.settings(state.settings.copy(expiryAlerts=enabled)); if(enabled && Build.VERSION.SDK_INT>=33 && !context.canNotify()) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    SwitchRow("Nur Gratis-testen-Alarme","Filter für neue Aktionen",state.settings.onlyFree) {vm.settings(state.settings.copy(onlyFree=it))}
                    var words by remember(state.settings.keywords) {mutableStateOf(state.settings.keywords)}
                    OutlinedTextField(words,{words=it},label={Text("Alarm-Begriffe, durch Komma getrennt")},placeholder={Text("Zahnpasta, Rossmann, dm")},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp))
                    TextButton(onClick={vm.settings(state.settings.copy(keywords=words));scope.launch{snack.showSnackbar("Alarmfilter gespeichert")}}) {Text("Filter speichern")}
                    if(!context.canNotify()) TextButton(onClick={if(Build.VERSION.SDK_INT>=33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)}) {Text("Benachrichtigungen erlauben")}
                } }
                item { Text("Dein Design",style=MaterialTheme.typography.titleLarge) }
                item { SettingsCard { ChipRow(listOf("System","Hell","Dunkel"),state.settings.dark) {vm.settings(state.settings.copy(dark=it))}; SwitchRow("Farben deines Handys","Material You ab Android 12",state.settings.dynamic) {vm.settings(state.settings.copy(dynamic=it))} } }
                item { Text("Anbieter & Coupons",style=MaterialTheme.typography.titleLarge) }
                item { Text("Direkt zu Händler-Apps, Coupons und Cashback-Portalen. Die jeweiligen Anbieter zeigen ihre aktuellen Konditionen und verlangen gegebenenfalls eine Anmeldung.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                items(providers) { p -> ProviderRow(p) }
                item { Text("Deine Daten",style=MaterialTheme.typography.titleLarge) }
                item { SettingsCard { Text("Merkliste, Belege und Notizen bleiben auf diesem Handy. Keine Anmeldung, kein Abo und keine Werbe-SDKs. Beim Abruf von Aktionen, Bildern oder Anbieterseiten entsteht eine Verbindung zur jeweiligen Quelle."); OutlinedButton(onClick={export.launch("Jessicas-Cashback-Sicherung.json")},modifier=Modifier.fillMaxWidth()) {Icon(Icons.Rounded.FileDownload,null);Spacer(Modifier.width(8.dp));Text("Sicherung exportieren")}; OutlinedButton(onClick={importBackupLauncher.launch(arrayOf("application/json","text/plain"))},modifier=Modifier.fillMaxWidth()) {Text("Sicherung importieren / zusammenführen")}; Text("Belegfotos sind nicht in der JSON-Sicherung enthalten. Keine automatische Anmeldung oder Übermittlung von Belegen an Anbieter.",style=MaterialTheme.typography.bodySmall) } }
                item { Notice("Die App sammelt öffentlich auffindbare Aktionen. Sie kann keine vollständige weltweite Abdeckung und keine Erstattung garantieren. Prüfe vor dem Einkauf Produkt, Händler, Zeitraum, Kontingent und Teilnahmebedingungen.",Icons.Rounded.Info) }
            }
        }
    }
    if(custom) AddDealDialog(onDismiss={custom=false},onSave={title,url,storeName,amount,deadline,type -> vm.custom(title,url,storeName,amount,deadline,type);custom=false})
    selected?.let { deal -> DetailSheet(deal,vm.entry(deal),onSave={vm.save(it)},onDismiss={selected=null},onPermission={if(Build.VERSION.SDK_INT>=33 && !context.canNotify()) permission.launch(Manifest.permission.POST_NOTIFICATIONS)}) }
}

@Composable fun Hero(count:Int,onClick:()->Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(listOf(Emerald,Color(0xFF004D43)))).clickable(onClick=onClick)) {
        Column(Modifier.padding(24.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Surface(color=Mint,shape=RoundedCornerShape(50)) { Text("GRATIS TESTEN",Modifier.padding(horizontal=12.dp,vertical=6.dp),color=Color(0xFF173322),style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold) }; Spacer(Modifier.weight(1f)); Icon(Icons.Rounded.AutoAwesome,null,tint=Mint,modifier=Modifier.size(30.dp)) }; Spacer(Modifier.height(18.dp)); Text("Kaufen. Testen.\nGeld zurück.",style=MaterialTheme.typography.headlineLarge,color=Color.White); Spacer(Modifier.height(8.dp)); Text("$count Gratis-testen-Hinweise entdecken",color=Color.White.copy(alpha=.85f)); Spacer(Modifier.height(18.dp)); Row(verticalAlignment=Alignment.CenterVertically) {Text("Deine nächste Sparchance",color=Mint,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Icon(Icons.AutoMirrored.Rounded.ArrowForward,null,tint=Mint)} }
    }
}
@Composable fun ChipRow(values:List<String>,selected:String,onSelect:(String)->Unit) { LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { items(values) { value -> FilterChip(selected=value==selected,onClick={onSelect(value)},label={Text(value)},shape=RoundedCornerShape(50)) } } }
@Composable fun Notice(text:String,icon:ImageVector) { Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(20.dp)) {Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {Icon(icon,null,Modifier.size(22.dp));Text(text,style=MaterialTheme.typography.bodySmall)} } }
@Composable fun EmptyState(title:String,text:String) { Column(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally) { Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(28.dp)) {Icon(Icons.Rounded.Savings,null,Modifier.padding(24.dp).size(40.dp),tint=MaterialTheme.colorScheme.secondary)};Spacer(Modifier.height(18.dp));Text(title,style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(8.dp));Text(text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable fun DealCard(deal:Deal,entry:Entry,onClick:()->Unit,onFavorite:()->Unit) {
    Card(onClick=onClick,shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer),contentAlignment=Alignment.Center) { if(deal.image.startsWith("https://")) AsyncImage(deal.image,contentDescription=deal.title,contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize()) else Icon(Icons.Rounded.Redeem,null,Modifier.size(36.dp),tint=MaterialTheme.colorScheme.primary) }
        Column(Modifier.weight(1f)) { Text(deal.kind.uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary,letterSpacing=1.sp); Spacer(Modifier.height(5.dp));Text(deal.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,maxLines=3,overflow=TextOverflow.Ellipsis);Spacer(Modifier.height(5.dp));Text(deal.stores.joinToString(" · ").ifBlank{"Händler auf Aktionsseite prüfen"},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        IconButton(onClick=onFavorite,modifier=Modifier.size(40.dp)) {Icon(if(entry.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,if(entry.favorite) "Favorit entfernen" else "Als Favorit merken",tint=if(entry.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)}
    }; Spacer(Modifier.height(14.dp)); Row(verticalAlignment=Alignment.CenterVertically) { Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(50)) {Text(if(deal.amountCents>0) "bis ${money(deal.amountCents)}" else deal.reward,Modifier.padding(horizontal=12.dp,vertical=6.dp),style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)};Spacer(Modifier.weight(1f)); val days=deal.daysLeft(); Text(if(entry.hidden) "Ausgeblendet" else if(entry.status!="Merkliste") entry.status else if(days!=null) if(days==0L) "Kaufzeitraum endet heute" else if(days<0) "Kaufzeitraum beendet" else "Kauf: noch $days Tage" else deal.source,style=MaterialTheme.typography.labelSmall,color=if(days!=null && days in 0..2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) } } }
}
@Composable fun SettingsCard(content:@Composable ColumnScope.()->Unit) { Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)} }
@Composable fun SwitchRow(title:String,subtitle:String,value:Boolean,onChange:(Boolean)->Unit) {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {Column(Modifier.weight(1f)) {Text(title,style=MaterialTheme.typography.titleSmall);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(value,onChange)} }
