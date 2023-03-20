# Dokumentation zum Modul "Datenbankseitige Programmierung"

Andreas Sünder 4BHIT - 09.03.2022

## Functions

> Aufgabe 1: Erweitere die Funktion 'bilanz' wie folgt: Die Bank hat bis 2019 bei jeder Transaktion eine Steuer von 2% eingehoben - d.h. ein Transfer von 100.- soll in der Bilanz des Empfaengers nur mit 98.- gewertet werden. 2020 wurde diese Steuer auf 1% gesenkt. Erstelle eine Funktion bilanz_mit_steuer, die diese beiden Steuersaetze beruecksichtigt. (Hint: mit date_part('year',date) laesst sich das Jahr zu einem Datum ermitteln.) Rufe deine Funktion mit select bilanz(<id>) auf und teste anhand von passenden Daten, ob sie auch funktioniert.