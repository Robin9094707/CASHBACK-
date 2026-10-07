# Jessicas Cashback 🌿

Native, deutschsprachige Android-App für Produkt-Cashback, Gratis-testen-Aktionen und eine persönliche Coupon-Verwaltung. Für Samsung und andere Android-Handys ab Android 8. Kein Konto, kein Abo, kein kostenpflichtiger Backend-Dienst.

## Neu in Version 2.0

Responsive Kartenraster und Navigation für Handy, Querformat, Split-Screen und Tablets. Große Produktbilder mit Lade- und Fehlerplatzhaltern, Bildkarussell, Kategorie- und Quellenfilter, Quellenstatus und lokale Hinweise. Die Ansicht aktualisiert sich bei der Rückkehr in die App. Jede Registerkarte behält ihre Scrollposition.

„Nähe“ enthält einen begrenzten, offline nutzbaren Bestand mit 209 benannten Filialen in Duisburg. Auf Wunsch berechnet die App Entfernungen aus einem ungefähren Standort. Die Koordinaten bleiben im Arbeitsspeicher auf dem Gerät; keine Hintergrundortung. Filialdaten stammen aus einem einmaligen, begrenzten OpenStreetMap-Abruf vom 07.10.2026. Die App fragt keinen öffentlichen Overpass-Server als Backend ab. Andere Orte können über die Karten-App gesucht werden. Filialdaten, Warenbestand und Teilnahmeberechtigung sind verschiedene Informationen: eine Filiale in der Nähe bestätigt keine Cashback-Teilnahme.

## APK installieren

[**Jessicas-Cashback.apk herunterladen**](https://github.com/Robin9094707/CASHBACK-/releases/download/android-latest/Jessicas-Cashback.apk)

**Bei einer vorhandenen Version zuerst unter Mehr die JSON-Sicherung exportieren und Belegfotos separat sichern.** Der Entwicklungsschlüssel wird im Build neu erzeugt; Android kann daher statt eines Updates eine Neuinstallation verlangen.

APK auf dem Handy öffnen und Androids Installationserlaubnis für die verwendete Download-App erteilen. Optional Benachrichtigungen zulassen. Der Build wird in [GitHub Actions](https://github.com/Robin9094707/CASHBACK-/actions/workflows/android-apk.yml) automatisch erstellt. Die APK liegt zusätzlich als `Jessicas-Cashback-APK` im erfolgreichen Workflow-Lauf.

## Funktionen

- Native Jetpack-Compose-Oberfläche mit Material 3, großen Karten, grünem / fliederfarbenem Design, Dunkelmodus und optionalen Material-You-Systemfarben.
- Live-Aktionen mit Produktbildern, öffentlich angegebenen Bedingungen, Aktionslinks und Quellenherkunft. Suche, Händlerfilter, Gratis-testen-Filter und Sortierung nach Frist oder maximalem Betrag.
- Favoriten, Einkaufsliste, ausgeblendete Aktionen wiederherstellen; eigene Cashback-Aktionen und Coupons hinzufügen.
- Persönlicher Status: Merkliste → Gekauft → Eingereicht → Erstattet / Abgelehnt. Erwartete und bestätigte Beträge getrennt anzeigen.
- Belegfoto lokal speichern, Notiz / Referenznummer und eigene Fristerinnerung festhalten.
- Optionale Alarme für neue Aktionen, nur Gratis testen oder Stichwörter wie `Zahnpasta, Rossmann`. Gespeicherte Fristen regelmäßig prüfen.
- Lokale Filialübersicht für Duisburg mit Radius- und Händlerfilter; freiwillige Freigabe des ungefähren Standorts. Weitere Orte über die Karten-App finden.
- Anbieterübersicht für dm, Rossmann, Lidl, ALDI, Netto, REWE, EDEKA, Kaufland, Müller, Amazon, eBay, Shoop, TopCashback, PAYBACK, Scondoo, Shopmium, SPARWELT und mydealz.
- JSON-Sicherung exportieren und importieren / zusammenführen. Belegfotos sind **nicht** in dieser Sicherung enthalten.

## Live-Daten und Grenzen

Fünf Adapter lesen öffentliche Inhalte von vier Anbietern: SPARWELT-Cashback-Übersicht, DealDoktor-Gratis-Übersicht, DealDoktor-RSS, MonsterDealz-RSS und Kostenlos.de-RSS. Ein erreichbarer Feed muss nicht bei jedem Abruf passende Aktionen enthalten. Produkt-Cashback steht im Mittelpunkt; zusätzlich erscheinen explizite Gratisartikel, Coupon-Hinweise und lokale Berichte. Abos, Kreditangebote, Casino- und VPN-Aktionen werden herausgefiltert. Quellenübergreifend sehr ähnliche Titel werden zusammengeführt; bereits vorhandene SPARWELT-IDs bleiben erhalten.

Vor jedem Quellenabruf wird die öffentliche robots.txt geprüft. Der Abruf verwendet einen identifizierbaren User-Agent, wartet mindestens zwei Sekunden zwischen Anfragen an denselben Host und begrenzt Timeout und Datenmenge. Bei Sperren, nicht prüfbaren Abrufregeln oder anderen Fehlern wird die Quelle übersprungen. Kein Login, keine private API, keine Proxyrotation, keine Captcha-Umgehung. Diese technische Rücksichtnahme ist keine pauschale rechtliche Lizenz für beliebige Weiterverwendung.

Die App zeigt die Herkunft jedes Hinweises. RSS-Berichte führen zur öffentlichen Quelle; SPARWELT-Metadaten können direkt auf den Veranstalter führen. Unbekannte Kontingente und Fristen bleiben unbekannt. Weitere Anbieter in der Übersicht sind direkte Einstiegspunkte, keine synchronisierten Konto-APIs. Persönliche Händler-Coupons müssen weiterhin in der jeweiligen Händler-App aktiviert werden.

`Refresh live cashback` aktualisiert `data/deals.json` ungefähr alle zwei Stunden. GitHub kann geplante Läufe verzögern; bei länger inaktiven öffentlichen Repositories können Zeitpläne deaktiviert werden. Bei einzelnen Quellenfehlern bleiben die anderen Quellen nutzbar und der Quellenstatus zeigt den Fehler. Ein vollständiger Ausfall behält den gespeicherten Feed und lässt den Workflow fehlschlagen. RSS-Einträge älter als 14 Tage und ausdrücklich abgelaufene Hinweise werden verworfen. Der initiale Quellenstand liegt auch als Offline-Startbestand in der APK. Die App zeigt ältere Daten als veraltet an und aktualisiert beim Start und auf Knopfdruck.

Es gibt keine universelle, öffentlich zugängliche Gratis-API für alle weltweit angebotenen Cashbacks. Diese App garantiert weder vollständige Abdeckung noch eine Auszahlung oder freie Kontingente. Händlererwähnungen sind aus den Quellen abgeleitet und keine Zusage zur Teilnahmeberechtigung jedes Geschäfts. Unbekannte Einreichungsfristen bleiben unbekannt. Eine 100-%-Erstattung ist ein Aktionshinweis, keine verbindliche Prüfung durch die App.

Die Teilnahme startet im Browser auf dem Aktionslink; Kauf, Kontoregistrierung, Identifikation und Belegübermittlung erfolgen dort durch die Nutzerin. Der Adapter kann von der Quelle vorgegebene Affiliate-Links enthalten. Die App verdient selbst keine Provision und sammelt keine eigenen Affiliate-IDs.

## Benachrichtigungen

Android WorkManager prüft neue Feed-Einträge etwa alle zwei Stunden, Erinnerungen etwa alle sechs Stunden. Der erste Hintergrundabruf legt einen Ausgangsstand an, statt alle bestehenden Aktionen als neu zu melden. Alarme sind optional, Stichwörter werden mit ODER verknüpft. Energiesparmodus und Androids Hintergrundregeln können Prüfungen verzögern. Es sind keine sekundengenauen Push-Nachrichten oder garantierten Echtzeitalarme. Auf Samsung kann in den App-Einstellungen die Hintergrundausführung erlaubt werden, wenn Erinnerungen stark verzögert sind.

## Datenschutz

Keine Anmeldung, Analyse-SDKs oder Werbe-SDKs. Merkliste, Status, Notizen und kopierte Belegbilder verbleiben im privaten App-Speicher. Netzwerkabrufe gehen an GitHub (öffentlicher Feed), Bildquellen und die bewusst geöffneten Anbieter. Android-Systembackups sind deaktiviert. JSON-Sicherungen enthalten persönliche Notizen; sicher aufbewahren. Keine Passwörter, Bankdaten oder IBAN in Notizen speichern.

## Entwicklung

JDK 17, Android SDK 36, Gradle 8.14.3, Kotlin 2.2.10, Android Gradle Plugin 8.11.1 und Material 3 1.4.0. Die Oberfläche setzt auf Material 3 mit expressiver Gestaltung.

```sh
gradle assembleRelease
pip install -r scripts/requirements.txt
python scripts/refresh_feed.py
```

Der Workflow erzeugt eine installierbare APK mit dem ausschließlich im Build generierten Android-Entwicklungsschlüssel und veröffentlicht nur die APK unter dem Tag `android-latest`. Kein Signierschlüssel wird im Repository oder als Artifact gespeichert. Bei späteren Builds kann der neue Schlüssel eine Neuinstallation erfordern: vorher JSON-Sicherung und Belegfotos sichern. Für dauerhaft aktualisierbare, vertrauenswürdig signierte Veröffentlichungen ist ein privater Schlüssel über GitHub-Secrets / Play App Signing erforderlich.

## Struktur

- `app/.../MainActivity.kt`: Oberfläche und Navigation
- `app/.../Details.kt`: Teilnahmeverwaltung und Anbieterübersicht
- `app/.../Data.kt`: Modelle, privater Speicher, Feed-Laden und Sicherungen
- `app/.../Alerts.kt`: Hintergrundabgleich und lokale Benachrichtigungen
- `scripts/refresh_feed.py` und `scripts/public_sources.py`: öffentliche Quellenadapter und begrenzte Abrufe
- `app/.../Cards.kt`: Produktbilder, Karussell und Karten
- `app/.../Nearby.kt`: lokale Filialübersicht und freiwillige Standortberechnung
- `data/stores-duisburg.json`: OSM-Filialbestand (ODbL 1.0)
- `.github/workflows/android-apk.yml`: APK-Build und direkter Release-Download
- `.github/workflows/refresh-cashback.yml`: kostenlose Feed-Aktualisierung

Validierung: APK-Build in GitHub Actions und fünf gezielte Regressionstests für Feed-Daten (Altersgrenze, Kauf-/Einreichungsfrist, lokale Hinweise, Abo-Filter und Ausfall der Hauptquelle). Kein umfassender Geräte- oder End-to-End-Test; Anbieter-Formulare und tatsächliche Erstattungen werden nicht automatisiert getestet.
