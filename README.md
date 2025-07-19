# Edle Eisbecher - RoboRally Spiel

## 🎮 Spielbeschreibung

**Edle Eisbecher** ist eine JavaFX-Implementierung des klassischen Brettspiels **RoboRally**. Das Spiel simuliert ein futuristisches Rennen, bei dem Spieler ihre Roboter durch programmierte Befehle über ein gefährliches Spielfeld steuern müssen.

### 🎯 Spielziel
Das Ziel ist es, als erster Spieler alle Checkpoints auf der Karte **in der richtigen Reihenfolge** zu erreichen. Dabei müssen Sie Ihren Roboter durch verschiedene Hindernisse, Laser, Förderbänder und andere Gefahren navigieren. 

**Wichtige Regeln:**
- Roboter starten mit 5 Energie
- Maximal 10 Energie möglich
- Bei 10 Schadenskarten muss der Roboter neu starten
- Roboter können sich gegenseitig blockieren
- Checkpoints müssen in numerischer Reihenfolge erreicht werden

## 🚀 Installation und Start

### Voraussetzungen
- Java 24 oder höher
- Maven 3.6 oder höher

### Installation
```bash
# Repository klonen
git clone [repository-url]
cd "Edle Eisbecher"

# Dependencies installieren
mvn clean install
```

### Spiel starten

#### Server starten
```bash
# Server auf Port 12345 starten 
java -jar target/ee-1.0-SNAPSHOT.jar --server
```

#### Client starten
```bash
# Client starten
java -jar target/ee-1.0-SNAPSHOT.jar
```

## Spielmechaniken

### Spielphasen
Das Spiel läuft in mehreren Phasen ab:

1. **Setup-Phase**: Spieler wählen ihre Roboter-Figur und Position
2. **Programmier-Phase**: Spieler bekommen 9 Programmierkarten und ordnen 5 davon im eigenen Register für die nächste Runde
3. **Aktivierungs-Phase**: Die gewählten Karten werden nacheinander ausgeführt (Register 1-5)
4. **Cleanup-Phase**: Karten werden aufgenommen, Schadenskarten werden verarbeitet

### Kartenarten

#### Programmierkarten
- **Bewegungskarten**: Move I (1 Feld), Move II (2 Felder), Move III (3 Felder), BackUp (1 Feld rückwärts)
- **Drehkarten**: TurnLeft (90° links), TurnRight (90° rechts), UTurn (180°)
- **Spezialkarten**: Again (wiederholt vorherige Karte), PowerUp (Energie +1)

#### Schadenskarten
- **Spam**: Wird in ein Register gesteckt und blockiert die Programmierung
- **Virus**: Verbreitet sich auf alle Roboter in Reichweite (6 Felder)
- **Wurm**: Zwingt den Roboter zum Neustart (zurück zum Startpunkt)
- **Trojaner**: Fügt zwei Spam-Karten zu

### Spielfeldelemente

#### Gefährliche Elemente
- **Laser**: Fügen Schaden zu
- **Pits**: Lassen Roboter abstürzen
- **Wände**: Blockieren Bewegung

#### Mechanische Elemente
- **Förderbänder**: 
  - **Grüne Förderbänder**: Bewegen Roboter 1 Feld in Richtung Pfeil
  - **Blaue Förderbänder**: Bewegen Roboter 2 Felder in Richtung Pfeil
- **Zahnräder**: 
  - **Grüne Zahnräder**: Drehen Roboter 90° im Uhrzeigersinn
  - **Rote Zahnräder**: Drehen Roboter 90° gegen den Uhrzeigersinn
- **Druckplatten**: Bewegen Roboter basierend auf aktueller Register-Nummer

#### Hilfreiche Elemente
- **Energie-Felder**: Geben Energie zurück (max. 10 Energie)
- **Checkpoints**: Müssen in Reihenfolge erreicht werden
- **Antenne**: Bestimmt Spielerreihenfolge bei Gleichständen
- **Neustart-Punkte**: Roboter landen hier nach Absturz

### Verfügbare Karten
- **Death Trap**: Gefährliche Karte mit vielen Hindernissen und Pits
- **Dizzy Highway**: Standard-Karte mit vielen Förderbändern
- **Extra Crispy**: Karte mit vielen Lasern und Feuer-Elementen
- **Lost Bearings**: Komplexe Karte mit vielen Zahnrädern

## Testanleitung für Tester

### Grundlegende Tests

#### 1. Verbindungstest
- [ ] Server startet ohne Fehler
- [ ] Client kann sich mit Server verbinden
- [ ] Mehrere Clients können gleichzeitig verbunden sein

#### 2. Lobby-Test
- [ ] Spieler können Namen eingeben
- [ ] Spieler können Roboter-Figur auswählen
- [ ] Spieler können "Bereit" Status setzen
- [ ] Chat-Funktion funktioniert

#### 3. Spielstart-Test
- [ ] Spiel startet wenn alle Spieler bereit sind
- [ ] Kartenauswahl funktioniert (9 Karten ziehen, 5 programmieren)
- [ ] Spieler erhalten ihre Handkarten
- [ ] Energie wird korrekt zugewiesen (5 Energie zu Beginn)

#### 4. Spielmechanik-Test
- [ ] Programmierkarten werden korrekt ausgeführt (Register 1-5)
- [ ] Bewegung funktioniert in alle Richtungen
- [ ] Kollisionen werden korrekt behandelt (Roboter blockieren sich)
- [ ] Schadenskarten werden angewendet
- [ ] Energie-System funktioniert (max. 10 Energie)

### Erweiterte Tests

#### 5. Spielfeldelemente-Test
- [ ] Laser fügen Schaden zu (1 Schadenskarte pro Laser)
- [ ] Förderbänder bewegen Roboter (grün: 1 Feld, blau: 2 Felder)
- [ ] Zahnräder drehen Roboter (grün: im Uhrzeigersinn, rot: gegen Uhrzeigersinn)
- [ ] Pits lassen Roboter abstürzen (zurück zum Neustart-Punkt)
- [ ] Checkpoints werden korrekt erkannt (müssen in Reihenfolge erreicht werden)
- [ ] Energie-Felder geben Energie zurück

#### 6. Netzwerk-Test
- [ ] Spiel funktioniert über Netzwerk
- [ ] Verbindungsabbrüche werden behandelt
- [ ] Nachrichten werden korrekt übertragen

#### 7. UI-Test
- [ ] Spielfeld wird korrekt angezeigt
- [ ] Karten werden visuell dargestellt
- [ ] Animationen funktionieren
- [ ] Chat-Interface ist benutzerfreundlich

### Fehlerszenarien testen

#### 8. Fehlerbehandlung
- [ ] Ungültige Kartenauswahl wird abgelehnt
- [ ] Netzwerkfehler werden behandelt
- [ ] Spieler-Austritt wird korrekt verarbeitet
- [ ] Server-Neustart funktioniert



##  Logging

Das Spiel verwendet Log4j2 für Logging. Logs werden in folgenden Dateien gespeichert:
- `logs/heartbeat-YYYY-MM-DD.log.gz`: Netzwerk-Herzschlag-Logs
- `logs/`: Weitere Debug- und Fehler-Logs

##  Entwicklung

### Projektstruktur
```
src/main/java/de/lmu/dbs/ifi/sep25/
├── card/           # Karten-Logik
├── game/           # Spielmechaniken
├── network/        # Netzwerk-Kommunikation
├── ui/             # Benutzeroberfläche
└── utils/          # Hilfsfunktionen
```

### Technologien
- **JavaFX**: Benutzeroberfläche
- **Maven**: Build-System
- **Log4j2**: Logging
- **Gson**: JSON-Serialisierung
- **JUnit**: Unit-Tests



---

**Viel Spaß beim Testen! 🎮**
