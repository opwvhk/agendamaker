Agendamaker
===========

Dit is een alleenstaand programma om agenda's mee te maken.

Gebruiksaanwijzing:
1. Installeer Java versie 21 of nieuwer
2. Download het programma
3. Gebruik het programma

De eerste stappen zijn eenmalig, de laatste niet.


Installeer Java versie 21 of nieuwer
------------------------------------

Er zijn verschillende Java-versies. Een goede keuze is de gratis Temurin-versie.

Er zijn verschillende manieren om deze te installeren, bijvoorbeeld via een zogeheten "package
manager", zoals Homebrew voor de Mac. Geen idee wat dat betekent? Geen nood! We installeren het
gewoon los:

Stappen om Java Temurin te installeren:
1. Ga naar de release-pagina van de website:
   [Java Temurin releases](https://adoptium.net/temurin/releases/)
2. Je vindt het installatieprogramma door naar beneden te scrollen
3. Download het installatieprogramma dat het beste past bij jouw computer
4. Start het installatieprogramma; dit installeert Java


Download het programma
----------------------

Download het `.jar`-bestand van de laatste release
* Je vindt ze aan de rechterkant, onder "Releases"
* Op de pagina kies je voor `agendamaker-0.7.1-full.jar`
* Zet het bestand op een leuke plek neer

Voor Windows ben je klaar; voor Mac nog niet:
* Download ook het bestand `Unquarantine.zip`
* Pak dit bestand uit; Je ziet nu een robotje met iets zwarts in zijn hand, en de naam "Unquarantine.app"
* Sleep het bestand `agendamaker-0.7.1-full.jar` over het robotje


Gebruik het programma
---------------------

1. Start het programma (dubbel klikken)
2. Lees de aanwijzingen op het scherm
3. Maak de gewenste aanpassingen aan de invoer
4. Druk op "Maak PDF"

Als je op een van de knoppen rechts onderin het scherm drukt, slaat het programma alle instellingen
op als voorkeursinstellingen. Deze worden weer getoond wanneer je het programma een volgende keer
opstart.

Agenda opnemen in een planner met andere pagina's
-------------------------------------------------

De agenda die gegenereerd wordt gebruikt de volgende instellingen:

* Paginaformaat: **A4**
* Marges: **20 mm** aan elke kant
* Lettertype: **PT Sans**
* Lettergrootte: **10pt**
* Regelafstand: 125%

In de broncode is te zien dat je ook een complete agenda kunt genereren, inclusief dagindeling,
notitiepagina's, mindmap-pagina's en verschillende extra vaste pagina's (allemaal in een vaste
volgorde). Dit vereist kennis van programmeren, want dat kan uitsluitend via de
methode [opwvhk.planner.PlannerGenerator#main()](https://github.com/opwvhk/agendamaker/blob/main/src/main/java/opwvhk/planner/PlannerGenerator.java#L92).
