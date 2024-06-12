package opwvhk.planner;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Link;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.Leading;
import com.itextpdf.layout.properties.ListNumberingType;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.itextpdf.kernel.geom.PageSize.A4;
import static com.itextpdf.layout.properties.TextAlignment.CENTER;
import static com.itextpdf.layout.properties.TextAlignment.LEFT;
import static com.itextpdf.layout.properties.TextAlignment.RIGHT;
import static com.itextpdf.layout.properties.UnitValue.createPercentArray;
import static com.itextpdf.layout.properties.VerticalAlignment.BOTTOM;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.Month.APRIL;
import static java.time.Month.DECEMBER;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JANUARY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.MAY;
import static java.time.Month.NOVEMBER;
import static java.time.Month.OCTOBER;
import static java.time.Month.SEPTEMBER;
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
			PlannerDescription plannerDescription = new PlannerDescription("Planagenda", "2024 – 2025",
					// 2, 0, 1, 9, ClassItemStructure.CLASS_ROOM_SINGLE,
					// EnumSet.noneOf(StaticPage.class),
					2, 3, 3, 7, ClassItemStructure.CLASS_ROOM_SINGLE,
					EnumSet.of(
							StaticPage.EMERGENCY_PLAN,
							StaticPage.SCHEDULE_AND_VACATIONS,
							StaticPage.SURVIVE_FRESHMAN_YEAR,
							StaticPage.PLANNING_HAND,
							// PlannerDescription.StaticPage.SURVIVE_LEARNING,
							StaticPage.USEFUL_STUFF,
							StaticPage.STUDYING_TIPS,
							StaticPage.PREREQUISITES_LEARNING,
							StaticPage.PLANNING_INSTRUCTIONS,
							StaticPage.PERSONAL_GOALS
					),
					// new DateTitle(LocalDate.of(2024, SEPTEMBER, 14), "Testing..."),
					new DateTitle(LocalDate.of(2024, OCTOBER, 26), "Herfstvakantie"),
					new DateTitle(LocalDate.of(2024, NOVEMBER, 4), ""),
					new DateTitle(LocalDate.of(2024, DECEMBER, 21), "Kerstvakantie"),
					new DateTitle(LocalDate.of(2024, DECEMBER, 25), "1e Kerstdag"),
					new DateTitle(LocalDate.of(2024, DECEMBER, 26), "2e Kerstdag"),
					new DateTitle(LocalDate.of(2024, DECEMBER, 27), "Kerstvakantie"),
					new DateTitle(LocalDate.of(2025, JANUARY, 6), ""),
					new DateTitle(LocalDate.of(2025, FEBRUARY, 15), "Voorjaarsvakantie"),
					// new DateTitle(LocalDate.of(2025, FEBRUARY, 22), "Voorjaarsvakantie"),
					new DateTitle(LocalDate.of(2025, FEBRUARY, 24), ""),
					new DateTitle(LocalDate.of(2025, APRIL, 19), "Meivakantie"),
					new DateTitle(LocalDate.of(2025, APRIL, 21), "1e Paasdag"),
					new DateTitle(LocalDate.of(2025, APRIL, 22), "2e Paasdag"),
					new DateTitle(LocalDate.of(2025, APRIL, 23), "Meivakantie"),
					new DateTitle(LocalDate.of(2025, APRIL, 27), "Koningsdag"),
					new DateTitle(LocalDate.of(2025, APRIL, 28), "Meivakantie"),
					new DateTitle(LocalDate.of(2025, MAY, 6), ""),
					new DateTitle(LocalDate.of(2025, MAY, 29), "Hemelvaart"),
					new DateTitle(LocalDate.of(2025, MAY, 30), "dag na Hemelvaart (vrij)"),
					new DateTitle(LocalDate.of(2025, MAY, 31), ""),
					new DateTitle(LocalDate.of(2025, JUNE, 8), "1e Pinksterdag"),
					new DateTitle(LocalDate.of(2025, JUNE, 9), "2e Pinksterdag"),
					new DateTitle(LocalDate.of(2025, JUNE, 10), ""),
					new DateTitle(LocalDate.of(2025, JULY, 12), "Zomervakantie"),
					new DateTitle(LocalDate.of(2025, JULY, 19), "Zomervakantie"),
					new DateTitle(LocalDate.of(2024, SEPTEMBER, 2), "") // Order doesn't matter...
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

		// Augment the list of date titles to ensure the first date is a Monday, and the last date is a Sunday.

		NavigableMap<LocalDate, String> sortedDateTitles = plannerDescription.sortedDateTitles();
		LocalDate startDate = sortedDateTitles.firstKey().with(TemporalAdjusters.previousOrSame(MONDAY));
		if (!sortedDateTitles.containsKey(startDate)) {
			dateTitles.add(new DateTitle(startDate, sortedDateTitles.firstEntry().getValue()));
		}
		LocalDate endDate = sortedDateTitles.lastKey().with(TemporalAdjusters.nextOrSame(SUNDAY));
		if (!sortedDateTitles.containsKey(endDate)) {
			dateTitles.add(new DateTitle(endDate, sortedDateTitles.lastEntry().getValue()));
		}

		this.plannerDescription = new PlannerDescription(plannerDescription.title(), plannerDescription.subtitle(), plannerDescription.timeTablePages(),
				plannerDescription.notesPages(), plannerDescription.mindmapPages(), plannerDescription.numClasses(), plannerDescription.classItemStructure(),
				plannerDescription.staticPages(), dateTitles);
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
		document.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
			PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
			document.draw(docEvent, (canvas, area) -> {
				int pageNumber = docEvent.getDocument().getPageNumber(docEvent.getPage());
				if (pageNumber == 1) {
					return;
				}

				int fontSize = 10;
				float centerX = (area.getLeft() + area.getRight()) / 2;
				float footerY = area.getBottom() - fontSize * 2.1f;
				canvas.setFontSize(fontSize);
				canvas.showTextAligned(String.valueOf(pageNumber), centerX, footerY, CENTER);
			});
		});

		if (plannerDescription.staticPages().isEmpty()) {
			document.drawFullPage(drawFullPageImage("/page_front.jpg"));
			document.startNewPage(false);
			document.startNewPage(true);
		} else {
			addTitlePage(document, plannerDescription.title(), plannerDescription.subtitle());
		}

		if (plannerDescription.staticPages().contains(StaticPage.EMERGENCY_PLAN)) {
			addEmergencyPlan(document);
		}

		if (plannerDescription.staticPages().contains(StaticPage.SCHEDULE_AND_VACATIONS)) {
			addClassSchedulesAndVacations(document);
		}
		if (plannerDescription.staticPages().contains(StaticPage.SURVIVE_FRESHMAN_YEAR)) {
			addHowToSurviveTheFreshmanYear(document);
		}

		if (plannerDescription.staticPages().contains(StaticPage.PLANNING_HAND)) {
			addPlanningHand(document);
		}
		if (plannerDescription.staticPages().contains(StaticPage.SURVIVE_LEARNING)) {
			addHowToSurviveLearning(document);
		}
		if (plannerDescription.staticPages().contains(StaticPage.USEFUL_STUFF)) {
			addUsefulStuff(document);
		}
		if (plannerDescription.staticPages().contains(StaticPage.STUDYING_TIPS)) {
			addStudyingTips(document);
		}
		if (plannerDescription.staticPages().contains(StaticPage.PREREQUISITES_LEARNING)) {
			addPrerequisitesForLearning(document);
		}

		/*
		 * The week planing pages must start on a left-hand page (i.e., an even numbered page) to ensure pages open with a full week in view.
		 * Add an extra timetable page if necessary to achieve this.
		 */
		boolean addPlanningInstructions = plannerDescription.staticPages().contains(StaticPage.PLANNING_INSTRUCTIONS);
		boolean addPersonalGoals = plannerDescription.staticPages().contains(StaticPage.PERSONAL_GOALS);
		int pagesBeforePlanner = document.numberOfPagesWrittenTo() + plannerDescription.timeTablePages() +
		                         (addPlanningInstructions ? 1 : 0) + (addPersonalGoals ? 1 : 0);
		int extraTimetablePages = pagesBeforePlanner % 2;
		addTimeSpentTables(document, plannerDescription.timeTablePages() + extraTimetablePages);

		if (addPlanningInstructions) {
			addPlanningInstructions(document); // 1 page
		}

		if (addPersonalGoals) {
			addPersonalGoals(document); // 1 page
		}

		// No page numbers beyond this point.
		document.removeEventHandlers(PdfDocumentEvent.END_PAGE);
		document.startNewPage(true); // Needed because all pages have been flushed.

		addPlanningWeeks(document, plannerDescription.numClasses(), plannerDescription.sortedDateTitles());

		addNotesPages(document, plannerDescription.notesPages());

		// The result must be printable as a booklet, which means a fourfold number of pages. Calculate how many are missing (0-3).
		// The formula purposefully comes up two pages short to accommodate the back cover.
		int extraMindmapPages = 3 - (document.numberOfPagesWrittenTo() + plannerDescription.mindmapPages() + 1) % 4;
		addMindMapPages(document, plannerDescription.mindmapPages() + extraMindmapPages);

		document.startNewPage(true);
		document.startNewPage(true);
		if (plannerDescription.staticPages().isEmpty()) {
			document.drawFullPage(drawFullPageImage("/page_back.jpg"));
		} else {
			// Nearly blank back page
			Paragraph closingRemarks = document.createParagraph(PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC), 10f)
					.setTextAlignment(RIGHT)
					.add("Gemaakt naar ontwerp van de Huizermaat");
			Rectangle pageArea = document.getEffectiveArea();
			document.addInFlow(closingRemarks.setFixedPosition(pageArea.getLeft(), pageArea.getBottom(), pageArea.getWidth()));
		}
	}

	BiConsumer<Canvas, Rectangle> drawFullPageImage(String pageImageResource) {
		ImageData imageData = ImageDataFactory.create(requireNonNull(getClass().getResource(pageImageResource)));
		return (canvas, area) -> {
			float overflowPerEdge = 10;
			Rectangle pageSize = canvas.getPage().getPageSize();
			float scaledWidth = pageSize.getWidth() + 2 * overflowPerEdge;
			float scaledHeight = imageData.getHeight() * scaledWidth / imageData.getWidth();
			float offsetY = pageSize.getHeight() - scaledHeight;
			offsetY -= overflowPerEdge * Math.signum(offsetY);

			Image image = new Image(imageData, -overflowPerEdge, offsetY, scaledWidth);
			canvas.add(image);
		};
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
				.add(italic("Lestijden Onderbouw\n\u00A0").setFontSize(14)).setTextAlignment(CENTER));

		UnitValue[] columnWidths = createPercentArray(new float[]{50, 50});
		Table table = new Table(columnWidths).setAutoLayout().setHorizontalAlignment(HorizontalAlignment.CENTER)
				.setPadding(0).setMargin(0).setMarginBottom(16);
		for (String cellText : java.util.List.of(
				"Normaal rooster", "Verkort rooster",
				"1. 08:15 - 09:15", "1. 08:15 - 08:55",
				"2. 09:15 - 10:15", "2. 08:55 - 09:35",
				"pauze", /*       */"pauze",
				"3. 10:30 - 11:30", "3. 09:55 - 10:35",
				"4. 11:30 - 12:30", "4. 10:35 - 11:15",
				"pauze", /*       */"pauze",
				"5. 13:00 - 14:00", "5. 11:35 - 12:15",
				"6. 14:00 - 15:00", "6. 12:15 - 12:55",
				"pauze", /*       */"7. 12:55 - 13:35",
				"7. 15:15 - 16:15", "8. 13:35 - 14:15"

		)) {
			table.addCell(createCell(document, 1, cellText).setPadding(mmToPt(2)));
		}
		document.addInFlow(table);

		document.addInFlow(document.createParagraph()
				.add(bold("\nVakanties en lesvrije dagen\n").setFontSize(16)).setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph()
				.add(italic("2024 – 2025\n\n").setFontSize(14)).setTextAlignment(CENTER));

		table = new Table(createPercentArray(new float[]{4.75f, 9.25f})).setAutoLayout()//.setFixedLayout()
				.setHorizontalAlignment(HorizontalAlignment.CENTER)
				.setPadding(0).setMargin(0).setMarginBottom(16);
		for (String cellText : java.util.List.of(
				"Herfstvakantie", "Zaterdag 26 oktober 2024 t/m zondag 3 november 2024",
				"Kerstvakantie", "Zaterdag 21 december 2024 t/m zondag 5 januari 2025",
				"Voorjaarsvakantie", "Zaterdag 15 februari 2025 t/m zondag 23 februari 2025",
				"Meivakantie", "Zaterdag 19 april 2025 t/m maandag 5 mei 2025",
				"Hemelvaart", "Donderdag 29 mei en vrijdag 30 mei 2025",
				"2e Pinksterdag", "Maandag 9 juni 2025",
				"Zomervakantie", "Zaterdag 12 juli 2025 t/m zondag 24 augustus 2025"
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
				.add(bold("\nEn dan kun je nu aan de slag, succes!")).setTextAlignment(CENTER));
	}

	private void addPlanningInstructions(WritableDocument document) throws IOException {
		LOGGER.debug("Adding planning instructions");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Hoe overleef ik het plannen?").setFontSize(16))
				.add("\n\n"));
		document.addInFlow(document.createParagraph()
				.add("""
						In deze agenda kun je voor jezelf een overzicht creëren van je moet doen, wanneer het af moet zijn,
						en ook wanneer je het af gaat maken. Zo ben je goed georganiseerd!
						""".replace("\n", " ")));
		document.addInFlow(document.createParagraph()
				.add("Hieronder staan de stappen voor het maken en uitvoeren van een planning. ")
				.add("Deze stappen zijn belangrijk om in je achterhoofd te houden tijdens het plannen, lees ze daarom goed door.")
				.add("\n\n"));

		document.addInFlow(document.createParagraph()
				.add(bold("\nStappenplan voor het maken van een planning:").setFontSize(16))
				.add("\n\n"));

		Image checkedCircleImage = document.loadSvgImageResource("/circle-checked.svg").setWidth(10).setHeight(10);

		List list = document.createList().setListSymbol(ListNumberingType.DECIMAL);
		list.setPostSymbolText(") ");
		list.add("Noteer toetsen en huiswerk in de planagenda.");
		list.add("Weet je niet wat het huiswerk of leerwerk is? Check de studiewijzer!");
		list.add("Geef toetsen en inlevermomenten een kleurtje (rood, roze, of geel).");
		list.add("Bekijk SOM een week vooruit. Zo weet je of je binnenkort een toets hebt.");
		list.add("Houd rekening met afspraken buiten school. Check hiervoor je tijdschema op blz. 18-20.\n" +
		         "Op dagen met veel afspraken kun je minder huiswerk maken.");
		list.add("Plan je huiswerk en toetsen in. Hak het in kleine stukjes (taken).");
		list.add("Dan ga je aan de slag! Nummer de taken. Begin bij de belangrijkste taak.");
		ListItem listItem = new ListItem();
		listItem.add(document.createParagraph().add("Taken die af zijn, vink je af met een check ").add(checkedCircleImage).add("."));
		list.add(listItem);
		list.add("Heb je het aan het eind van de dag nog niet alles af? Plan deze taken opnieuw in.");
		document.addInFlow(list);
	}

	private void addPersonalGoals(WritableDocument document) {
		LOGGER.debug("Adding personal goals page");
		BiConsumer<Integer, Paragraph> titleConsumer = (i, par) -> par.add(bold("Wat zijn je doelen voor de komende periode?\n\n\n").setFontSize(16));
		addLinesPagesWithTitle(document, 1, titleConsumer);
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
		Image handImage = new Image(document.loadPdfPageAsObject("/hand.pdf", 1));
		handImage.scaleToFit(pageArea.getWidth() / PHI, pageArea.getHeight() / PHI);
		// centered on the page, but then moved down 20mm
		float x = pageArea.getX() + (pageArea.getWidth() - handImage.getImageScaledWidth()) / 2f - mmToPt(25);
		float y = pageArea.getY() + (pageArea.getHeight() - handImage.getImageScaledHeight()) / 2f - mmToPt(10);
		handImage.setFixedPosition(x, y);
		document.addInFlow(handImage);

		document.addInFlow(document.createParagraph(14)
				.add("Wat moet ik doen?")
				.setFixedPosition(x - mmToPt(1), y + mmToPt(63), mmToPt(25)));
		document.addInFlow(document.createParagraph(14)
				.add("Waarom moet ik dat doen?\nWat kan ik ervan leren?")
				.setFixedPosition(x + mmToPt(10), y + mmToPt(110), mmToPt(35)));
		document.addInFlow(document.createParagraph(14)
				.add("Wanneer moet het af zijn?")
				.setFixedPosition(x + mmToPt(55), y + mmToPt(120), mmToPt(40)));
		document.addInFlow(document.createParagraph(14)
				.add("Wat heb ik nodig?")
				.setFixedPosition(x + mmToPt(75), y + mmToPt(108), mmToPt(50)));
		document.addInFlow(document.createParagraph(14)
				.add("Wanneer ben ik klaar? Wanneer ben ik tevreden?")
				.setFixedPosition(x + mmToPt(90), y + mmToPt(85), mmToPt(60)));
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
			document.addInFlow(document.createParagraph().add("\u00A0"));
		}
	}

	private void addUsefulStuff(WritableDocument document) {
		LOGGER.debug("Adding useful abbreviations and links");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("\nHandige afkortingen").setFontSize(16))
				.add("\n\n"));

		Table table1 = new Table(2).setPadding(0).setMargin(0).setMarginBottom(16).setBorder(Border.NO_BORDER);
		table1.addHeaderCell(createCell0(document, "Vak")).addHeaderCell(createCell0(document, "Afkorting"));
		for (String cellText : java.util.List.of(
				"Nederlands", "Nl",
				"Engels", "En",
				"Frans", "Fr",
				"Duits", "Du",
				"Wiskunde", "Wi",
				"Science", "Sc",
				"Osa", "Os",
				"Handvaardigheid", "Hv",
				"Tekenen", "Te",
				"Lichamelijke Opvoeding\u00A0\u00A0\u00A0", "LO",
				"Muziek", "Mu"
		)) {
			table1.addCell(createCell0(document, cellText));
		}

		document.addInFlow(table1);//.addInFlow(document.createParagraph().add("\u00A0"));
		document.addInFlow(document.createParagraph()
				.add(bold("\nHuiswerk Noteren").setFontSize(16))
				.add("\n\n"));

		Table table2 = new Table(createPercentArray(new float[]{3, 2})).setPadding(0).setMargin(0).setMarginBottom(16).setBorder(Border.NO_BORDER);
		table2.addHeaderCell(createCell0(document, "Wat")).addHeaderCell(createCell0(document, "Afkorting"));
		for (String cellText : java.util.List.of(
				"Maken", "M",
				"Leren", "Lr",
				"Lezen", "Lz",
				"Toets", "T",
				"Schriftelijke Overhoring", "SO",
				"Praktische Opdracht", "PO",
				"Paragraaf", "§"
		)) {
			table2.addCell(createCell0(document, cellText));
		}
		document.addInFlow(table2);//.addInFlow(document.createParagraph().add("\u00A0"));

		document.addInFlow(document.createParagraph()
				.add(bold("\nHandige Sites").setFontSize(16))
				.add("\n\n"));

		Table table3 = new Table(createPercentArray(new float[]{2, 3}))
				.useAllAvailableWidth().setFixedLayout()
				.setPadding(0).setMargin(0).setMarginBottom(16).setBorder(Border.NO_BORDER);
		table3.addHeaderCell(createCell(document, "Website")).addHeaderCell(createCell(document, "Inloggegevens"));
		java.util.List.of(
				Map.entry("SomToday (agenda)", "https://inloggen.somtoday.nl/"),
				Map.entry("Zermelo (rooster)", "https://hzm-gsf.zportal.nl/"),
				Map.entry("Schoolmail", "https://mail.google.com/")
		).forEach(entry ->
				table3.addCell(createCell(document, 1, LEFT,
						p -> p.add(entry.getKey() + "\n").add(new Link(entry.getValue(), PdfAction.createURI(entry.getValue())))
				)).addCell(emptyCell(document)));
		for (int i = 0; i < 4; i++) {
			table3.addCell(createCell(document, "\n\u00A0")).addCell(emptyCell(document));
		}
		document.addInFlow(table3);
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
		document.addInFlow(document.createParagraph().add(bold("Wat moet ik doen als ik te laat kom?")));
		document.addInFlow(document.createParagraph().add("""
				Als je te laat op school bent of te laat voor een les, haal je altijd eerst een telaatbriefje bij de conciërges. Met zo'n
				briefje mag jij de les in, of je nu geoorloofd te laat was of niet.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nWat moet ik doen als ik naar de orthodontist/dokter moet?")));
		document.addInFlow(document.createParagraph().add("""
				Je ouders moeten voor de afspraak aan school laten weten welk lesuur je er niet bent.
				Dat kan door te bellen, of ze geven je een briefje voor de conciërges mee met daarin waarom je welk lesuur niet aanwezig kunt zijn.
				Dat briefje moet je dan voor de afspraak aan een van de conciërges geven.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nWat moet ik doen als ik ziek ben?")));
		document.addInFlow(document.createParagraph()
				.add("Als je ziek bent, dan bellen je ouders voor het eerste lesuur naar school om je ziek te melden.\n\u00A0"));

		document.addInFlow(document.createParagraph().add(bold("Wat moet ik doen als ik ziek naar huis wil?")));
		document.addInFlow(document.createParagraph().add("""
				Wanneer je je tijdens lestijd opeens niet lekker voelt, dan ga je naar je docent en geef je aan dat je naar huis wilt,
				je meldt je daarna af bij de conciërges.
				Als je thuis bent, laat je je ouders naar school bellen dat je weer veilig thuis bent.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nWat moet ik doen als ik een toets gemist heb?")));
		document.addInFlow(document.createParagraph().add("""
				Je geeft bij je vakdocent aan dat je een toets gemist hebt en vraagt de vakdocent wanneer je de toets kunt inhalen.
				Je vakdocent zorgt dat er een inhaaltoets voor je klaar ligt in de mediatheek.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nNeem ik mijn jas en gymtas mee naar het lokaal?")));
		document.addInFlow(document.createParagraph().add("""
				Op Huizermaat heeft elke leerling een eigen kluisje. Hierin kun je je gymtas, je jas en je telefoon bewaren.
				Tijdens de gymles bewaar je je chromebook en je telefoon in je kluisje.
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
				Vanaf de projectweek zullen we ze in de lessen gaan gebruiken en moet je dus altijd een opgeladen chromebook bij je hebben.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nMag je je telefoon gebruiken tijdens de les?")));
		document.addInFlow(document.createParagraph().add("""
				Nee, tijdens de lessen bewaar je je telefoon, terwijl die uit is, in je kluis.
				Een telefoon leidt immers veel te veel af van wat er tijdens de les gebeurt.
				Als je internet nodig hebt voor een schoolopdracht, gebruik je je chromebook.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nWaarom geven we huiswerk?")));
		document.addInFlow(document.createParagraph().add("""
				We geven huiswerk omdat je dan nog eens rustig kunt oefenen met de stof die in de les is behandeld.
				Of je maakt juist huiswerk om je voor te bereiden op de les die komen gaat.
				We bouwen het rustig op. Na de herfstvakantie moet je rekenen op één à anderhalf uur per dag (ook in het weekend).
				Als je goed meedoet tijdens de les, scheelt dat in wat je thuis moet doen. Op school kun je bovendien de docent of
				medeleerlingen om hulp vragen. Het is vooral handig om leerwerk rustig thuis te doen.
				""".replace("\n", " ")));

		document.addInFlow(document.createParagraph().add(bold("\nWat zijn de afspraken in de leszone?")));
		document.addInFlow(document.createParagraph().add("""
				Zodra je door de klapdeuren een leszone in loopt, gelden de volgende afspraken:
				""".replace("\n", " ")));
		List learningZoneRules = document.createList().setListSymbol("- ");
		learningZoneRules.add("Je praat zachtjes en loopt rustig.");
		learningZoneRules.add("Je hebt geen jas aan (deze ligt al in je kluis).");
		learningZoneRules.add("Je hebt je telefoon niet bij je (deze ligt al in je kluis).");
		learningZoneRules.add("Eten en drinken is niet toegestaan in de leszone (wel daarbuiten).");
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

	private void addPlanningWeeks(WritableDocument document, int numClassSlots, NavigableMap<LocalDate, String> dateTitles)
			throws IOException {
		LOGGER.debug("Adding planning weeks");

		Image emptyCircleImage = document.loadSvgImageResource("/circle-empty.svg").setWidth(15).setHeight(15);
		UnitValue[] columnWidths = createPercentArray(3);
		// numClassSlots is also used for planning slots on Saturday
		int numPlanningSlots = 13 - numClassSlots;
		if (numClassSlots < 3) {
			throw new IllegalArgumentException("numClassSlots must at least be 3");
		}
		if (numPlanningSlots < 3) {
			throw new IllegalArgumentException("numPlanningSlots must at least be 3");
		}

		LocalDate lastDateThatMustBePresent = dateTitles.lastKey();
		for (LocalDate monday = dateTitles.firstKey(); !monday.isAfter(lastDateThatMustBePresent); monday = monday.plusWeeks(1)) {
			int extraPadding = 9;
			LOGGER.debug("Adding planning week starting on {}", monday);

			ClassItemStructure classItemStructure = plannerDescription.classItemStructure();
			int numberOfWeeks = (int) WEEKS.between(dateTitles.firstKey(), monday);
			if (classItemStructure == null) {
				ClassItemStructure[] choices = new ClassItemStructure[]{ClassItemStructure.CLASS_ROOM_SINGLE, ClassItemStructure.CLASS_ROOM_DOUBLE,
						ClassItemStructure.CLASS_ROOM_TRIPLE};
				classItemStructure = choices[(numberOfWeeks % choices.length)];
			}

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
				table.addCell(getCreateClassCell(document, 1, extraPadding, classItemStructure, r + 1));
				table.addCell(getCreateClassCell(document, 1, extraPadding, classItemStructure, r + 1));
				table.addCell(getCreateClassCell(document, 1, extraPadding, classItemStructure, r + 1));
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
			// document.addInFlow(document.createParagraph().add("\n"));

			table = new Table(columnWidths).useAllAvailableWidth().setPadding(0).setMargin(0).setMarginBottom(16).setFixedLayout();
			for (int c = 0; c < 3; c++) {
				// Thursday to Saturday
				table.addCell(createDateCellWithText(document, dateTitles, columnWidth, monday.plusDays(c + 3)));
			}
			for (int r = 0; r < numClassSlots; r++) {
				table.addCell(getCreateClassCell(document, 2, extraPadding, classItemStructure, r + 1));
				table.addCell(getCreateClassCell(document, 2, extraPadding, classItemStructure, r + 1));
				// These planning cells are less high than the others, but will grow to the same size as the numbered cells.
				// The other two sets of planning sets must create their own height correctly.
				if (r == 0) {
					table.addCell(createCell(document, 1, "Planning"));
					table.addCell(createCell(document, 1, RIGHT, p -> p.add("\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				} else {
					table.addCell(createCell(document, 2, RIGHT, p -> p.add("\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				}
			}
			table.addCell(createCell(document, 1, "Planning"));
			table.addCell(createCell(document, 1, "Planning"));
			// Sunday
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

	private Cell getCreateClassCell(WritableDocument document, int rowspan, int extraPadding, ClassItemStructure classItemStructure, int classHour) {
		float extraPadding1 = extraPadding + 22f;
		float extraPadding2 = ((extraPadding + 11f) / 2f) - 0.5f;
		float extraPadding3 = (extraPadding / 3f) - 1f;
		float extraPadding4 = extraPadding3 - 0.577f;
		if (classItemStructure == ClassItemStructure.SINGLE_FIELD) {
			return createCell(document, rowspan, classHour + "\n\n\u00A0").setPaddingBottom(extraPadding);
		}
		Table t = new Table(createPercentArray(new float[]{1, 3})).useAllAvailableWidth().setFixedLayout().setPadding(0).setMargin(0)
				.setBorder(Border.NO_BORDER);
		switch (classItemStructure) {
			case CLASS_ROOM_SINGLE -> {
				t.addCell(b(6, createCell(document, 1, classHour + "\u00A0").setPaddingBottom(extraPadding2)));
				t.addCell(b(1, createCell(document, 2, "\u00A0").setPaddingBottom(extraPadding1)));
				t.addCell(b(12, createCell(document, 1, "\u00A0").setPaddingBottom(extraPadding2)));
			}
			case CLASS_ROOM_DOUBLE -> {
				t.addCell(b(6, createCell(document, 1, classHour + "\u00A0").setPaddingBottom(extraPadding2)));
				t.addCell(b(3, createCell(document, 1, "\u00A0").setPaddingBottom(extraPadding2)));
				t.addCell(b(12, createCell(document, 1, "\u00A0").setPaddingBottom(extraPadding2)));
				t.addCell(b(9, createCell(document, 1, "\u00A0").setPaddingBottom(extraPadding2)));
			}
			case CLASS_ROOM_TRIPLE -> {
				t.addCell(b(6, createCell(document, 1, classHour + "\u00A0").setPaddingBottom(extraPadding3)));
				t.addCell(b(3, createCell(document, 1, "\u00A0").setPaddingBottom(extraPadding3)));
				t.addCell(b(14, createCell(document, 1, "\u00A0").setPaddingBottom(extraPadding4)));
				t.addCell(b(11, createCell(document, 1, "\u00A0").setPaddingBottom(extraPadding4)));
				t.addCell(b(12, createCell(document, 1, "\u00A0").setPaddingBottom(extraPadding4)));
				t.addCell(b(9, createCell(document, 1, "\u00A0").setPaddingBottom(extraPadding4)));
			}
		}
		return new Cell(rowspan, 1).setMargin(0).setPadding(0).add(t);
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

	private Cell createCell(WritableDocument document, String text) {
		return createCell(document, 1, text);
	}

	private Cell createCell(WritableDocument document, int rowspan, String text) {
		return createCell(document, rowspan, TextAlignment.LEFT, paragraph -> paragraph.add(requireNonNullElse(text, "\u00A0")));
	}

	private Cell createCell0(WritableDocument document, String text) {
		return createCell(document, 1, text).setBorder(Border.NO_BORDER);
	}

	private Cell createCell(WritableDocument document, int rowspan, TextAlignment alignment, Consumer<Paragraph> paragraphConsumer) {
		Paragraph paragraph = document.createParagraph(10.5f).setMargin(0).setMultipliedLeading(1);
		paragraphConsumer.accept(paragraph);
		Cell cell = new Cell(rowspan, 1).setMargin(0);
		return cell.setTextAlignment(requireNonNull(alignment)).add(paragraph);
	}

	/**
	 * Clear a cell border according to the nibble: the last four bits of nibble, if unset, clear (remove) the cell border.
	 * The four bits (MSB to LSB) are to keep (if set) or clear (if unset) the top, right, bottom and left borders respectively.
	 */
	private Cell b(int nibble, Cell element) {
		if ((nibble & 0b00001000) == 0) {
			element.setBorderTop(Border.NO_BORDER);
		}
		if ((nibble & 0b00000100) == 0) {
			element.setBorderRight(Border.NO_BORDER);
		}
		if ((nibble & 0b00000010) == 0) {
			element.setBorderBottom(Border.NO_BORDER);
		}
		if ((nibble & 0b00000001) == 0) {
			element.setBorderLeft(Border.NO_BORDER);
		}
		return element;
	}

	private void addNotesPages(WritableDocument document, int numberOfNotesPages) {
		LOGGER.debug("Adding {} notes pages", numberOfNotesPages);
		BiConsumer<Integer, Paragraph> titleConsumer = (i, par) -> {
			String header = i == 0 ? "\nRUIMTE VOOR AANTEKENINGEN\n\n\n" : "\n\n\n\n";
			par.add(header).setTextAlignment(CENTER);
		};
		addLinesPagesWithTitle(document, numberOfNotesPages, titleConsumer);
	}

	private static void addLinesPagesWithTitle(WritableDocument document, int numberOfNotesPages, BiConsumer<Integer, Paragraph> titleConsumer) {
		float pageWidth = document.getEffectiveArea().getWidth();
		Paragraph blockWithLines = document.createParagraph(null, 14)
				.addTabStops(new TabStop(pageWidth, TabAlignment.LEFT, new SolidLine(.75f)))
				.add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n")
				.add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n").add("\n");
		for (int i = 0; i < numberOfNotesPages; i++) {
			document.startNewPage(false);
			Paragraph paragraph = document.createParagraph();
			titleConsumer.accept(i, paragraph);
			document.addInFlow(paragraph);
			document.addInFlow(blockWithLines).addInFlow(blockWithLines).addInFlow(blockWithLines).addInFlow(blockWithLines);
		}
	}

	private void addMindMapPages(WritableDocument document, int numberOfMindMapPages) {
		LOGGER.debug("Adding {} mind map pages", numberOfMindMapPages);
		for (int i = 0; i < numberOfMindMapPages; i++) {
			document.startNewPage(false);
			String header = i == 0 ? "\nRUIMTE VOOR MINDMAPS\n\n\n" : "\n\n\n\n";
			document.addInFlow(document.createParagraph().add(header).setTextAlignment(CENTER));
			document.draw((canvas, area) -> canvas.getPdfCanvas()
					.setLineWidth(0.5f).setColor(ColorConstants.BLACK, false)
					.rectangle(document.getEffectiveArea())
					.stroke());
		}
	}
}
