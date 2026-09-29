# Gestione Turni

App Android per la gestione dei turni di lavoro, scritta in Kotlin con Jetpack Compose.

Calendario mensile dei turni (Mattina, Pomeriggio, Notte, Straordinario, Riposo, Ferie) con
inserimento rapido, modifica del singolo turno, conteggio di ore e straordinari, condiviso fra
due persone tramite Supabase.

## Stack

- Jetpack Compose + Material 3
- Navigation 3
- Room (persistenza locale)
- Hilt (dependency injection)
- ViewModel + Coroutines/Flow
- Unit test (JUnit) e UI test strumentati (Compose + Hilt)

## Struttura

```
app/src/main/java/it/simoc/gestioneturni/
├── GestioneTurni.kt              # Application (@HiltAndroidApp)
├── data/
│   ├── TurnoRepository.kt        # interfaccia + implementazione di default
│   ├── di/DataModule.kt          # binding del repository (+ fake per i preview/test)
│   └── local/
│       ├── database/AppDatabase.kt
│       ├── database/Turno.kt     # entity + DAO
│       └── di/DatabaseModule.kt
└── ui/
    ├── MainActivity.kt
    ├── Navigation.kt             # NavDisplay + entry
    ├── NavigationKeys.kt         # NavKey serializzabili
    ├── theme/                    # Color, Theme, Type
    └── turno/
        ├── TurnoScreen.kt        # UI Compose + preview
        └── TurnoViewModel.kt     # uiState (Loading/Success/Error)
```

## Requisiti

- JDK 17
- Android SDK 36 (`compileSdk`/`targetSdk` = 36, `minSdk` = 23)
- Il percorso dell'SDK va in `local.properties` (`sdk.dir=...`), non versionato

## Comandi

```bash
./gradlew assembleDebug        # build APK di debug
./gradlew testDebugUnitTest    # unit test
./gradlew connectedCheck       # UI test (serve un device/emulatore)
./gradlew installDebug         # installa su device/emulatore collegato
```

## Sincronizzazione con Supabase

L'app funziona offline (Room) e sincronizza con Supabase: le modifiche vengono messe in coda
e inviate con WorkManager appena c'è rete; quelle dell'altro telefono arrivano con Realtime.
In caso di conflitto vince la modifica più recente.

Configurazione (una volta sola):

1. Su supabase.com → SQL Editor → esegui `supabase/schema.sql`.
2. Authentication → Sign In / Providers → Email: disattiva **Confirm email** (l'app non gestisce
   la conferma via email: dopo la registrazione si entra subito).
3. In `local.properties` (non versionato) aggiungi:
   ```
   SUPABASE_URL=https://<progetto>.supabase.co
   SUPABASE_ANON_KEY=<chiave anon o publishable>
   ```
   Mai la chiave `service_role`/`secret`: finirebbe dentro l'APK.
4. Nell'app: la prima persona crea l'account e il calendario, poi da *Menu → Invita* condivide
   il codice; la seconda crea il suo account e inserisce il codice.
