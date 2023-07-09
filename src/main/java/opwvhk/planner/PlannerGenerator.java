package opwvhk.planner;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.function.Consumer;

import static com.itextpdf.kernel.geom.PageSize.A4;
import static com.itextpdf.layout.properties.TextAlignment.CENTER;
import static com.itextpdf.layout.properties.TextAlignment.RIGHT;
import static com.itextpdf.layout.properties.UnitValue.createPercentArray;
import static com.itextpdf.layout.properties.VerticalAlignment.BOTTOM;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static opwvhk.planner.WritableDocument.mmToPt;

/**
 * Class to generate a PDF file with one or more calendar weeks.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe — Kind</a>
 */
public class PlannerGenerator {
	private static final float PHI = 1.618033988749f;

	public static void main(String[] args) throws IOException {
		try (OutputStream output = new FileOutputStream("planner.pdf")) {
			PlannerDescription plannerDescription = new PlannerDescription("Planagenda", "2023 – 2024", 2, 3, 3,
					java.util.List.of(
							new DateTitle(LocalDate.of(2023, Month.SEPTEMBER, 4), ""),
							new DateTitle(LocalDate.of(2023, Month.OCTOBER, 2), "Projectweek 1"),
							new DateTitle(LocalDate.of(2023, Month.OCTOBER, 7), ""),
							new DateTitle(LocalDate.of(2023, Month.OCTOBER, 23), "Herfstvakantie"),
							new DateTitle(LocalDate.of(2023, Month.OCTOBER, 30), ""),
							new DateTitle(LocalDate.of(2023, Month.DECEMBER, 25), "Kerstvakantie"),
							new DateTitle(LocalDate.of(2024, Month.JANUARY, 8), ""),
							new DateTitle(LocalDate.of(2024, Month.FEBRUARY, 19), "Voorjaarsvakantie"),
							new DateTitle(LocalDate.of(2024, Month.FEBRUARY, 26), ""),
							new DateTitle(LocalDate.of(2024, Month.APRIL, 1), "2e Paasdag"),
							new DateTitle(LocalDate.of(2024, Month.APRIL, 2), "Projectweek 2"),
							new DateTitle(LocalDate.of(2024, Month.APRIL, 7), ""),
							new DateTitle(LocalDate.of(2024, Month.APRIL, 22), "Meivakantie"),
							new DateTitle(LocalDate.of(2024, Month.MAY, 6), ""),
							new DateTitle(LocalDate.of(2024, Month.MAY, 9), "Hemelvaart"),
							new DateTitle(LocalDate.of(2024, Month.MAY, 10), "dag na Hemelvaart (vrij)"),
							new DateTitle(LocalDate.of(2024, Month.MAY, 11), ""),
							new DateTitle(LocalDate.of(2024, Month.MAY, 20), "2e Pinksterdag"),
							new DateTitle(LocalDate.of(2024, Month.MAY, 21), ""),
							new DateTitle(LocalDate.of(2024, Month.JULY, 1), "Toetsweek"),
							new DateTitle(LocalDate.of(2024, Month.JULY, 6), ""),
							new DateTitle(LocalDate.of(2024, Month.JULY, 20), "Zomervakantie")
					)
			);
			new PlannerGenerator(plannerDescription).generate(output);
		}
	}

	/**
	 * Logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PlannerGenerator.class.getName());

	private static final Locale LOCALE = Locale.forLanguageTag("nl-NL");
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm", LOCALE);
	private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM", LOCALE);
	private static final DateTimeFormatter WEEK_NR_FORMATTER = DateTimeFormatter.ofPattern("w", LOCALE);
	private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE: d", LOCALE);
	/**
	 * The description of the planner to generate.
	 */
	private final PlannerDescription plannerDescription;

	/**
	 * Create a planner generator.
	 *
	 * @param plannerDescription a description of the planner to generate
	 */
	public PlannerGenerator(PlannerDescription plannerDescription) {
		java.util.List<DateTitle> dateTitles = new ArrayList<>(plannerDescription.dateTitles());

		NavigableMap<LocalDate, String> sortedDateTitles = plannerDescription.sortedDateTitles();
		LocalDate startDate = sortedDateTitles.firstKey().with(TemporalAdjusters.previousOrSame(MONDAY));
		if (!sortedDateTitles.containsKey(startDate)) {
			dateTitles.add(new DateTitle(startDate, sortedDateTitles.firstEntry().getValue()));
		}
		LocalDate endDate = sortedDateTitles.lastKey().with(TemporalAdjusters.nextOrSame(SUNDAY));

		/*
		 * The pages:
		 * * Note the number of (static) pages before the timetables
		 * * Note the number of (static) after the timetables and before the planner
		 */
		int staticPagesBeforePlanner = 18;
		/*
		 * The total must be printable as a booklet, and must thus be a multiple of 4. Also, the week planing pages must start on an even numbered page to
		 * ensure pages open with a full week in view. To achieve this, the number of timetable and mindmap pages is increased as necessary.
		 */
		int numberOfWeeks = (int) WEEKS.between(startDate, endDate.plusDays(1));
		int pagesBeforePlanner = staticPagesBeforePlanner + plannerDescription.timeTablePages();
		// The planner pages must start on a left-hand page for each week, so the number of preceding pages must be odd.
		int extraTimetablePages = 1 - pagesBeforePlanner % 2;
		int pagesToEnd = pagesBeforePlanner + extraTimetablePages + 2 * numberOfWeeks +
				plannerDescription.notesPages() + plannerDescription.mindmapPages() + 1;
		// The result must be printable as a booklet, which means a fourfold number of pages. Calculate how many are missing (0-3).
		int extraMindmapPages = 3 - (pagesToEnd - 1) % 4;

		// Increase the number of mindmap pages to ensure the total number of pages is a multiple of four.
		// Also overwrite the start and end dated with their corrected versions: start on the last Monday on or before the earliest date, and end on the first
		// Sunday on or after the last date.
		this.plannerDescription = new PlannerDescription(plannerDescription.title(), plannerDescription.subtitle(),
				plannerDescription.timeTablePages() + extraTimetablePages, plannerDescription.notesPages(),
				plannerDescription.mindmapPages() + extraMindmapPages, dateTitles);
	}

	/**
	 * Generate a week planner into an {@link OutputStream}.
	 *
	 * @param output the stream to write to
	 * @throws IOException  when the planner cannot be written
	 * @throws PdfException when the planner cannot be generated
	 */
	public void generate(OutputStream output) throws IOException, PdfException {
		float topBottomMargin = mmToPt(20);
		float margin = mmToPt(20);
		try (WritableDocument document = new WritableDocument(A4, margin, margin, topBottomMargin, output)) {
			generate(document);
		}
	}

	/**
	 * Generate a week planner into a {@link PdfDocument}.
	 *
	 * @param document the document to write to
	 * @throws IOException  when the planner cannot be written
	 * @throws PdfException when the planner cannot be generated
	 */
	public void generate(WritableDocument document) throws IOException, PdfException {
		//document.startNewPage(true);
		addTitlePage(document, plannerDescription.title(), plannerDescription.subtitle()); // 1 page
		addEmergencyPlan(document); // 1 page

		addClassSchedulesAndVacations(document); // 1 page
		addHowToSurviveTheFreshmanYear(document); // 2 pages

		addPlanningHand(document); // 1 page
		addHowToSurviveLearning(document); // 10 pages
		addStudyingTips(document); // 1 page
		//addPrerequisitesForLearning(document); // 2 pages
		addTimeSpentTables(document, plannerDescription.timeTablePages());

		addPlanningInstructions(document); // 1 page
		addPlanningWeeks(document, plannerDescription.sortedDateTitles());

		addNotesPages(document, plannerDescription.notesPages());
		addMindMapPages(document, plannerDescription.mindmapPages());

		// Blank back page
		document.startNewPage(true);
		/* Preferably not: adolescents tend to try and find out anything they can (great), but may pull pranks (not good)
		Paragraph closingRemarks = document.createParagraph(PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC), 10f)
				//.setBorder(new SolidBorder(ColorConstants.RED, 1))
				.setTextAlignment(RIGHT)
				.add("Gemaakt naar ontwerp van de Huizermaat\n")
				.add("met software van Oscar Westra van Holthe - Kind");
		Rectangle pageArea = document.getEffectiveArea();
		// Paragraphs are rendered full-width, and objects are anchored to the bottom-left corner.
		// This also means that the bottom-left page corner has coordinates (0,0).
		document.addInFlow(closingRemarks.setFixedPosition(pageArea.getLeft(), pageArea.getBottom(), pageArea.getWidth()));
		*/
	}

	private void addTitlePage(WritableDocument document, String title, String subtitle) {
		LOGGER.debug("Adding title page");
		Rectangle pageArea = document.getEffectiveArea();
		float pageWidth = pageArea.getWidth();

		ImageData titleImageData = ImageDataFactory.create(requireNonNull(getClass().getResource("/Planner.png")));
		float titleScaledWidth = pageWidth * 0.75f;
		float titleScaledHeight = titleScaledWidth * titleImageData.getHeight() / titleImageData.getWidth();
		Image titleImage = new Image(titleImageData).setWidth(titleScaledWidth).setHeight(titleScaledHeight);

		ImageData logoImageData = ImageDataFactory.create(requireNonNull(getClass().getResource("/logo-huizermaat.png")));
		float logoWidth = pageWidth * 0.4f;
		Image logoImage = new Image(logoImageData).setWidth(logoWidth).setHeight(logoWidth * logoImageData.getHeight() / logoImageData.getWidth());

		document.addInFlow(document.createParagraph(null, 48).add("\n" + title).setMarginBottom(0).setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph(null, 32).add(subtitle + "\n\n").setMarginBottom(0).setTextAlignment(CENTER));
		document.addInFlow(titleImage.setHorizontalAlignment(HorizontalAlignment.CENTER));
		document.addInFlow(document.createParagraph().add("\n\n\n\n\n\n\n\n\n\nDeze planner is van:\n\n"));
		document.addInFlow(document.createParagraph().addTabStops(new TabStop(pageWidth * 0.75f, TabAlignment.LEFT, new SolidLine(.75f)))
				.add(new Tab()).add(" "));
		document.addInFlow(logoImage.setFixedPosition(pageArea.getRight() - logoWidth, pageArea.getBottom(), logoWidth));
	}

	@SuppressWarnings("unused")
	private static Text size(float size, String text) {
		return new Text(text).setFontSize(size);
	}

	private Text bold(String text) {
		return new Text(text).setBold();
	}

	private Text italic(String text) {
		return new Text(text).setItalic();
	}

	private Text underline(String text) {
		return new Text(text).setUnderline();
	}

	private void addClassSchedulesAndVacations(WritableDocument document) {
		LOGGER.debug("Adding class times and important dates");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Lestijden en vakanties\n\u00A0").setFontSize(16)).setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph()
				.add(italic("Lestijden klas 1\n\u00A0").setFontSize(14)).setTextAlignment(CENTER));

		UnitValue[] columnWidths = createPercentArray(new float[]{50, 50});
		Table table = new Table(columnWidths).setAutoLayout().setHorizontalAlignment(HorizontalAlignment.CENTER)
				.setPadding(0).setMargin(0).setMarginBottom(16);
		for (String cellText : java.util.List.of(
				"Normaal rooster", "Verkort rooster",
				"1. 08:15-09:15", "1. 08:15- 08:55",
				"2. 09:15-10:15", "2. 08:55- 09:35",
				"pauze", "3. 09:35-10:15",
				"3. 10:30-11:30", "pauze",
				"4. 11:30-12:30", "4. 10:35-11:15",
				"pauze", "5. 11:15-11:55",
				"5. 13:00-14:00", "6. 11:55-12:35",
				"6. 14:00-15:00", "7. 12:35-13:15"
		)) {
			table.addCell(createCell(document, 1, cellText).setPadding(mmToPt(2)));
		}
		document.addInFlow(table);

		document.addInFlow(document.createParagraph()
				.add(bold("\nVakanties en lesvrije dagen:\n").setFontSize(16)).setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph()
				.add(italic("2023 – 2024\n\n").setFontSize(14)).setTextAlignment(CENTER));

		table = new Table(createPercentArray(new float[]{4.75f, 9.25f})).setAutoLayout()//.setFixedLayout()
				.setHorizontalAlignment(HorizontalAlignment.CENTER)
				.setPadding(0).setMargin(0).setMarginBottom(16);
		for (String cellText : java.util.List.of(
				"Herfstvakantie", "Maandag 23 oktober 2023 t/m vrijdag 27 oktober 2023",
				"Kerstvakantie", "Maandag 25 december 2023 t/m vrijdag 5 januari 2024",
				"Voorjaarsvakantie", "Maandag 19 februari 2024 t/m vrijdag 23 februari 2024",
				"2e Paasdag", "Maandag 1 april 2024",
				"Meivakantie", "Maandag 22 april 2024 t/m vrijdag 3 mei 2024",
				"Hemelvaart", "Donderdag 9 mei en vrijdag 10 mei 2024",
				"2e Pinksterdag", "Maandag 20 mei 2024",
				"Zomervakantie", "Maandag 22 juli 2024 t/m zondag 1 september 2024"
		)) {
			table.addCell(createCell(document, 1, cellText).setPadding(mmToPt(2)));
		}
		document.addInFlow(table);
	}

	private void addEmergencyPlan(WritableDocument document) {
		LOGGER.debug("Adding emergency plan");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Noodplan\n\n").setFontSize(16))
				.setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph()
				.add(italic("(Plak hier de rode kaart die je van de conciërge krijgt)").setFontSize(14))
				.setTextAlignment(CENTER));
	}

	private void addPrerequisitesForLearning(WritableDocument document) {
		LOGGER.debug("Adding prerequisites for learning");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("\nVoorwaarden voor leren").setFontSize(16)));
		document.addInFlow(document.createParagraph()
				.add("\n")
				.add("""
						Voor je ligt de planagenda. In deze agenda kun je voor jezelf overzicht creëren in wat wanneer af moet
						zijn, maar ook wanneer je het af gaat maken. Zo ben je goed georganiseerd!
						""".replace("\n", " ")));
		document.addInFlow(document.createParagraph()
				.add("""
						Voordat je aan de slag kunt gaan met het plannen, staan hieronder nog een aantal voorwaarden voor het leren opgesteld.
						Deze voorwaarden zijn belangrijk om in je achterhoofd te houden tijdens het plannen, lees ze daarom maar goed door.
						""".replace("\n", " ").strip())
				.add("\n\n"));

		float indent = mmToPt(10);
		document.addInFlow(document.createParagraph()
				.add(bold("Laat je niet afleiden")));
		document.addInFlow(document.createParagraph()
				.setMarginLeft(indent)
				.add("Zorg dat alles wat je kan afleiden (denk aan telefoon, computer, te veel tabbladen open) niet bij jou in de buurt is.")
				.add("\n\u00A0"));
		document.addInFlow(document.createParagraph()
				.add(bold("Zoek een rustige plaats")));
		document.addInFlow(document.createParagraph()
				.setMarginLeft(indent).add("Waar je je op je gemak voelt én je kan concentreren.")
				.add("\n\u00A0"));
		document.addInFlow(document.createParagraph()
				.add(bold("Houd je doel voor ogen")));
		document.addInFlow(document.createParagraph()
				.setMarginLeft(indent)
				.add("Bijvoorbeeld een cijfer dat je wilt halen; een onvoldoende wegwerken; een bepaalde studie die je wilt gaan doen.")
				.add("\n\u00A0"));
		document.addInFlow(document.createParagraph()
				.add(bold("Neem er de tijd voor")));
		document.addInFlow(document.createParagraph()
				.setMarginLeft(indent)
				.add("Als je tijdsdruk ervaart en/of niet ontspannen bent, neem je minder informatie op.")
				.add("\n\u00A0"));
		document.addInFlow(document.createParagraph()
				.add(bold("Maak een planning")));
		document.addInFlow(document.createParagraph()
				.setMarginLeft(indent)
				.add("Kijk goed naar wat je moet leren en bedenk hoeveel tijd je daarvoor nodig hebt. ")
				.add("Een dag voor de toets niks nieuws leren, maar ").add(underline("alles herhalen")).add(".")
				.add("\n\u00A0"));
		document.addInFlow(document.createParagraph()
				.add(bold("Bedenk hoe je de informatie het beste kunt leren")));
		document.addInFlow(document.createParagraph()
				.setMarginLeft(indent).add("Wat is voor jou de beste aanpak die aansluit bij hoe jij graag leert?")
				.add("\n\u00A0"));

		document.addInFlow(document.createParagraph().add(bold("\nHERHALEN, HERHALEN, HERHALEN")));
		document.addInFlow(document.createParagraph()
				.add("""
						Je hebt het vast al héél vaak gehoord, maar herhaling van de stof die je moet leren is het allerbelangrijkste.
						Wanneer je veel aandacht aan iets geeft, worden er verbindingen aangelegd in je hersenen, waardoor je
						er steeds beter in wordt! In de eerste 20 minuten na het leren, kan je al zo’n 40% vergeten. Dat is
						bijna de helft! Herhalen zorgt ervoor dat je minder vergeet. Wanneer je dus veel aandacht aan iets
						geeft door het te herhalen, word je er én""".replace("\n", " ") + " ")
				.add(italic("beter"))
				.add(" in én ")
				.add(italic("onthoudt"))
				.add(" je het ")
				.add(italic("beter"))
				.add("! Snap je nu waarom het zo belangrijk is?")
		);
		document.addInFlow(document.createParagraph()
				.add("Daarnaast is er één regel: ")
				.add(underline("de dag vóór de toets mag je "))
				.add(underline("géén").setBold())
				.add(underline(" nieuwe informatie meer leren. Op deze dag mag je alleen nog maar herhalen"))
				.add("! Zorg er dus voor dat je op tijd klaar bent met leren.")
		);

		document.addInFlow(document.createParagraph()
				.add(bold("\n\nEn dan kun je nu aan de slag, succes!")).setTextAlignment(CENTER));
	}

	private void addPlanningInstructions(WritableDocument document) throws IOException {
		LOGGER.debug("Adding planning instructions");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Hoe overleef ik het plannen?").setFontSize(16))
				.add("\n\n"));
		document.addInFlow(document.createParagraph()
				.add("""
						In deze agenda kun je voor jezelf overzicht creëren in wat wanneer af moet zijn,
						maar ook wanneer je het af gaat maken. Zo ben je goed georganiseerd!
						""".replace("\n", " ")));
		document.addInFlow(document.createParagraph()
				.add("""
						Voordat je aan de slag kunt gaan met het plannen, staan hieronder nog een aantal stappen om je agenda in te vullen.
						Deze stappen zijn belangrijk om in je achterhoofd te houden tijdens het plannen, lees ze daarom maar goed door.
						""".replace("\n", " ").strip())
				.add("\n\n"));

		document.addInFlow(document.createParagraph()
				.add(bold("\nStappenplan voor het maken van een planning:").setFontSize(16))
				.add("\n\n"));

		Image checkedCircleImage = document.loadSvgImageResource("/circle-checked.svg").setWidth(15).setHeight(15);

		List list = document.createList().setListSymbol(ListNumberingType.DECIMAL);
		list.setPostSymbolText(") ");
		list.add("Noteer toetsen en huiswerk in de planagenda.");
		list.add("Geef toetsen en inlevermomenten een kleurtje (rood of roze).");
		list.add("Huiswerk dat af is kleur je groen.");
		ListItem listItem = new ListItem();
		listItem.add(document.createParagraph().add("De taken die af zijn, vink je af met een check ").add(checkedCircleImage).add("."));
		list.add(listItem);
		list.add("Bekijk SOM een paar weken vooruit. Zo weet je of je binnenkort een toets hebt.");
		list.add("Houd rekening met afspraken buiten school. Op dagen met veel afspraken kun je minder huiswerk maken.");
		list.add("Plan je huiswerk en toetsen in. Hak het in kleine stukjes.");
		list.add("Weet je niet wat het huiswerk of leerwerk is? Check de studiewijzer!");
		list.add("Dan ga je aan de slag! Nummer de taken. Begin bij de belangrijkste taak.");
		list.add("Heb je het aan het eind van de dag nog niet alles af gekregen? Plan deze taken opnieuw in.");
		document.addInFlow(list);
	}

	private void addPlanningHand(WritableDocument document) throws IOException {
		LOGGER.debug("Adding instructions to plan by 'the hand'");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("\nDe hand-vragen:").setFontSize(16))
				.add("\n\n")
				.add("\n\n")
				.add("\n\n")
				.add("\n\n")
				.add("\n\n")
		);

		Rectangle pageArea = document.getEffectiveArea();
		Image handImage = document.loadSvgImageResource("/hand.svg");
		handImage.scaleToFit(pageArea.getWidth() / PHI, pageArea.getHeight() / PHI);
		// centered on the page, but then moved down 20mm
		float x = pageArea.getX() + (pageArea.getWidth() - handImage.getImageScaledWidth()) / 2f;
		float y = pageArea.getY() + (pageArea.getHeight() - handImage.getImageScaledHeight()) / 2f - mmToPt(10);
		handImage.setFixedPosition(x, y);
		document.addInFlow(handImage);

		document.addInFlow(document.createParagraph(14)
				.add("Wat moet ik doen?")
				.setFixedPosition(mmToPt(40), mmToPt(143), mmToPt(50)));
		document.addInFlow(document.createParagraph(14)
				.add("Waarom moet ik dat doen? Wat kan ik ervan leren?")
				.setFixedPosition(mmToPt(30), mmToPt(180), mmToPt(60)));
		document.addInFlow(document.createParagraph(14)
				.add("Wanneer moet het af zijn?")
				.setFixedPosition(mmToPt(100), mmToPt(195), mmToPt(40)));
		document.addInFlow(document.createParagraph(14)
				.add("Wat heb ik nodig?")
				.setFixedPosition(mmToPt(130), mmToPt(184), mmToPt(50)));
		document.addInFlow(document.createParagraph(14)
				.add("Wanneer ben ik klaar? Wanneer ben ik tevreden?")
				.setFixedPosition(mmToPt(145), mmToPt(162), mmToPt(60)));
	}

	private void addHowToSurviveLearning(WritableDocument document) {
		LOGGER.debug("Adding subject pages for planning");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("\nHoe overleef ik leren?").setFontSize(16))
				.add("\n\n"));

		List subjects = document.createList().setListSymbol("• ");
		subjects.setProperty(Property.LEADING, new Leading(Leading.MULTIPLIED, 2.5f));
		subjects.add("Planning");
		subjects.add("Organisatie");
		subjects.add("Mindmap");
		subjects.add("Samenvatting");
		subjects.add("Woordjes");
		subjects.add("Leerstrategieën");
		subjects.add("Concentratie/mindset");
		subjects.add("Toets voorbereiden");
		subjects.add("Toets maken");
		document.addInFlow(subjects);

		for (int i = 0; i < 9; i++) {
			document.startNewPage(false);
			document.addInFlow(document.createParagraph().add(" "));
		}
	}

	private void addStudyingTips(WritableDocument document) {
		LOGGER.debug("Adding studying tips");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Studietips\n\u00A0").setFontSize(16)).setTextAlignment(CENTER));

		float pageWidth = document.getEffectiveArea().getWidth();
		Paragraph blockWithLines = document.createParagraph(null, 14)
				.addTabStops(new TabStop(pageWidth, TabAlignment.LEFT, new SolidLine(.75f)))
				.add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n")
				.add("\n");

		document.addInFlow(document.createParagraph().add(bold("Tips voor het leren:").setFontSize(14)));

		document.addInFlow(document.createParagraph()
				.add("Hoe zorg je ervoor dat je niet wordt afgeleid tijdens het maken van je huiswerk of het leren van toetsen?")
		).addInFlow(blockWithLines);
		document.addInFlow(document.createParagraph()
				.add("Hoe kom je erachter wat je precies moet maken of leren?")
		).addInFlow(blockWithLines);
		document.addInFlow(document.createParagraph()
				.add("Wat is voor jou een fijne plek in het huis om je huiswerk te maken of te leren?")
		).addInFlow(blockWithLines);
		document.addInFlow(document.createParagraph()
				.add("Wat is voor jou een fijn moment op de dag om je huiswerk te maken?")
		).addInFlow(blockWithLines);
		document.addInFlow(document.createParagraph()
				.add("Wat is voor jou een fijn moment om je tas in te pakken?")
		).addInFlow(blockWithLines);
	}

	private void addHowToSurviveTheFreshmanYear(WritableDocument document) {
		LOGGER.debug("Adding guide to survive as a freshman");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Hoe overleef ik de brugklas\n\u00A0").setFontSize(16)).setTextAlignment(CENTER));

		// First heading: do not start with a newline (for the rest: do)
		document.addInFlow(document.createParagraph().add(bold("Wat moet ik doen als ik te laat komt?")));
		document.addInFlow(document.createParagraph().add("""
				Als je te laat op school bent of te laat voor een les, haal je altijd eerst een telaatbriefje bij de conciërges. Met zo'n
				briefje mag jij de les in, of je nu geoorloofd te laat was of niet.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nWat moet ik doen als ik naar de orthodontist/dokter moet?")));
		document.addInFlow(document.createParagraph().add("""
				Je ouders moeten voor de afspraak aan school laten weten welk lesuur je er niet bent.
				Dat kan door te bellen. Of ze geven je een briefje voor de conciërges mee met daarin waarom je welk lesuur niet aanwezig kunt zijn.
				Dat briefje moet je dan voor de afspraak aan een van de conciërges geven.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nWat moet ik doen als ik ziek ben?")));
		document.addInFlow(document.createParagraph()
				.add("Als je ziek bent, dan bellen je ouders voor het eerste lesuur naar school om je ziek te melden.\n\u00A0"));

		document.addInFlow(document.createParagraph().add(bold("Wat moet ik doen als ik ziek naar huis wil?")));
		document.addInFlow(document.createParagraph().add("""
				Wanneer je je tijdens lestijd opeens niet lekker voelt, dan ga je naar je docent en geef je aan dat je naar huis wilt,
				je meld je daarna af bij de conciërges.
				Als je thuis bent, laat je je ouders naar school bellen dat je weer veilig thuis bent.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nWat moet ik doen als ik een toets gemist heb?")));
		document.addInFlow(document.createParagraph().add("""
				Je geeft bij je vakdocent aan dat je een toets gemist hebt en vraagt de vakdocent wanneer je de toets kunt inhalen.
				Je vakdocent zorgt dat er een inhaaltoets voor je klaar ligt in de mediatheek.
				Elke dinsdagmiddag tussen 14:15-15:15 is er een moment om toetsen in te halen.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nNeem ik mijn jas en gymtas mee naar het lokaal?")));
		document.addInFlow(document.createParagraph().add("""
				Op Huizermaat heeft elke leerling een eigen kluisje. Hierin kun je je gymtas, je jas en je telefoon bewaren.
				Tijdens de gymles bewaar je je chromebook in je kluisje.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nZijn alle gymlessen in de gymzalen van Huizermaat?")));
		document.addInFlow(document.createParagraph().add("""
				Nee, vanaf april tot aan de herfstvakantie gymmen we buiten op de sportvelden en de atletiekbaan,
				op twee minuten van de school.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nGebruikt elke leerling een chromebook?")));
		document.addInFlow(document.createParagraph().add("""
				Ja, op Huizermaat gebruiken we een chromebook ter ondersteuning van het onderwijs; we gebruiken dus ook boeken.
				Een chromebook lijkt op een laptop.
				Vanaf de herfstvakantie zullen we ze in de lessen gaan gebruiken en moet je dus altijd een opgeladen chromebook bij je hebben.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nMag je je telefoon gebruiken tijdens de les?")));
		document.addInFlow(document.createParagraph().add("""
				Nee, tijdens de lessen bewaar je je telefoon, terwijl die uit is, in je tas of in je kluis.
				Een telefoon leidt immers veel te veel af van wat er tijdens de les gebeurt.
				Als je internet nodig hebt voor een schoolopdracht, gebruik je je chromebook.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nWaarom geven we huiswerk?")));
		document.addInFlow(document.createParagraph().add("""
				We geven huiswerk omdat je dan nog eens rustig kunt oefenen met de stof die in de les is behandeld.
				Of je maakt juist huiswerk om je voor te bereiden op de les die komen gaat.
				We bouwen het rustig op. Na de herfstvakantie moet je rekenen op één à anderhalf uur per dag.
				Als je goed meedoet tijdens de les, scheelt dat in wat je thuis moet doen. Op school kun je bovendien de docent of
				medeleerlingen om hulp vragen. Het is vooral handig om leerwerk rustig thuis te doen.
				Ook tijdens het weekend moet je vaak iets voor school doen.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nWat zijn de afspraken in de leszone?")));
		document.addInFlow(document.createParagraph().add("""
				Zodra je door de klapdeuren een leszone in loopt, gelden de volgende afspraken:
				""".replace("\n", " ")));
		List learningZoneRules = document.createList().setListSymbol("- ");
		learningZoneRules.add("Je praat zachtjes en loopt rustig");
		learningZoneRules.add("Je hebt geen jas aan (deze ligt al in je kluis)");
		learningZoneRules.add("Je hebt je telefoon niet bij je (deze ligt al in je kluis)");
		learningZoneRules.add("Eten en drinken is niet toegestaan in de leszone (wel daarbuiten)");
		document.addInFlow(learningZoneRules);
	}

	@SuppressWarnings("SameParameterValue")
	private void addTimeSpentTables(WritableDocument document, int numberOfPages) {
		LOGGER.debug("Adding {} time spent tables", numberOfPages);
		for (int i = 0; i < numberOfPages; i++) {
			addTimeSpentTable(document);
		}
	}

	private void addTimeSpentTable(WritableDocument document) {
		LOGGER.debug("Adding time spent table");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Tijdschema:\n").setFontSize(16)));
		document.addInFlow(document.createParagraph()
				.add("Hoe is jouw week gevuld met school, hobby’s en sporten? Vul dit hieronder in. Geef elk tijdsblok een ander kleurtje."));

		UnitValue[] columnWidths = createPercentArray(8);
		Table table = new Table(columnWidths).useAllAvailableWidth().setFixedLayout().setPadding(0).setMargin(0).setMarginBottom(16);
		LocalTime startOfDay = LocalTime.of(8, 0); // Must be a longer duration after midnight than the minutes we add in the loop
		for (LocalTime time = startOfDay; !time.isBefore(startOfDay); time = time.plusMinutes(30)) {
			table.addCell(new Cell().add(document.createParagraph().add(time.format(TIME_FORMAT)).setMultipliedLeading(1.15f))
					.setTextAlignment(RIGHT).setPaddings(3, 6, -3, 0));
			for (int c = 1; c < columnWidths.length; c++) {
				table.addCell(emptyCell(document));
			}
		}
		document.addInFlow(table);
	}

	private void addPlanningWeeks(WritableDocument document, NavigableMap<LocalDate, String> dateTitles)
			throws IOException {
		LOGGER.debug("Adding planning weeks");

		Image emptyCircleImage = document.loadSvgImageResource("/circle-empty.svg").setWidth(15).setHeight(15);
		UnitValue[] columnWidths = createPercentArray(3);
		LocalDate lastDateThatMustBePresent = dateTitles.lastKey();
		for (LocalDate monday = dateTitles.firstKey(); !monday.isAfter(lastDateThatMustBePresent); monday = monday.plusWeeks(1)) {
			int numClassSlots = 7; // This is also used for planning slots on Saturday
			int numPlanningSlots = 6;
			int extraPadding = 9;
			LOGGER.debug("Adding planning week starting on {}", monday);

			// Left page: Monday to Wednesday

			String month = MONTH_FORMATTER.format(monday);
			if (monday.getMonth() != monday.plusDays(6).getMonth()) {
				month += " / " + MONTH_FORMATTER.format(monday.plusDays(6));
			}

			document.startNewPage(false);
			float pageWidth = document.getEffectiveArea().getWidth();
			float columnWidth = pageWidth / 3 - 0;
			Paragraph header = document.createParagraph()
					.addTabStops(new TabStop(pageWidth, TabAlignment.RIGHT))
					.add(bold("Maand: " + month))
					.add(new Tab())
					.add(bold("Weeknr.: " + WEEK_NR_FORMATTER.format(monday)))
					.add("\n");
			document.addInFlow(header);

			Table table = new Table(columnWidths).useAllAvailableWidth().setPadding(0).setMargin(0).setMarginBottom(16).setFixedLayout();
			for (int c = 0; c < 3; c++) {
				// Monday to Wednesday
				table.addCell(createDateCellWithText(document, dateTitles, columnWidth, monday.plusDays(c)));
			}
			for (int r = 0; r < numClassSlots; r++) {
				table.addCell(createCell(document, 1, (r + 1) + "\n\n\u00A0").setPaddingBottom(extraPadding));
				table.addCell(createCell(document, 1, (r + 1) + "\n\n\u00A0").setPaddingBottom(extraPadding));
				table.addCell(createCell(document, 1, (r + 1) + "\n\n\u00A0").setPaddingBottom(extraPadding));
			}
			for (int c = 0; c < 3; c++) {
				table.addCell(createCell(document, 1, "Planning"));
			}
			for (int r = 0; r < numPlanningSlots * 3; r++) {
				table.addCell(createCell(document, 1, RIGHT, p -> p.add("\n\n\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
			}
			document.addInFlow(table);

			// Right page: Thursday to Sunday

			document.startNewPage(false);
			//document.addInFlow(document.createParagraph().add("\n"));

			table = new Table(columnWidths).useAllAvailableWidth().setPadding(0).setMargin(0).setMarginBottom(16).setFixedLayout();
			for (int c = 0; c < 3; c++) {
				// Thursday to Saturday
				table.addCell(createDateCellWithText(document, dateTitles, columnWidth, monday.plusDays(c + 3)));
			}
			for (int r = 0; r < numClassSlots; r++) {
				table.addCell(createCell(document, 2, (r + 1) + "\n\n\u00A0").setPaddingBottom(extraPadding));
				table.addCell(createCell(document, 2, (r + 1) + "\n\n\u00A0").setPaddingBottom(extraPadding));
				if (r == 0) {
					table.addCell(createCell(document, 1, "Planning"));
					table.addCell(createCell(document, 1, RIGHT, p -> p.add("\n\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				} else {
					table.addCell(createCell(document, 2, RIGHT, p -> p.add("\n\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				}
			}
			table.addCell(createCell(document, 1, "Planning"));
			table.addCell(createCell(document, 1, "Planning"));
			table.addCell(createDateCellWithText(document, dateTitles, columnWidth, monday.plusDays(6)));
			for (int r = 0; r < numPlanningSlots; r++) {
				table.addCell(createCell(document, 2, RIGHT, p -> p.add("\n\n\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				table.addCell(createCell(document, 2, RIGHT, p -> p.add("\n\n\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				if (r == 0) {
					table.addCell(createCell(document, 1, "Planning"));
					table.addCell(createCell(document, 1, RIGHT, p -> p.add("\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				} else {
					table.addCell(createCell(document, 2, RIGHT, p -> p.add("\n\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				}
			}
			document.addInFlow(table);
		}
	}

	private Cell createDateCellWithText(WritableDocument document, NavigableMap<LocalDate, String> dateTitles, float columnWidth, LocalDate date) {
		String dayText = Optional.ofNullable(dateTitles.floorEntry(date).getValue()).filter(s -> !s.isEmpty()).orElse("\u00A0");
		return createCell(document, 1, TextAlignment.LEFT, p -> p.add(date.format(DAY_FORMAT) + "\n").add(text(document, columnWidth, "…", dayText)));
	}

	private Text text(WritableDocument document, float width, @SuppressWarnings("SameParameterValue") String truncatedTextSuffix, String textToFit) {
		if (document.getTextWidth(textToFit) <= width) {
			return new Text(textToFit);
		}
		String text = textToFit + truncatedTextSuffix;
		int reduction = truncatedTextSuffix.length() + 1;
		while (document.getTextWidth(text) > width) {
			text = text.substring(0, text.length() - reduction) + truncatedTextSuffix;
		}
		return new Text(text);
	}

	private Cell emptyCell(WritableDocument document) {
		return createCell(document, 1, null);
	}

	private Cell createCell(WritableDocument document, int rowspan, String text) {
		return createCell(document, rowspan, TextAlignment.LEFT, paragraph -> paragraph.add(requireNonNullElse(text, "\u00A0")));
	}

	private Cell createCell(WritableDocument document, int rowspan, TextAlignment alignment, Consumer<Paragraph> paragraphConsumer) {
		Paragraph paragraph = document.createParagraph(10.5f).setMargin(0).setMultipliedLeading(1);
		paragraphConsumer.accept(paragraph);
		return new Cell(rowspan, 1).setTextAlignment(requireNonNull(alignment)).add(paragraph);
	}

	private void addNotesPages(WritableDocument document, int numberOfNotesPages) {
		LOGGER.debug("Adding {} notes pages", numberOfNotesPages);
		float pageWidth = document.getEffectiveArea().getWidth();
		Paragraph blockWithLines = document.createParagraph(null, 14)
				.addTabStops(new TabStop(pageWidth, TabAlignment.LEFT, new SolidLine(.75f)))
				.add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n")
				.add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n").add("\n");
		for (int i = 0; i < numberOfNotesPages; i++) {
			document.startNewPage(false);
			String header = i == 0 ? "\nRUIMTE VOOR AANTEKENINGEN\n\n\n" : "\n\n\n\n";
			document.addInFlow(document.createParagraph().add(header).setTextAlignment(CENTER));
			document.addInFlow(blockWithLines).addInFlow(blockWithLines).addInFlow(blockWithLines).addInFlow(blockWithLines);
		}
	}

	private void addMindMapPages(WritableDocument document, int numberOfMindMapPages) {
		LOGGER.debug("Adding {} mind map pages", numberOfMindMapPages);
		for (int i = 0; i < numberOfMindMapPages; i++) {
			document.startNewPage(false);
			String header = i == 0 ? "\nRUIMTE VOOR MINDMAPS\n\n\n" : "\n\n\n\n";
			document.addInFlow(document.createParagraph().add(header).setTextAlignment(CENTER));
			document.draw((canvas, area) -> {
				canvas.setLineWidth(0.5f).setColor(ColorConstants.BLACK, true)
						//.rectangle(pageArea.applyMargins(2, 2, 2, 2, false))
						.rectangle(area)
						.stroke();
				//canvas.
			});
		}
	}
}
