# Jessicas Cashback 🌿

Native, deutschsprachige Android-App für Produkt-Cashback, Gratis-testen-Aktionen und eine persönliche Coupon-Verwaltung. Für Samsung und andere Android-Handys ab Android 8. Kein Konto, kein Abo, kein kostenpflichtiger Backend-Dienst.

## APK installieren

[**Jessicas-Cashback.apk herunterladen**](https://github.com/Robin9094707/CASHBACK-/releases/download/android-latest/Jessicas-Cashback.apk)

APK auf dem Handy öffnen und Androids Installationserlaubnis für die verwendete Download-App erteilen. Optional Benachrichtigungen zulassen. Der Build wird in [GitHub Actions](https://github.com/Robin9094707/CASHBACK-/actions/workflows/android-apk.yml) automatisch erstellt. Die APK liegt zusätzlich als `Jessicas-Cashback-APK` im erfolgreichen Workflow-Lauf.

## Funktionen

- Native Jetpack-Compose-Oberfläche mit Material 3, großen Karten, grünem / fliederfarbenem Design, Dunkelmodus und optionalen Material-You-Systemfarben.
- Live-Aktionen mit Produktbildern, öffentlich angegebenen Bedingungen, Aktionslinks und Quellenherkunft. Suche, Händlerfilter, Gratis-testen-Filter und Sortierung nach Frist oder maximalem Betrag.
- Favoriten, Einkaufsliste, ausgeblendete Aktionen wiederherstellen; eigene Cashback-Aktionen und Coupons hinzufügen.
- Persönlicher Status: Merkliste → Gekauft → Eingereicht → Erstattet / Abgelehnt. Erwartete und bestätigte Beträge getrennt anzeigen.
- Belegfoto lokal speichern, Notiz / Referenznummer und eigene Fristerinnerung festhalten.
- Optionale Alarme für neue Aktionen, nur Gratis testen oder Stichwörter wie `Zahnpasta, Rossmann`. Gespeicherte Fristen regelmäßig prüfen.
- Händler in der Nähe über die Karten-App finden. Keine Standortberechtigung oder Standortübermittlung durch diese App; die Karten-App verwaltet ihre eigene Standortfreigabe.
- Anbieterübersicht für dm, Rossmann, Lidl, ALDI, Netto, REWE, EDEKA, Kaufland, Müller, Amazon, eBay, Shoop, TopCashback, PAYBACK, Scondoo, Shopmium, SPARWELT und mydealz.
- JSON-Sicherung exportieren und importieren / zusammenführen. Belegfotos sind **nicht** in dieser Sicherung enthalten.

## Live-Daten und Grenzen

Der aktuell implementierte automatische Adapter liest die öffentlich sichtbare SPARWELT-Cashback-Übersicht ohne Anmeldung, private API oder Umgehung einer Zugriffssperre. Er extrahiert sichtbare Aktionsmetadaten und verweist für verbindliche Angaben auf den Veranstalter. Andere Anbieter sind direkte Einstiegspunkte, **keine automatisch synchronisierten Cashback-APIs**. Coupons können selbst erfasst werden; aktuelle persönliche Händler-Coupons werden in den jeweiligen Händler-Apps aktiviert.

`Refresh live cashback` aktualisiert `data/deals.json` ungefähr alle zwei Stunden. GitHub kann geplante Läufe verzögern; bei länger inaktiven öffentlichen Repositories können Zeitpläne deaktiviert werden. Bei einem Quellenfehler bleibt der letzte Feed bestehen, erhält einen sichtbaren Fehlerstatus und der Workflow meldet den Fehler. Der initiale Quellenstand liegt auch als Offline-Startbestand in der APK. Die App zeigt ältere Daten als veraltet an und aktualisiert beim Start und auf Knopfdruck.

Es gibt keine universelle, öffentlich zugängliche Gratis-API für alle weltweit angebotenen Cashbacks. Diese App garantiert weder vollständige Abdeckung noch eine Auszahlung oder freie Kontingente. Händlererwähnungen sind aus den Quellen abgeleitet und keine Zusage zur Teilnahmeberechtigung jedes Geschäfts. Unbekannte Einreichungsfristen bleiben unbekannt. Eine 100-%-Erstattung ist ein Aktionshinweis, keine verbindliche Prüfung durch die App.

Die Teilnahme startet im Browser auf dem Aktionslink; Kauf, Kontoregistrierung, Identifikation und Belegübermittlung erfolgen dort durch die Nutzerin. Der Adapter kann von der Quelle vorgegebene Affiliate-Links enthalten. Die App verdient selbst keine Provision und sammelt keine eigenen Affiliate-IDs.

## Benachrichtigungen

Android WorkManager prüft neue Feed-Einträge etwa alle zwei Stunden, Erinnerungen etwa alle sechs Stunden. Der erste Hintergrundabruf legt einen Ausgangsstand an, statt alle bestehenden Aktionen als neu zu melden. Alarme sind optional, Stichwörter werden mit ODER verknüpft. Energiesparmodus und Androids Hintergrundregeln können Prüfungen verzögern. Es sind keine sekundengenauen Push-Nachrichten oder garantierten Echtzeitalarme. Auf Samsung kann in den App-Einstellungen die Hintergrundausführung erlaubt werden, wenn Erinnerungen stark verzögert sind.

## Datenschutz

Keine Anmeldung, Analyse-SDKs oder Werbe-SDKs. Merkliste, Status, Notizen und kopierte Belegbilder verbleiben im privaten App-Speicher. Netzwerkabrufe gehen an GitHub (öffentlicher Feed), Bildquellen und die bewusst geöffneten Anbieter. Android-Systembackups sind deaktiviert. JSON-Sicherungen enthalten persönliche Notizen; sicher aufbewahren. Keine Passwörter, Bankdaten oder IBAN in Notizen speichern.

## Entwicklung

JDK 17, Android SDK 36, Gradle 8.14.3, Kotlin 2.2.10, Android Gradle Plugin 8.11.1 und Material 3 1.4.0. Die neueren Expressive-APIs waren beim Aufbau noch Alpha; die Oberfläche setzt auf die stabile Material-3-Bibliothek mit expressiver Gestaltung.

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
- `scripts/refresh_feed.py`: öffentlicher Feed-Adapter mit sichtbarem Fehlerstatus
- `.github/workflows/android-apk.yml`: APK-Build und direkter Release-Download
- `.github/workflows/refresh-cashback.yml`: kostenlose Feed-Aktualisierung

Validierung: APK-Build in GitHub Actions. Kein umfassender Geräte- oder End-to-End-Test; Anbieter-Formulare und tatsächliche Erstattungen werden nicht automatisiert getestet.
