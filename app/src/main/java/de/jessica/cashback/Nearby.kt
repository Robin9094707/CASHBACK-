package de.jessica.cashback

import android.Manifest
import android.location.LocationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.*

data class NearbyShop(val id:String,val name:String,val lat:Double,val lon:Double,val address:String,val postcode:String,val openingHours:String)
fun loadShops(context:Context):Pair<String,List<NearbyShop>> { val obj=JSONObject(context.assets.open("stores-duisburg.json").bufferedReader().use {it.readText()});val a=obj.getJSONArray("shops");return obj.getString("generatedAt") to (0 until a.length()).map { val x=a.getJSONObject(it);NearbyShop(x.getString("id"),x.getString("name"),x.getDouble("lat"),x.getDouble("lon"),x.optString("address"),x.optString("postcode"),x.optString("openingHours")) } }
fun retailerName(name:String):String { val text=name.lowercase();return when {text.contains("rossmann")->"Rossmann";text.startsWith("dm")->"dm";text.contains("aldi")->"Aldi";text.contains("lidl")->"Lidl";text.contains("netto")->"Netto";text.contains("rewe")->"REWE";text.contains("edeka")->"EDEKA";text.contains("kaufland")->"Kaufland";text.contains("müller")->"Müller";text.contains("penny")->"Penny";else->name} }
fun distanceKm(lat:Double,lon:Double,shop:NearbyShop):Double { val a=Math.toRadians(lat);val b=Math.toRadians(shop.lat);val dl=Math.toRadians(shop.lat-lat);val dn=Math.toRadians(shop.lon-lon);val h=sin(dl/2).pow(2)+cos(a)*cos(b)*sin(dn/2).pow(2);return 6371*2*asin(sqrt(h.coerceIn(0.0,1.0))) }
suspend fun approximateLocation(context:Context):Pair<Double,Double>? = withTimeoutOrNull(15000) {
    suspendCancellableCoroutine { continuation ->
        val manager=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val signal=CancellationSignal();continuation.invokeOnCancellation {signal.cancel()}
        try {
            LocationManagerCompat.getCurrentLocation(manager,LocationManager.NETWORK_PROVIDER,signal,ContextCompat.getMainExecutor(context)) { location -> if(continuation.isActive) continuation.resume(location?.let {it.latitude to it.longitude}) }
        } catch(e:Exception) {if(continuation.isActive) continuation.resume(null)}
    }
}
@Composable fun NearbyPanel(deals:List<Deal>,onSelect:(Deal)->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val directory=remember {runCatching {loadShops(context)}.getOrDefault("" to emptyList())}
    var center by remember {mutableStateOf(51.4344 to 6.7623)};var located by remember {mutableStateOf(false)};var loading by remember {mutableStateOf(false)};var message by remember {mutableStateOf<String?>(null)};var chain by remember {mutableStateOf("Alle")};var radius by remember {mutableStateOf("5 km")}
    fun locate() { scope.launch { loading=true;message=null;val result=approximateLocation(context);loading=false;if(result!=null) {center=result;located=true} else message="Standort nicht verfügbar. Du kannst die Karten-App oder den Ausgangspunkt Duisburg-Mitte verwenden." } }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed -> if(allowed) locate() else message="Standortfreigabe ist freiwillig. Die Übersicht bleibt für Duisburg verfügbar." }
    val shops=directory.second.map {it to distanceKm(center.first,center.second,it)}.filter {(shop,dist)->dist<=radius.substringBefore(' ').toInt() && (chain=="Alle" || retailerName(shop.name)==chain)}.sortedBy {it.second}.take(40)
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(30.dp)) { Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Icon(Icons.Rounded.NearMe,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(32.dp));Text("Dein Einkauf.\nGanz in der Nähe.",style=MaterialTheme.typography.headlineMedium);Text("Filialübersicht für Duisburg · ${directory.second.size} öffentliche Einträge",style=MaterialTheme.typography.bodyMedium);Button(onClick={if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) locate() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)},enabled=!loading) {if(loading) CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp) else Icon(Icons.Rounded.MyLocation,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(if(located) "Standort neu bestimmen" else "Ungefähren Standort verwenden")};Text(if(located) "Entfernungen ab deinem ungefähren Standort" else "Entfernungen ab Duisburg-Mitte",style=MaterialTheme.typography.labelMedium) } }
        message?.let {Notice(it,Icons.Rounded.Info)}
        ChipRow(listOf("3 km","5 km","10 km"),radius) {radius=it}
        ChipRow(listOf("Alle","dm","Rossmann","Lidl","Aldi","Netto","REWE","EDEKA","Kaufland","Müller","Penny"),chain) {chain=it}
        Notice("Filialdaten sind keine Bestands- oder Teilnahmebestätigung. Die Aktionsbedingungen entscheiden, ob ein Einkauf in dieser Filiale zählt. Dein Standort wird von der App nicht hochgeladen.",Icons.Rounded.Info)
        val local=deals.filter {it.kind=="Lokal" && it.cities.any {c -> c in listOf("Duisburg","Düsseldorf","Essen","Oberhausen","Mülheim","Krefeld","Moers","Dinslaken")}}
        if(local.isNotEmpty()) {Text("Lokale Hinweise im Ruhrgebiet",style=MaterialTheme.typography.titleLarge);local.forEach {deal -> CompactDealCard(deal,onClick={onSelect(deal)})}}
        Text("${shops.size} Filialen im gewählten Umkreis",style=MaterialTheme.typography.titleLarge)
        if(shops.isEmpty()) EmptyState("Keine Filiale im Bestand gefunden","Für andere Orte öffne die Karten-App. Der gespeicherte Filialbestand umfasst Duisburg.")
        shops.forEach { (shop,distance) ->
            val matches=deals.filter {d -> d.stores.any {s -> s.equals(retailerName(shop.name),true)} && d.kind!="Lokal"}
            Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Row(verticalAlignment=Alignment.Top) {Column(Modifier.weight(1f)) {Text(shop.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text((shop.address+" · "+shop.postcode).trim(' ','·'),style=MaterialTheme.typography.bodySmall)};Text(String.format(Locale.GERMANY,"%.1f km",distance),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)};if(matches.isNotEmpty()) Text("${matches.size} Aktionshinweise nennen ${retailerName(shop.name)}",style=MaterialTheme.typography.bodySmall);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {OutlinedButton(onClick={val uri=Uri.parse("geo:${shop.lat},${shop.lon}?q="+Uri.encode("${shop.lat},${shop.lon} (${shop.name})"));runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,uri))}.onFailure {context.openUrl("https://www.openstreetmap.org/?mlat=${shop.lat}&mlon=${shop.lon}#map=18/${shop.lat}/${shop.lon}")}}) {Icon(Icons.Rounded.Directions,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Karte")};if(matches.isNotEmpty()) TextButton(onClick={onSelect(matches.first())}) {Text("Aktion ansehen")}} } }
        }
        OutlinedButton(onClick={context.findStore(if(chain=="Alle") "Supermarkt" else chain)},modifier=Modifier.fillMaxWidth()) {Text("Weitere Filialen in der Karten-App")}
        Text("© OpenStreetMap-Mitwirkende · ODbL 1.0\nFilialbestand: ${directory.first.take(10)} · Adressen können fehlen oder sich ändern.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={context.openUrl("https://www.openstreetmap.org/copyright")}) {Text("Datenquelle & Lizenz")}
    }
}
