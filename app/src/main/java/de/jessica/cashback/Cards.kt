package de.jessica.cashback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable fun ProductImage(deal:Deal,modifier:Modifier=Modifier) {
    AsyncImage(model=ImageRequest.Builder(LocalContext.current).data(deal.image.takeIf {it.startsWith("https://")}).crossfade(true).build(),contentDescription="Produktbild zur Aktion: ${deal.title}",placeholder=painterResource(R.drawable.product_placeholder),error=painterResource(R.drawable.product_placeholder),fallback=painterResource(R.drawable.product_placeholder),contentScale=ContentScale.Fit,modifier=modifier.background(Color.White))
}
@Composable fun DealCard(deal:Deal,entry:Entry,onClick:()->Unit,onFavorite:()->Unit) {
    ElevatedCard(onClick=onClick,shape=RoundedCornerShape(28.dp),colors=CardDefaults.elevatedCardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.elevatedCardElevation(defaultElevation=1.dp)) {
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            ProductImage(deal,Modifier.fillMaxSize().padding(12.dp))
            Surface(Modifier.align(Alignment.TopStart).padding(12.dp),color=if(deal.kind=="Gratis testen") Color(0xFFCBF5A2) else MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(50)) {Text(deal.reward,Modifier.padding(horizontal=12.dp,vertical=7.dp),style=MaterialTheme.typography.labelLarge,color=if(deal.kind=="Gratis testen") Color(0xFF173322) else MaterialTheme.colorScheme.onSecondaryContainer,fontWeight=FontWeight.Bold)}
            FilledIconButton(onClick=onFavorite,modifier=Modifier.align(Alignment.TopEnd).padding(10.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=MaterialTheme.colorScheme.surface.copy(alpha=.95f))) {Icon(if(entry.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,if(entry.favorite) "Favorit entfernen" else "Aktion merken",tint=MaterialTheme.colorScheme.primary)}
        }
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Text("${deal.category.uppercase()} · ${deal.kind}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
            Text(deal.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,maxLines=3,overflow=TextOverflow.Ellipsis)
            Text(deal.stores.joinToString(" · ").ifBlank {"Händler noch prüfen"},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(if(deal.amountCents>0) "bis ${money(deal.amountCents)}" else deal.source,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                val days=deal.daysLeft();val label=if(entry.hidden) "Ausgeblendet" else if(entry.status!="Merkliste") entry.status else when {days==null->"Details ansehen";days<0->"Kauf beendet";days==0L->"Kauf endet heute";else->"Noch $days Tage"}
                Text(label,style=MaterialTheme.typography.labelSmall,color=if(days!=null && days in 0..2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.widthIn(max=140.dp))
            }
            if(deal.amountCents>0) Text(deal.source,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable fun FeaturedStrip(deals:List<Deal>,onSelect:(Deal)->Unit) {
    if(deals.isEmpty()) return
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) { Row(verticalAlignment=Alignment.CenterVertically) {Icon(Icons.Rounded.AutoAwesome,null,Modifier.size(22.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(8.dp));Text("Für deinen nächsten Einkauf",style=MaterialTheme.typography.titleLarge)}
        LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {items(deals,key={it.id}) {deal -> Card(onClick={onSelect(deal)},modifier=Modifier.width(190.dp),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {ProductImage(deal,Modifier.fillMaxWidth().height(130.dp).padding(10.dp));Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {Text(deal.title,style=MaterialTheme.typography.titleSmall,maxLines=2,overflow=TextOverflow.Ellipsis);Text(deal.reward,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)}}} }
    }
}
@Composable fun CompactDealCard(deal:Deal,onClick:()->Unit) {Card(onClick=onClick,shape=RoundedCornerShape(22.dp)) {Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {ProductImage(deal,Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)));Column(Modifier.weight(1f)) {Text(deal.title,style=MaterialTheme.typography.titleSmall,maxLines=3,overflow=TextOverflow.Ellipsis);Text(deal.cities.joinToString(" · ").ifBlank {deal.source},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}} }
@Composable fun SourceStatusCard(source:FeedSource) {Card(shape=RoundedCornerShape(22.dp)) {Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {Row(verticalAlignment=Alignment.CenterVertically) {Icon(if(source.status=="ok") Icons.Rounded.CheckCircle else Icons.Rounded.CloudOff,null,tint=if(source.status=="ok") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,modifier=Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text(source.name,style=MaterialTheme.typography.titleSmall)};Text(if(source.status=="ok") "${source.count} Hinweise beim letzten Abruf" else "Quelle nicht erreichbar · gespeicherte Hinweise bleiben verfügbar",style=MaterialTheme.typography.bodySmall)} } }
