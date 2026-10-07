import datetime as dt
import unittest
from public_sources import record, rss, kind
from refresh_feed import parse

class PublicFeedChecks(unittest.TestCase):
    def test_cashback_at_prime_event_is_product_cashback(self):
        self.assertEqual(kind('Milka Weihnachts-Schoki bei Amazon mit 3€ Cashback – Prime Deal Days'), 'Cashback')
        self.assertIsNone(kind('Amazon Kindle Unlimited 3 Monate gratis testen'))

    def test_expired_rss_entry_is_not_new(self):
        xml='<rss><channel><item><title>Zahnpasta gratis testen</title><link>https://example.org/action</link><pubDate>Mon, 01 Jan 2001 00:00:00 GMT</pubDate></item></channel></rss>'
        self.assertEqual(rss(xml, 'Test'), [])

    def test_purchase_and_upload_are_different_dates(self):
        future=dt.date.today()+dt.timedelta(days=30)
        start=dt.date.today().strftime('%d.%m.%Y')
        end=future.strftime('%d.%m.%Y')
        upload=(future+dt.timedelta(days=10)).strftime('%d.%m.%Y')
        d=record('Zahnpasta 3€ Cashback','https://example.org/action',f'<li>Kaufzeitraum: {start} bis {end}</li><li>Einreichungsfrist: {upload}</li>','Test')
        self.assertEqual(d['purchaseEnd'],future.isoformat())
        self.assertEqual(d['deadline'],(future+dt.timedelta(days=10)).isoformat())
        self.assertEqual(d['amountCents'],300)

    def test_local_donation_event_is_not_promised_as_free_cashback(self):
        d=record('Düsseldorf (lokal): Reibekuchen für den guten Zweck','https://example.org/local','','Test')
        self.assertEqual(d['kind'],'Lokal')
        self.assertEqual(d['reward'],'Lokaler Hinweis')
        self.assertEqual(d['cities'],['Düsseldorf'])

    def test_empty_primary_source_does_not_replace_cached_feed(self):
        with self.assertRaises(ValueError):parse('<html>Maintenance</html>')

if __name__=='__main__':unittest.main()
