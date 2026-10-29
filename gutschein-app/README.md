# Gutschein-Safe 🎁

Eine Android-App für alle deine Gutscheine: per Foto speichern, Ablaufdatum eintragen und rechtzeitig erinnert werden, bevor etwas verfällt.

## Funktionen (Version 1)

- **Gutscheine erfassen:** Foto von Vorder- und Rückseite (Kamera oder Galerie) plus manuelle Eingabe:
  - wo einlösbar und wofür, Kategorie (Wellness, Supermarkt, Restaurant, Hotel …)
  - Wert und Restguthaben
  - gültig bis (mit Schnellwahl für die gesetzliche **3-Jahres-Frist** zum 31.12.)
  - Code und PIN, Ort/Adresse, Website, „Geschenk von“, Bedingungen und Notizen
- **Übersicht:** verfügbares Guthaben, Anzahl der aktiven Gutscheine, Warnung bei Gutscheinen, die diese Woche ablaufen, Filter (Aktiv / Bald fällig / Favoriten / Archiv), Kategorien und Suche.
- **Erinnerungen:** standardmäßig 30 Tage, 7 Tage und 1 Tag vorher sowie am letzten Tag. Tage und Uhrzeit sind einstellbar.
- **Ideen:** saisonale Vorschläge (z. B. Herbst → Wellness, Kino; Frühling → Mode, Garten), passend zu deinen eigenen Gutscheinen. Dazu besondere Anlässe wie Valentinstag, Muttertag und das Jahresende. Einmal im Monat kommt eine Benachrichtigung mit einem Vorschlag.
- **An der Kasse:** Vollbild-Foto mit maximaler Helligkeit, damit sich Barcodes gut scannen lassen. Codes lassen sich mit einem Tipp kopieren.
- **Einlösen:** als eingelöst markieren oder teilweise einlösen (Restguthaben wird angepasst).
- **Planen:** Termin mit einem Tipp in den Kalender eintragen, Ort in der Karten-App öffnen, Gutschein teilen.
- **Sicherung:** alle Gutscheine samt Fotos als ZIP sichern und wiederherstellen (z. B. beim Handywechsel).
- Kein Konto, keine Cloud, keine Werbung: Alle Daten bleiben auf dem Handy.

## Installation auf dem Handy

### Variante A: Download von GitHub (am einfachsten)
1. Auf GitHub im Repository unter **Actions → „Gutschein-Safe APK“** den neuesten grünen Build öffnen.
2. Unten bei **Artifacts** auf `Gutschein-Safe-APK` tippen. Das lädt eine ZIP-Datei herunter, darin liegt die `.apk`.
3. Die APK auf dem Handy öffnen. Beim ersten Mal fragt Android, ob Installationen aus „unbekannten Quellen“ erlaubt sind (für Browser oder Dateimanager) → erlauben.

### Variante B: Per USB-Kabel
1. Auf dem Handy die **Entwickleroptionen** aktivieren (Einstellungen → Über das Telefon → 7× auf „Build-Nummer“ tippen) und **USB-Debugging** einschalten.
2. Am Computer die [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) installieren.
3. Handy per USB verbinden und installieren:
   ```
   adb install -r Gutschein-Safe-build-XX.apk
   ```
   Ohne Entwickleroptionen geht es auch so: die APK per USB in den Download-Ordner des Handys kopieren und dort antippen.

### Updates
Eine neue APK installiert sich als Update über die alte, solange sie mit demselben Schlüssel signiert ist. Deine Gutscheine bleiben dabei erhalten. Für einen dauerhaft festen Schlüssel kann man im Repository diese Secrets hinterlegen (Settings → Secrets and variables → Actions):

| Secret | Inhalt |
|---|---|
| `GUTSCHEIN_KEYSTORE_BASE64` | Keystore-Datei, base64-kodiert (`base64 -w0 release.jks`) |
| `GUTSCHEIN_STORE_PASSWORD` | Passwort des Keystores |
| `GUTSCHEIN_KEY_ALIAS` | Alias des Schlüssels |
| `GUTSCHEIN_KEY_PASSWORD` | Passwort des Schlüssels |

Ohne diese Secrets nutzt der Build einen zwischengespeicherten Debug-Schlüssel. Das funktioniert auch, wird aber nach längerer Pause (mehr als 7 Tage ohne Build) neu erzeugt. Vor einer Neuinstallation also am besten in der App unter **Einstellungen → Sichern** eine Sicherung machen.

## Selbst bauen

Voraussetzung: JDK 17 und Android SDK (z. B. über Android Studio).

```
cd gutschein-app
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

Oder in Android Studio den Ordner `gutschein-app` öffnen und mit angeschlossenem Handy auf ▶ drücken.

## Technik

Kotlin · Jetpack Compose (Material 3) · Room (Datenbank) · WorkManager (tägliche Erinnerungen) · Coil (Bilder). Mindestens Android 8.0.

## Ideen für spätere Versionen

- Text und Ablaufdatum automatisch aus dem Foto erkennen (OCR, z. B. ML Kit)
- Barcode/QR-Code scannen und als scanbaren Code anzeigen
- Startbildschirm-Widget „Läuft bald ab“
- Ortsbasierte Erinnerung („Du bist gerade bei REWE – du hast hier einen Gutschein!“)
- Gutscheine mit dem Partner teilen und synchronisieren
