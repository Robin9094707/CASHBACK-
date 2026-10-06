package de.jessica.cashback

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

const val FEED_URL = "https://raw.githubusercontent.com/Robin9094707/CASHBACK-/main/data/deals.json"
fun JSONArray.strings() = (0 until length()).map { optString(it) }
data class Deal(val id: String, val title: String, val kind: String = "Cashback", val reward: String = "Cashback", val url: String = "", val source: String = "Eigene Aktion", val sourceUrl: String = "", val conditionsUrl: String = "", val image: String = "", val stores: List<String> = emptyList(), val facts: List<String> = emptyList(), val purchasePeriod: String = "", val purchaseEnd: String = "", val deadline: String = "", val amountCents: Int = 0) {
    fun json() = JSONObject().put("id",id).put("title",title).put("kind",kind).put("reward",reward).put("url",url).put("source",source).put("sourceUrl",sourceUrl).put("conditionsUrl",conditionsUrl).put("image",image).put("stores",JSONArray(stores)).put("facts",JSONArray(facts)).put("purchasePeriod",purchasePeriod).put("purchaseEnd",purchaseEnd).put("deadline",deadline).put("amountCents",amountCents)
    companion object { fun parse(o: JSONObject) = Deal(o.getString("id"),o.getString("title"),o.optString("kind","Cashback"),o.optString("reward","Cashback"),o.optString("url"),o.optString("source"),o.optString("sourceUrl"),o.optString("conditionsUrl"),o.optString("image"),o.optJSONArray("stores")?.strings() ?: emptyList(),o.optJSONArray("facts")?.strings() ?: emptyList(),o.optString("purchasePeriod"),o.optString("purchaseEnd"),o.optString("deadline"),o.optInt("amountCents")) }
    fun daysLeft(): Long? = runCatching { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(purchaseEnd)) }.getOrNull()
}
data class Entry(val deal: Deal, val favorite: Boolean=false, val hidden: Boolean=false, val status: String="Merkliste", val amountCents: Int=0, val note: String="", val receipt: String="", val reminder: String="") {
    fun json() = JSONObject().put("deal",deal.json()).put("favorite",favorite).put("hidden",hidden).put("status",status).put("amountCents",amountCents).put("note",note).put("receipt",receipt).put("reminder",reminder)
    companion object { fun parse(o: JSONObject) = Entry(Deal.parse(o.getJSONObject("deal")),o.optBoolean("favorite"),o.optBoolean("hidden"),o.optString("status","Merkliste"),o.optInt("amountCents"),o.optString("note"),o.optString("receipt"),o.optString("reminder")) }
}
data class Settings(val newAlerts: Boolean=false, val expiryAlerts: Boolean=true, val onlyFree: Boolean=false, val keywords: String="", val dark: String="System", val dynamic: Boolean=false) {
    fun json()=JSONObject().put("newAlerts",newAlerts).put("expiryAlerts",expiryAlerts).put("onlyFree",onlyFree).put("keywords",keywords).put("dark",dark).put("dynamic",dynamic)
    companion object { fun parse(o:JSONObject)=Settings(o.optBoolean("newAlerts"),o.optBoolean("expiryAlerts",true),o.optBoolean("onlyFree"),o.optString("keywords"),o.optString("dark","System"),o.optBoolean("dynamic")) }
}
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("jessicas_cashback",Context.MODE_PRIVATE)
    fun entries(): Map<String,Entry> = runCatching { val a=JSONArray(prefs.getString("entries","[]")); (0 until a.length()).map { Entry.parse(a.getJSONObject(it)) }.associateBy { it.deal.id } }.getOrDefault(emptyMap())
    fun settings()=Settings.parse(JSONObject(prefs.getString("settings","{}")!!))
    fun settings(s: Settings) { prefs.edit().putString("settings",s.json().toString()).apply() }
    fun save(entry: Entry) = synchronized(lock) { val m=entries().toMutableMap(); m[entry.deal.id]=entry; prefs.edit().putString("entries",JSONArray(m.values.map { it.json() }).toString()).commit(); Unit }
    fun cache()=prefs.getString("feed",null)
    fun cache(value:String) { prefs.edit().putString("feed",value).apply() }
    fun markOnce(key:String):Boolean = synchronized(lock) { if(prefs.getBoolean(key,false)) false else { prefs.edit().putBoolean(key,true).commit(); true } }
    fun baseline()=prefs.getStringSet("seen",null)
    fun seen(ids:Set<String>) { prefs.edit().putStringSet("seen",ids).apply() }
    fun export(): String=JSONObject().put("schemaVersion",1).put("settings",settings().json()).put("entries",JSONArray(entries().values.map { it.copy(receipt="").json() })).toString(2)
    fun importBackup(text:String) {
        val root=JSONObject(text); require(root.getInt("schemaVersion")==1)
        val a=root.getJSONArray("entries"); require(a.length() <= 10000)
        val parsed=(0 until a.length()).map { Entry.parse(a.getJSONObject(it)).copy(receipt="") }
        val s=Settings.parse(root.getJSONObject("settings"))
        synchronized(lock) { val m=entries().toMutableMap(); parsed.forEach { m[it.deal.id]=it }; prefs.edit().putString("entries",JSONArray(m.values.map { it.json() }).toString()).putString("settings",s.json().toString()).commit() }
    }
    companion object { private val lock=Any() }
}
suspend fun downloadFeed(): String = withContext(Dispatchers.IO) {
    val connection=URL(FEED_URL).openConnection() as HttpURLConnection
    connection.connectTimeout=15000; connection.readTimeout=20000
    try { require(connection.responseCode==200) { "Quelle momentan nicht erreichbar (${connection.responseCode})" }; val text=connection.inputStream.bufferedReader().use { it.readText() }; require(text.length<3_000_000); require(JSONObject(text).getInt("schemaVersion")==1); text } finally { connection.disconnect() }
}
fun parseDeals(text:String):List<Deal> { val a=JSONObject(text).getJSONArray("deals"); return (0 until a.length()).map { Deal.parse(a.getJSONObject(it)) } }
data class AppState(val deals:List<Deal> = emptyList(), val entries:Map<String,Entry> = emptyMap(), val settings:Settings=Settings(), val loading:Boolean=false, val error:String?=null, val lastSync:String="", val stale:Boolean=false)
class CashbackViewModel(app:Application):AndroidViewModel(app) {
    val store=Store(app)
    var state by androidx.compose.runtime.mutableStateOf(AppState()); private set
    init { val text=store.cache() ?: runCatching { app.assets.open("deals.json").bufferedReader().use { it.readText() } }.getOrNull(); if(text!=null) accept(text); reload(); refresh() }
    private fun accept(text:String) { val obj=JSONObject(text); val stamp=obj.optString("lastSuccessAt",obj.optString("generatedAt")); val old=runCatching { ChronoUnit.HOURS.between(Instant.parse(stamp),Instant.now())>24 }.getOrDefault(true); state=state.copy(deals=parseDeals(text),lastSync=stamp,stale=obj.optString("status")!="ok" || old) }
    fun reload() { state=state.copy(entries=store.entries(),settings=store.settings()) }
    fun refresh() { if(state.loading) return; viewModelScope.launch { state=state.copy(loading=true,error=null); try { val text=downloadFeed(); store.cache(text); accept(text) } catch(e:Exception) { state=state.copy(error="Aktualisierung fehlgeschlagen. Gespeicherte Aktionen bleiben verfügbar.",stale=true) } finally { state=state.copy(loading=false); reload() } } }
    fun entry(deal:Deal)=state.entries[deal.id] ?: Entry(deal,amountCents=deal.amountCents)
    fun save(e:Entry) { store.save(e); reload() }
    fun settings(s:Settings) { store.settings(s); reload() }
    fun custom(title:String,url:String,storeName:String,amount:Int,deadline:String,kind:String) { val deal=Deal("own-"+UUID.randomUUID(),title,kind=kind,reward=if(kind=="Gratis testen") "100 %" else if(kind=="Coupons") "Coupon" else "Cashback",url=url,stores=listOf(storeName).filter { it.isNotBlank() },deadline=deadline,amountCents=amount); save(Entry(deal,favorite=true,amountCents=amount)) }
}
