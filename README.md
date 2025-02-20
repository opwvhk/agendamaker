# Agendamaker

Dit is een alleenstaand programma om agenda's mee te maken.

Gebruiksaanwijzing:
1. Installeer Java versie 23 of nieuwer
2. Bouw of download het `.jar`-bestand
3. Dubbelklik het `.jar`-bestand, of voer het uit via de "command line":
    ```shell
    java -jar agendamaker.jar
    ```
4. Lees de aanwijzingen op het scherm
5. Maak de gewenste aanpassingen aan de invoer
6. Druk op "Maak PDF"

Als je op een van de knoppen rechts onderin het scherm drukt, slaat het programma alle instellingen
op als voorkeursinstellingen. Deze worden weer getoond wanneer je het programma een volgende keer
opstart.

## Agenda opnemen in een planner met andere pagina's

De agenda die gegenereerd wordt gebruikt de volgende instellingen:

* Paginaformaat: **A4**
* Marges: **20 mm** aan elke kant
* Lettertype: **Helvetica**  
  (een van de standaardlettertypes van PDF-bestanden)
* Lettergrootte: **11pt**
* Regelafstand: 133%

In de broncode is te zien dat je ook een complete agenda kunt genereren, inclusief dagindeling,
notitiepagina's, mindmap-pagina's en verschillende extra vaste pagina's (allemaal in een vaste
volgorde). Dit vereist kennis van programmeren, want dat kan uitsluitend via de
methode [opwvhk.planner.PlannerGenerator#main()](https://github.com/opwvhk/agendamaker/blob/main/src/main/java/opwvhk/planner/PlannerGenerator.java#L92).
