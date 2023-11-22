# WS 01 _ Backupstrategien und Backup in der Praxis [GK]

Andreas Sünder 5BHIT - 13.11.2023

## Setup

Für diese Aufgabe wurde ein eigener Docker-Container mit folgendem Befehl erstellt:

```bash
docker run --name postgres-5bhit-bv -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres -v ${PWD}:/mnt/host
```

... wobei `${PWD}` das Verzeichnis mit der Datei `dvdrental.tar` ist. Im Container muss dann nur zu `/mnt/host` navigiert werden und das Backup kann eingespielt werden:

```bash
pg_restore -U postgres -d postgres --format=t --clean dvdrental.tar
```

## Rollen & Berechtigungen

Rolle können mit `CREATE ROLE` angelegt werden. Standardmäßig erhält eine Rolle die Berechtigungen `NOSUPERUSER`, weshalb für den Administrator die Option `SUPERUSER` gesetzt werden muss. Mit `LOGIN` kann die Rolle sich anmelden (dies muss für alle Rollen angewendet werden). Einzelne Berechtigungen können mit `GRANT` oder mit `REVOKE` (auch auf Spaltenebene, siehe Script für die Kunden) wieder entzogen werden.

```sql
-- Administrator
CREATE ROLE administrator WITH SUPERUSER LOGIN;
GRANT USAGE ON SCHEMA public TO administrator;

-- Mitarbeiter
CREATE ROLE mitarbeiter LOGIN;
GRANT SELECT, INSERT ON TABLE payment TO mitarbeiter;

-- Redakteur
CREATE ROLE redakteur LOGIN;

-- Kunde
CREATE ROLE kunde LOGIN;
REVOKE SELECT (replacement_cost) ON TABLE film FROM kunde;
```

## Benutzer

Einzelne Benutzer werden mit `CREATE USER` erstellt. Diese können dann mit `GRANT` einer Rolle zugewiesen werden:

```sql
CREATE USER Mitarbeiter1 LOGIN;
GRANT Mitarbeiter1 TO mitarbeiter;

CREATE USER Redakteur1 LOGIN;
GRANT Redakteur1 TO redakteur;
```

## Berechtigungen über die Datei pg_hba.conf

Wenn alle Verbindungen (außer jene, die über `localhost` etc. auf die DB zugreifen wollen) von außen ohne SSL verboten werden und nur jene mit SSL akzeptiert werden sollen, müssen nur eine Zeile in der Datei `pg_hba.conf` angepasst werden:

```
hostssl all             all             all                     trust
hostnossl all           all             0.0.0.0/0               reject
```

Das lehnt alle Verbindungen ohne SSL ab und akzeptiert nur jene mit SSL.

## View und Policy

Die Mitarbeiter haben grundsätzlich keine Berechtigungen für die Tabelle `customer`, jedoch können sie eine Berechtigung (ausschließlich) auf eine spezielle View erhalten, die nur jene Kunden anzeigt, die aktiv sind:

```sql
CREATE POLICY email ON customer TO mitarbeiter USING (active=1);
CREATE VIEW showCostumers AS SELECT * FROM customer;
```

Zusätzlich muss die View der Rolle `mitarbeiter` zugewiesen werden:

```sql
GRANT SELECT ON showCostumers TO mitarbeiter;
```
