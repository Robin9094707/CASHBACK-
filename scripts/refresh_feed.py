"""Public-page adapter. No login, private API or access-control bypass."""
import datetime as dt
import hashlib
import json
import re
import difflib
import sys
import urllib.request
from pathlib import Path
from bs4 import BeautifulSoup
from public_sources import public_get, rss, dealdoktor_index, category_for
ROOT = Path(__file__).resolve().parents[1]
SOURCE = 'https://www.sparwelt.de/gratis/cashback'
STORES = ['dm', 'Rossmann', 'Lidl', 'Aldi', 'Netto', 'REWE', 'EDEKA', 'Kaufland', 'Müller', 'Penny', 'Amazon', 'eBay', 'Apotheke']

def clean(text):
    return re.sub(r'\s+', ' ', re.sub(r'[*_#]', '', text)).strip()

def parse(html):
    soup = BeautifulSoup(html, 'html.parser')
    script = soup.select_one('#__NUXT_DATA__')
    if script is None:
        raise ValueError('Source structure changed: no public Nuxt payload')
    arr = json.loads(script.text)
    def value(obj, key, default=''):
        idx = obj.get(key)
        return arr[idx] if isinstance(idx, int) and idx >= 0 else default
    shown = {x.get('data-content-uuid', '').split(':')[-1] for x in soup.select('[data-content-uuid]')}
    result = {}
    today = dt.date.today()
    for obj in arr:
        if not isinstance(obj, dict) or not {'title', 'description', 'affiliateUrl', 'id'} <= obj.keys():
            continue
        title, desc = value(obj, 'title'), value(obj, 'description')
        if not isinstance(title, str) or not isinstance(desc, str) or str(value(obj, 'id')) not in shown:
            continue
        if not re.search(r'cashback|geld.?zurück|gratis.testen|kostenlos.testen', title + ' ' + desc, re.I):
            continue
        url = value(obj, 'affiliateUrl')
        if not isinstance(url, str) or not url.startswith('https://'):
            continue
        facts = []
        purchase = ''
        deadline = ''
        for line in desc.splitlines():
            t = clean(line.lstrip('- '))
            if not t or not line.lstrip().startswith('-'):
                continue
            if re.search(r'aktions|kauf|wichtig|teilnahm|einlös|registr|upload|kontingent|rückerstatt|händler|verfüg|einreich', t, re.I):
                facts.append(t[:350])
            dates = re.findall(r'\b(\d{2}\.\d{2}\.\d{4})\b', t)
            if dates and re.search(r'kaufzeitraum|aktionszeitraum', t, re.I):
                purchase = t
            if dates and re.search(r'einreich|upload|registrierung.*bis|teilnahmeschluss', t, re.I):
                deadline = dt.datetime.strptime(dates[-1], '%d.%m.%Y').date().isoformat()
        ends = re.findall(r'\b(\d{2}\.\d{2}\.\d{4})\b', purchase)
        purchase_end = dt.datetime.strptime(ends[-1], '%d.%m.%Y').date().isoformat() if ends else ''
        if purchase_end and dt.date.fromisoformat(purchase_end) < today:
            continue
        kind = 'Gratis testen' if re.search(r'100\s*%|gratis.testen|kostenlos.testen', title + ' ' + desc, re.I) else 'Cashback'
        stores = [s for s in STORES if re.search(r'(?<!\w)' + re.escape(s) + r'(?!\w)', desc + ' ' + title, re.I)]
        conditions = re.search(r'\[Teilnahmebedingungen\]\((https://[^\s]+)\)', desc, re.I)
        amount = re.search(r'(?:maximal\w* (?:Rückerstattung|Erstattung)|bis zu|max\.)[^\d\n]{0,30}(\d+[,.]\d{2})\s*€', desc, re.I)
        image_obj = value(obj, 'image', {})
        image_url = value(image_obj, 'url') if isinstance(image_obj, dict) else ''
        if image_url.startswith('/'):
            image_url = 'https://sparwelt-cdn-assets.imgix.net' + image_url + '?auto=format,compress&w=480'
        key = 'sparwelt-' + str(value(obj, 'id'))
        result[key] = dict(id=key, title=title, kind=kind, source='SPARWELT', sourceUrl=SOURCE,
            url=url, conditionsUrl=conditions.group(1) if conditions else '', image=image_url,
            stores=stores, facts=facts[:8], purchasePeriod=purchase, purchaseEnd=purchase_end,
            deadline=deadline, reward='100 %' if kind == 'Gratis testen' else 'Cashback',
            amountCents=round(float(amount.group(1).replace(',', '.')) * 100) if amount else 0,
            published=value(obj, 'publishedAt'), verified=False, cities=[], category=category_for(title), evidence='Metadaten aus öffentlicher Cashback-Übersicht')
    if not result:
        raise ValueError('No cashback records parsed; previous feed retained')
    return list(result.values())

SOURCES = [
    ('SPARWELT', SOURCE, parse),
    ('DealDoktor', 'https://www.dealdoktor.de/themenwelten/gratis/', dealdoktor_index),
    ('DealDoktor RSS', 'https://www.dealdoktor.de/feed/', lambda value: rss(value, 'DealDoktor')),
    ('MonsterDealz', 'https://www.monsterdealz.de/feed/', lambda value: rss(value, 'MonsterDealz')),
    ('Kostenlos.de', 'https://www.kostenlos.de/feed/', lambda value: rss(value, 'Kostenlos.de')),
]

def main():
    path=ROOT/'data/deals.json';now=dt.datetime.now(dt.timezone.utc).isoformat()
    old=json.loads(path.read_text()) if path.exists() else {'deals': []}
    collected=[];statuses=[];success=0
    for name,url,adapter in SOURCES:
        try:
            deals=adapter(public_get(url));collected.extend(deals);success+=1
            statuses.append(dict(name=name,url=url,status='ok',count=len(deals),updatedAt=now))
            print(f'{name}: {len(deals)} matching public records')
        except Exception as error:
            statuses.append(dict(name=name,url=url,status='error',count=0,error=str(error)[:200]))
            preserved=[d for d in old['deals'] if d.get('source')==name and (not d.get('purchaseEnd') or d['purchaseEnd']>=dt.date.today().isoformat())]
            collected.extend(preserved)
            print(f'{name}: source unavailable; {len(preserved)} cached records retained')
    unique={}
    def identity(title):
        words=re.findall(r'[a-zäöüß0-9]+',title.lower())
        ignored={'gratis','testen','kostenlos','geld','zurück','cashback','jetzt','wieder','neue','aktion','kaufen','erhalten','dank','top','100','prozent'}
        return ' '.join(sorted(w for w in words if w not in ignored))
    for deal in collected:
        if deal['id'] in unique:continue
        signature=identity(deal['title'])
        duplicate=next((d for d in unique.values() if d['kind']==deal['kind'] and len(signature)>10 and difflib.SequenceMatcher(None,signature,identity(d['title'])).ratio()>=0.84),None)
        if duplicate:
            duplicate.setdefault('otherSources',[]).append(deal['source'])
            continue
        unique[deal['id']]=deal
    feed=dict(schemaVersion=1,generatedAt=now,lastSuccessAt=now if success else old.get('lastSuccessAt',''),status='ok' if success==len(SOURCES) else ('partial' if success else 'stale'),sources=statuses,deals=list(unique.values()))
    path.parent.mkdir(exist_ok=True);path.write_text(json.dumps(feed,ensure_ascii=False,indent=2)+'\n')
    print(f"{len(unique)} records from {success}/{len(SOURCES)} responding adapters")
    if not success:raise RuntimeError('All public sources unavailable; cached feed retained')

if __name__=='__main__':main()
