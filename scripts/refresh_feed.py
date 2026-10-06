"""Public-page adapter. No login, private API or access-control bypass."""
import datetime as dt
import hashlib
import json
import re
import sys
import urllib.request
from pathlib import Path
from bs4 import BeautifulSoup
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
        kind = 'Gratis testen' if re.search(r'100\s*%|gratis.testen|kostenlos.testen', title, re.I) else 'Cashback'
        stores = [s for s in STORES if re.search(r'(?<!\w)' + re.escape(s) + r'(?!\w)', desc + ' ' + title, re.I)]
        conditions = re.search(r'\[Teilnahmebedingungen\]\((https://[^\s)]+)\)', desc, re.I)
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
            published=value(obj, 'publishedAt'), verified=False)
    if not result:
        raise ValueError('No cashback records parsed; previous feed retained')
    return list(result.values())

def main():
    path = ROOT / 'data/deals.json'
    now = dt.datetime.now(dt.timezone.utc).isoformat()
    old = json.loads(path.read_text()) if path.exists() else {'deals': []}
    try:
        request = urllib.request.Request(SOURCE, headers={'User-Agent': 'JessicasCashback/1.0 (public cashback index; no login)'})
        with urllib.request.urlopen(request, timeout=40) as response:
            deals = parse(response.read().decode('utf-8'))
        feed = dict(schemaVersion=1, generatedAt=now, lastSuccessAt=now, status='ok',
            sources=[dict(name='SPARWELT', url=SOURCE, status='ok', count=len(deals))], deals=deals)
    except Exception as error:
        feed = {**old, 'generatedAt': now, 'status': 'stale', 'error': str(error),
            'sources': [dict(name='SPARWELT', url=SOURCE, status='error')]}
        path.parent.mkdir(exist_ok=True)
        path.write_text(json.dumps(feed, ensure_ascii=False, indent=2) + '\n')
        raise
    path.write_text(json.dumps(feed, ensure_ascii=False, indent=2) + '\n')
    print(f'{len(deals)} live cashback records; updated {now}')

if __name__ == '__main__':
    main()
