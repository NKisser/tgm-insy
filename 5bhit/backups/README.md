# WS 01 _ Backupstrategien und Backup in der Praxis [GK]

Andreas Sünder 5BHIT - 16.10.2023

## Backupstrategien

### Inkrimentelles Backup

Ein **inkrementelles Backup** speichert immer die Änderung zum vorherigen Backup. Zu Beginn wird ein Vollbackup durchgeführt, anschließend werden nur mehr die Änderungen gespeichert. Um ein inkrementelles Backup wiederherzustellen, müssen *alle* Backups vorhanden sein. Jedoch stellt es ein einfaches Verfahren da, welches wenig Speicherplatz benötigt.

### Differentielles Backup

Ein **differentielles Backup** ist dem inkrementellen sehr ähnlich, jedoch speichert es nicht die Änderungen gegenüber dem vorherigen Backup, sondern gegenüber dem zu Beginn durchgeführten Vollbackup. Um ein differentielles Backup wiederherzustellen, wird nur das Vollbackup und das gewünschte differentielle Backup benötigt. Es ist somit einfacher wiederherzustellen als ein inkrementelles Backup, jedoch benötigt es mehr Speicherplatz, da alle Änderungen bei jedem Backup gespeichert werden.

### Vollbackup

Beim **Vollbackup** werden alle vorhandenen Daten gesichert. Ein vergangener Stand kann somit immer als Ganzes wiederhergestellt werden (es wird somit kein vergangener Stand benötigt). Im Vergleich zu den anderen Strategien ist der Speicherbedarf hier sehr hoch.

## Umsetzung

### Vorbereitung

Alle hier durchgeführten Schritte wurden mit bzw. in einem extra erstellen Docker-Container ausgeführt. Dieser wurde mit folgendem Befehl konfiguriert:

```bash
docker run --name postgres_5bhit -v ${PWD}/my-postgres.conf:/etc/postgresql/postgresql.conf -v ${PWD}/data:/var/lib/postgresql/data -v ${PWD}/archive:/mnt/postgres/archive -v ${PWD}/backups:/mnt/postgres/backups -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres -c "config_file=/etc/postgresql/postgresql.conf"
```

Die hier verknüpften Ordner dienen für den Datenzugriff von außen in den Container. Des Weiteren wurde eine Konfigurationsdatei für den Postgres-Server verknüpft. Hier sind folgende (vom Standard abweichende) Einstellungen ausshchlaggebend:

```conf
wal_level = replica
achive_mode = on
archive_command = 'test ! -f /mnt/postgres/archive/%f && cp %p /mnt/postgres/archive/%f'
restore_command = 'cp /mnt/postgres/archive/%f %p'
```

### SQL Dump

Mit einem *SQL Dump* kann eine Liste von SQL-Befehlen generiert werden, die, wenn sie ausgeführt werden, die Datenbank in den Zustand des Zeitpunkts des Dumps versetzen. Mit Docker kann ein SQL Dump wie folgt erstellt werden:

```bash
# Sichern
pg_dump -U postgres postgres > /mnt/postgres/backups/dump.sql
```

Hier wird die Datenbank `postgres` gesichert. Wiederherstellen lässt sich ein (bzw. dieser) SQL Dump mit:

```bash
# Wiederherstellen
psql -U postgres postgres < /mnt/postgres/backups/dump.sql
```

Auch hier ist es wieder wichtig, den Namen der entsprechenden Datenbank anzugeben (im Container gibt es zu Beginn sowieso nur die). Da mit `pg_dump` nur datenbankweise gesichert werden kann und auch nicht die Rollen mitgesichert werden, kann mit `pg_dumpall` eine Sicherung aller Datenbanken und Rollen erstellt werden:

```bash
# Sichern
pg_dumpall -U postgres > /mnt/postgres/backups/dump_all.sql

# Wiederherstellen
psql -U postgres -f /mnt/postgres/backups/dump.sql postgres
```

### File System Level Backup

Für *File System Level Backup* muss lediglich der Ordner, in dem die Daten der Datennbank liegen, kopiert werden. Dies entspricht in diesem Fall dem einfachen Kopieren des `data/`-Ordners, der mit dem Container verknüpft wurde. Es ist anzumerken, dass für ein ordnungsgemäßes Backup der Server gestoppt werden sollte, da die Datenbank in einem *laufendem Zustand* kopiert wird. Ein sogenanntes *Basebackup* kann auch mit dem `pg_basebackup`-Befehl durchgeführt werden:

```bash
pg_basebackup -U postgres -D /mnt/postgres/backups/bb_1
```

Hier gibt `-D` den Pfad des erzeugten Backups an.

### WAL Archivierung & Point in Time Recovery

Postgres schreibt alle an den Datenbanken durchgeführten Änderungen ständig im sogenannten *Write Ahead Log* (WAL) mit. Dieser Log wird in Segmente aufgeteilt, die normalerweise 16 MB groß sind und *inkrementell* gespeichert werden, wobei die Dateinamen der einzelnen Segmente für die Position in der WAL-Sequenz stehen. Damit kann auch eine *endlose* Sequenz entstehen, was vor allem für größere Datenbanken praktisch ist, da damit ständige Vollbackups vermieden werden können. Des Weiteren ist es möglich, von einem beliebigen Punkt in der WAL-Sequenz wiederherzustellen, was als *Point in Time Recovery* bezeichnet wird. Wichtig ist nur, dass ein Vollbackup (sprich das File System Level Backup) vorhanden ist.

**Wichtig**: `pg_dump[all]` zählt in diesem Fall nicht als Vollbackup, von dem mittels WAL wiederhergestellt werden kann, weil mit diesem Befehl *logische* Backups erstellt werden, keine *physischen*.

Das automatische Archivieren der WAL-Dateien ist standardmäßig nicht aktiviert, kann aber über die `postgresql.conf` aktiviert werden (siehe oben). Nach dem Aktivieren werden die Dateien automatisch archiviert:

```bash
root@991a3ac1d115:/# ls -l /mnt/postgres/archive/
total 98304
-rw------- 1 postgres postgres 16777216 Oct  9 19:23 000000010000000000000001
-rw------- 1 postgres postgres 16777216 Oct  9 19:24 000000010000000000000002
-rw------- 1 postgres postgres 16777216 Oct  9 19:24 000000010000000000000003
-rw------- 1 postgres postgres      338 Oct  9 19:24 000000010000000000000003.00000028.backup
-rw------- 1 postgres postgres 16777216 Oct  9 19:28 000000010000000000000004
-rw------- 1 postgres postgres 16777216 Oct  9 19:40 000000020000000000000005
-rw------- 1 postgres postgres 16777216 Oct 15 18:46 000000020000000000000006
-rw------- 1 postgres postgres       41 Oct  9 19:37 00000002.history
```

Die einzelnen Logs werden dateiweise "aufgefüllt", bis sie (wie vorher erwähnt) etwa 16 MB groß sind. Möchte man manuell weiterspringen, so ist das mit

```sql
SELECT pg_switch_wal();
```

möglich. Soll nun von einem bestimmten Punkt wiederhergestellt werden, so sind folgende Schritte zu tätigen:

1. Stoppen des Servers
2. Löschen des Postgres Data Ordners
3. Kopieren des letzten Vollbackups
4. Anlegen einer leeren `postgresql.conf` im Data Ordner (diese signalisiert Postgres beim nächsten Start, dass von einem Backup wiederhergestellt werden soll)
5. Starten des Servers

Das manuelle Kopieren der archivierten WAL-Dateien ist nicht nötig, da Postgres dies beim Wiederherstellen selbst übernimmt (siehe `restore_command` in der `postgresql.conf`).
