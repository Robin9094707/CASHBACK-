package de.jessica.cashback

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class CashbackApplication:Application() {
    override fun onCreate() { super.onCreate(); val manager=getSystemService(NotificationManager::class.java); manager.createNotificationChannel(NotificationChannel("cashback","Cashback & Erinnerungen",NotificationManager.IMPORTANCE_DEFAULT)); WorkManager.getInstance(this).enqueueUniquePeriodicWork("cashback-sync",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<CashbackWorker>(2,TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()); WorkManager.getInstance(this).enqueueUniquePeriodicWork("cashback-reminders",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<ReminderWorker>(6,TimeUnit.HOURS).build()) }
}
fun Context.canNotify() = Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
fun notifyCashback(context:Context,id:String,title:String,body:String) {
    if(!context.canNotify()) return
    val intent=Intent(context,MainActivity::class.java).putExtra("dealId",id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val pending=PendingIntent.getActivity(context,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val n=NotificationCompat.Builder(context,"cashback").setSmallIcon(de.jessica.cashback.R.drawable.ic_notification).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(pending).build()
    context.getSystemService(NotificationManager::class.java).notify(id.hashCode(),n)
}
class CashbackWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val store=Store(applicationContext)
        return try { val text=downloadFeed(); store.cache(text); val deals=parseDeals(text); val previous=store.baseline(); val settings=store.settings(); val entries=store.entries(); val words=settings.keywords.split(',').map { it.trim() }.filter { it.isNotBlank() }
            if(previous!=null && settings.newAlerts && applicationContext.canNotify()) {
                val fresh=deals.filter { it.id !in previous && entries[it.id]?.hidden!=true && (it.daysLeft()==null || it.daysLeft()!!>=0) && (!settings.onlyFree || it.kind=="Gratis testen") && (words.isEmpty() || words.any { word -> (it.title+" "+it.stores.joinToString()).contains(word,true) }) }
                if(fresh.isNotEmpty()) notifyCashback(applicationContext,"new-deals","${fresh.size} neue Cashback-Aktionen",fresh.take(3).joinToString(" • ") { it.title })
            }
            store.seen((previous ?: emptySet()) + deals.map { it.id }); Result.success()
        } catch(e:Exception) { Result.retry() }
    }
}
class ReminderWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val store=Store(applicationContext); val settings=store.settings(); val today=LocalDate.now()
        if(!applicationContext.canNotify()) return Result.success()
        store.entries().values.filter { !it.hidden && it.status!="Erstattet" && it.status!="Abgelehnt" }.forEach { e ->
            val date=runCatching { LocalDate.parse(e.reminder) }.getOrNull()
            if(date!=null && !date.isAfter(today) && store.markOnce("reminder-${e.deal.id}-${e.reminder}")) notifyCashback(applicationContext,e.deal.id,"Deine Cashback-Erinnerung",e.deal.title)
            val end=runCatching { LocalDate.parse(e.deal.deadline.ifBlank { e.deal.purchaseEnd }) }.getOrNull()
            if(settings.expiryAlerts && (e.favorite || e.status!="Merkliste") && end!=null && !end.isBefore(today) && !end.isAfter(today.plusDays(2)) && store.markOnce("expiry-${e.deal.id}-$end")) notifyCashback(applicationContext,e.deal.id,"Frist im Blick behalten",e.deal.title+" · "+end)
        }; return Result.success()
    }
}
