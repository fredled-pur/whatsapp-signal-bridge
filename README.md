# WhatsApp → Signal Bridge App

Een Android app die WhatsApp berichten doorstuurt naar Signal, zodat je ze op een andere telefoon kunt ontvangen zonder dat Meta metadata krijgt van je werk telefoon.

## Features

- 📱 Leest WhatsApp notificaties
- 🔒 Stuurt berichten door via Signal protocol (end-to-end encrypted)
- ⚙️ Configureerbare filters (contacten, spam, stille uren)
- 📊 Statistieken en logs
- 🔋 Draait op de achtergrond

## Vereisten

- Android 8.0 (API 26) of hoger
- GrapheneOS compatible
- Een extra telefoonnummer voor Signal registratie (prepaid SIM)

## Installatie

### Optie 1: Build zelf

1. Clone dit project
2. Open in Android Studio
3. Build de APK:
   ```bash
   ./gradlew assembleRelease
   ```
4. Installeer de APK op je telefoon

### Optie 2: Directe installatie

1. Download de APK van releases
2. Sta installatie van onbekende bronnen toe
3. Installeer de APK

## Setup

### Stap 1: Notificatie toegang

De app vraagt om toegang tot notificaties. Dit is nodig om WhatsApp berichten te kunnen lezen.

### Stap 2: Signal registratie

1. Plaats een SIM kaart in een oude telefoon
2. Voer het telefoonnummer in de app in
3. Ontvang de SMS verificatiecode
4. Voer de code in de app in
5. De SIM is nu geregistreerd en kan in een la

### Stap 3: Bestemming instellen

Voer het Signal nummer in van je werk telefoon waar berichten naartoe moeten worden gestuurd.

### Stap 4: Klaar!

Zet de bridge aan en alle WhatsApp berichten worden doorgestuurd naar Signal op je werk telefoon.

## Architectuur

```
┌─────────────────────────────────────────┐
│  GrapheneOS (privé telefoon)            │
│                                         │
│  WhatsApp                               │
│      ↓ (notificatie)                    │
│  NotificationListenerService            │
│      ↓                                  │
│  Signal Protocol (libsignal)            │
│      ↓ (encrypted)                      │
│  Signal Servers                         │
│                                         │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  Werk telefoon                          │
│                                         │
│  Signal App                             │
│  (ontvangt berichten)                   │
│                                         │
└─────────────────────────────────────────┘
```

## Privacy

- Je werk telefoon heeft geen WhatsApp, dus Meta krijgt geen metadata
- Alle berichten zijn end-to-end encrypted via Signal protocol
- De bridge app slaat alleen lokaal data op (keys, logs)
- Geen cloud sync, geen analytics, geen tracking

## Technische details

### Dependencies

- **libsignal-client**: Signal protocol implementatie
- **Jetpack Compose**: Modern Android UI
- **Room**: Lokale database voor keys en logs
- **DataStore**: App preferences
- **OkHttp/Retrofit**: Network calls naar Signal servers

### Permissies

- `BIND_NOTIFICATION_LISTENER_SERVICE`: Lezen van notificaties
- `INTERNET`: Communicatie met Signal servers
- `FOREGROUND_SERVICE`: Achtergrond service
- `RECEIVE_BOOT_COMPLETED`: Automatisch starten na boot
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Betrouwbaar draaien

## Troubleshooting

### App ontvangt geen berichten

1. Check of notificatie toegang is verleend
2. Check of batterij optimalisatie is uitgeschakeld
3. Check of WhatsApp notificaties aan staan

### Berichten komen niet aan op werk telefoon

1. Check of Signal is geïnstalleerd op werk telefoon
2. Check of het juiste nummer is ingesteld
3. Check internet verbinding

### Registratie mislukt

1. Probeer een ander telefoonnummer
2. Wacht 24 uur en probeer opnieuw
3. Check of het nummer niet al geregistreerd is

## License

MIT License

## Disclaimer

Dit project is voor educatieve doeleinden. Gebruik op eigen risico. De ontwikkelaars zijn niet verantwoordelijk voor misbruik of problemen die ontstaan door het gebruik van deze app.
