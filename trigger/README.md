# Dokumentation zum Modul "Datenbankseitige Programmierung"

Andreas Sünder 4BHIT - 09.03.2022

## Functions

> **Aufgabe 1:** Erweitere die Funktion 'bilanz' wie folgt: Die Bank hat bis 2019 bei jeder Transaktion eine Steuer von 2% eingehoben - d.h. ein Transfer von 100.- soll in der Bilanz des Empfaengers nur mit 98.- gewertet werden. 2020 wurde diese Steuer auf 1% gesenkt. Erstelle eine Funktion bilanz_mit_steuer, die diese beiden Steuersaetze beruecksichtigt. (Hint: mit date_part('year',date) laesst sich das Jahr zu einem Datum ermitteln.) Rufe deine Funktion mit select bilanz(<id>) auf und teste anhand von passenden Daten, ob sie auch funktioniert.

Die kann man folgendermaßen lösen:

```sql
CREATE OR REPLACE FUNCTION bilanz_mit_steuer(client_id INTEGER)
    RETURNS INTEGER
AS
$$
DECLARE
    einzahlungen DECIMAL;
    auszahlungen DECIMAL;
    einzahlungen2 DECIMAL;
    auszahlungen2 DECIMAL;
BEGIN
    SELECT SUM(amount) * 0.98 INTO einzahlungen FROM transfers WHERE to_client_id = client_id AND date < '2020-01-01';
    SELECT SUM(amount) * 0.98 INTO auszahlungen FROM transfers WHERE from_client_id = client_id AND date < '2020-01-01';

    SELECT SUM(amount) * 0.99 INTO einzahlungen2 FROM transfers WHERE to_client_id = client_id AND date >= '2020-01-01';
    SELECT SUM(amount) * 0.99 INTO auszahlungen2 FROM transfers WHERE from_client_id = client_id AND date >= '2020-01-01';

    RETURN (einzahlungen + einzahlungen2) - (auszahlungen + auszahlungen2);
END;
$$ LANGUAGE plpgsql;
```
Die beiden Summen werden für die Jahre vor und nach 2020 berechnet und dann zusammengezählt, da die Steuersätze unterschiedlich sind.

## Procedures

> **Aufgabe 2:** Passe die Procedure transfer_direct() so an, dass die Ueberweisung nicht durchgefuehrt wird, falls das Konto nicht gedeckt ist, d.h., wenn am Konto des Senders weniger Geld vorhanden ist, als ueberwiesen werden soll. (Hint 1: mit zum Beispiel IF a < b THEN ... END IF; lassen sich Fallunterscheidungen durchfuehren.) (Hint 2: mit RAISE EXCEPTION 'meine Fehlermeldung' laesst sich die Procedure abbrechen.)

```sql
CREATE OR REPLACE PROCEDURE transfer_direct(sender INTEGER, recipient INTEGER, howmuch DECIMAL)
AS
$$
DECLARE
    sender_balance DECIMAL;
BEGIN
    SELECT amount INTO sender_balance FROM accounts WHERE client_id = sender;

    IF sender_balance < howmuch THEN
        RAISE EXCEPTION 'Der Sender hat zu wenig Geld!';
    END IF;

    UPDATE accounts SET amount = amount - howmuch WHERE client_id = sender;
    UPDATE accounts SET amount = amount + howmuch WHERE client_id = recipient;
    COMMIT;
END;
$$ LANGUAGE plpgsql;
```

Bevor die Transaktion durchgeführt wird, wird der Kontostand des Senders ausgelesen und mit dem Betrag der Überweisung verglichen. Wenn der Kontostand kleiner ist, wird eine Exception geworfen.

## Triggers

> **Aufgabe 3:** Erstelle einen Trigger, welcher bewirkt, dass bei dem Anlegen eines neuen Transfers der aktuelle Kontostand in der accounts-Tabelle automatisch angepasst wird. Erstelle dafuer eine Function update_accounts(), die die noetigen Anpassungen durchfuehrt und mache diese Funktion mittels CREATE TRIGGER new_transfer BEFORE INSERT ON transfers FOR EACH ROW EXECUTE PROCEDURE update_accounts(); zum Trigger. (Hint: Damit der Transfer auch gespeichert wird, muss dein Trigger RETURN NEW zurueckgeben);

> **Aufgabe 4:** Passe den Trigger aus Aufgabe 3 so an, dass nur Transfers angenommen werden, die das Senderkonto nicht ueberziehen. D.h. ueberpruefe, ob das Konto nach der Ueberweisung unter 0.- fallen wuerde und verhindere in diesem Fall mit RETURN NULL das Einfuegen der Transaktion

```sql
CREATE OR REPLACE FUNCTION update_accounts()
RETURNS TRIGGER
AS $$
DECLARE
    sender_balance DECIMAL;
BEGIN
    SELECT amount INTO sender_balance FROM accounts WHERE client_id = NEW.from_client_id;

    IF sender_balance < NEW.amount THEN
        RAISE EXCEPTION 'Der Sender hat zu wenig Geld!';
    END IF;

    UPDATE accounts SET amount = amount - NEW.amount WHERE client_id = NEW.from_client_id;
    UPDATE accounts SET amount = amount + NEW.amount WHERE client_id = NEW.to_client_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER new_transfer BEFORE INSERT ON transfers FOR EACH ROW EXECUTE PROCEDURE update_accounts();
```

Um die Kontostände vergleichen bzw. aktualisieren zu können, wird auf den Datensatz `NEW` zugegriffen. Dieser beinhaltet die Daten, die in die Tabelle eingefügt werden sollen. Der Trigger wird vor dem Einfügen der Daten ausgeführt und kann die Daten mit `RETURN NEW` zurückgeben, um sie einzufügen. 
Hat der Sender zu wenig Geld, wird hier ebenfalls eine Exception geworfen.