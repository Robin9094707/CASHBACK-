"""Bounded public feeds only. Honor robots, crawl delays, failures and HTTPS."""
import datetime as dt
import email.utils
import hashlib
import html as html_module
import re
import time
import urllib.error
import urllib.parse
import urllib.request
import urllib.robotparser
import xml.etree.ElementTree as ET
from bs4 import BeautifulSoup
AGENT='JessicasCashback/2.0 (+https://github.com/Robin9094707/CASHBACK-)'
STORES=['dm','Rossmann','Lidl','Aldi','Netto','REWE','EDEKA','Kaufland','Müller','Penny','Amazon','eBay','Apotheke']
CITIES=['Duisburg','Düsseldorf','Essen','Oberhausen','Mülheim','Krefeld','Moers','Dinslaken']
POLICIES={}
LAST={}

def read(url):
    if not url.startswith('https://'): raise ValueError('HTTPS required')
    request=urllib.request.Request(url,headers={'User-Agent':AGENT,'Accept':'application/rss+xml, application/xml, text/html'})
    with urllib.request.urlopen(request,timeout=30) as response:
        if urllib.parse.urlparse(response.url).scheme!='https': raise ValueError('Insecure redirect rejected')
        content=response.read(3_000_001)
        if len(content)>3_000_000: raise ValueError('Source exceeds size limit')
        return content.decode('utf-8')

def public_get(url):
    host=urllib.parse.urlsplit(url).netloc
    if host not in POLICIES:
        robots_url='https://'+host+'/robots.txt'
        try: text=read(robots_url)
        except urllib.error.HTTPError as error:
            if error.code==404: text='User-agent: *\nAllow: /'
            else: raise ValueError('robots.txt unavailable; source skipped') from error
        policy=urllib.robotparser.RobotFileParser();policy.parse(text.splitlines());POLICIES[host]=policy
        LAST[host]=time.monotonic()
    policy=POLICIES[host]
    if not policy.can_fetch(AGENT,url): raise ValueError('robots.txt excludes this URL')
    delay=max(2,policy.crawl_delay(AGENT) or 0)
    time.sleep(max(0,delay-(time.monotonic()-LAST.get(host,0))))
    try:return read(url)
    finally:LAST[host]=time.monotonic()

def text(html): return BeautifulSoup(html,'html.parser').get_text(' ',strip=True)
def iso_date(value):
    try:return dt.datetime.strptime(value,'%d.%m.%Y').date().isoformat()
    except ValueError:return ''

def kind(title):
    if re.search(r'uvm\.|übersicht|alle aktuellen|aktionen im überblick',title,re.I):return None
    if re.search(r'vertrag|strom|gas.deal|vpn|kredit|girokonto|casino|leasing',title,re.I):return None
    if re.search(r'lokal',title,re.I):return 'Lokal'
    if re.search(r'cashback|geld.?zurück',title,re.I):return 'Gratis testen' if re.search(r'100\s*%|gratis.testen',title,re.I) else 'Cashback'
    if re.search(r'abo|prime|kindle|bookbeat|audible',title,re.I):return None
    if re.search(r'gratis.testen|kostenlos.testen',title,re.I):return 'Gratis testen'
    if re.search(r'coupon|gutschein|rabattcode|\d+\s*%.*(?:auf alles|rabatt)',title,re.I):return 'Coupons'
    if re.search(r'gratis|kostenlos|geschenkt',title,re.I):return 'Gratisartikel'
    return None

def record(title,url,html,source,published='',image=''):
    title=html_module.unescape(title)
    category=kind(title)
    if category is None or not url.startswith('https://'):return None
    if published:
        try:
            date=dt.datetime.fromisoformat(published.replace('Z','+00:00'))
            if date.date()<dt.date.today()-dt.timedelta(days=14):return None
        except ValueError:pass
    soup=BeautifulSoup(html,'html.parser');body=soup.get_text(' ',strip=True)
    if re.search(r'aktion (?:ist )?(?:beendet|abgelaufen)|deal (?:ist )?abgelaufen',body,re.I):return None
    if not image:
        candidates=[i.get('src','') for i in soup.select('img') if 'avatar' not in i.get('alt','').lower()]
        image=next((i for i in candidates if i.startswith('https://') and not re.search(r'avatar|users/|pixel|tracking',i,re.I)), '')
    listed=[s for s in STORES if re.search(r'(?<!\w)'+re.escape(s)+r'(?!\w)',title+' '+body,re.I)]
    deadline_match=re.search(r'(?:einreich\w*|upload\w*|teilnahmeschluss|registrierungsfrist)[^.!?]{0,60}?(\d{2}\.\d{2}\.\d{4})',body,re.I)
    purchase_match=re.search(r'(?:kaufzeitraum|aktionszeitraum)[^.!?]{0,25}?(\d{2}\.\d{2}\.\d{4})\s*(?:bis|–|-)\s*(\d{2}\.\d{2}\.\d{4})',body,re.I)
    end=iso_date(purchase_match.group(2)) if purchase_match else ''
    if end and end<dt.date.today().isoformat():return None
    fixed=re.search(r'(\d+(?:[,.]\d{1,2})?)\s*€\s*(?:cashback|zurück)',title,re.I)
    percent=re.search(r'(\d+)\s*%',title)
    reward='100 %' if category=='Gratis testen' else (percent.group(1)+' %' if percent else ('Lokaler Hinweis' if category=='Lokal' else category))
    cities=[c for c in CITIES if c.lower() in title.lower()] if category=='Lokal' else []
    facts=[x.get_text(' ',strip=True)[:300] for x in soup.select('li') if re.search(r'kaufzeitraum|aktionszeitraum|cashback|einreich|mindestkauf|teilnahm|registr|kontingent',x.get_text(' ',strip=True),re.I)]
    return dict(id='public-'+hashlib.sha256(url.encode()).hexdigest()[:18],title=title,kind=category,reward=reward,url=url,source=source,sourceUrl=url,conditionsUrl='',image=image,stores=listed,facts=facts[:6],purchasePeriod=(purchase_match.group(0) if purchase_match else ''),purchaseEnd=end,deadline=iso_date(deadline_match.group(1)) if deadline_match else '',amountCents=round(float(fixed.group(1).replace(',','.'))*100) if fixed else 0,published=published,verified=False,cities=cities,category=category_for(title),evidence='Öffentlicher Aktionsbericht; Teilnahmebedingungen beim Anbieter prüfen')

def category_for(title):
    for label,pattern in [('Drogerie',r'zahn|oral.b|sensodyne|deo|dusch|shampoo|binde|tampon|tena|always|gillette|o\.b\.'),('Haushalt',r'zewa|lenor|spül|claro|calgon|vileda|reiniger|air.wick'),('Tierbedarf',r'katze|hund|felix|tierfutter|perfect.fit'),('Lebensmittel',r'milka|bifi|schwartau|käse|lipton|jacobs|kühne|arla|nutella|pizza|skyr|rockstar|milkana|solero|endori|chocol|wasa|kinder|tee|yfood')]:
        if re.search(pattern,title,re.I):return label
    return 'Weitere'

def rss(xml,source):
    root=ET.fromstring(xml);out=[]
    for item in root.findall('.//item'):
        title=item.findtext('title','').strip();url=item.findtext('link','').strip()
        content=item.findtext('{http://purl.org/rss/1.0/modules/content/}encoded') or item.findtext('description','')
        try:published=email.utils.parsedate_to_datetime(item.findtext('pubDate','')).isoformat()
        except (ValueError,TypeError):published=''
        deal=record(title,url,content,source,published)
        if deal:out.append(deal)
    return out

def dealdoktor_index(html):
    soup=BeautifulSoup(html,'html.parser');out=[]
    for card in soup.select('.box-deal')[:60]:
        heading=card.select_one('h2.title-loop a')
        if not heading:continue
        title=heading.get('title') or heading.get_text(' ',strip=True);url=heading.get('href','')
        snippet=card.select_one('.teaser');meta=card.select_one('.meta');published=''
        if meta:
            match=re.search(r'\d{2}\.\d{2}\.\d{4}',meta.get_text(' ',strip=True))
            if match:published=iso_date(match.group())+'T00:00:00+02:00'
        image=next((x.get('src','') for x in card.select('img') if '/users/' not in x.get('src','') and 'avatar' not in x.get('alt','').lower()),'')
        deal=record(title,url,str(snippet) if snippet else '', 'DealDoktor',published,image)
        if deal:out.append(deal)
    return out
