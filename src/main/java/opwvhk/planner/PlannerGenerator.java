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
import com.itextpdf.layout.properties.FloatPropertyValue;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.Leading;
import com.itextpdf.layout.properties.ListNumberingType;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.itextpdf.kernel.geom.PageSize.A4;
import static com.itextpdf.layout.properties.TextAlignment.CENTER;
import static com.itextpdf.layout.properties.TextAlignment.LEFT;
import static com.itextpdf.layout.properties.TextAlignment.RIGHT;
import static com.itextpdf.layout.properties.UnitValue.createPercentArray;
import static com.itextpdf.layout.properties.VerticalAlignment.BOTTOM;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.Month.*;
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
	public static void main(String[] args) throws IOException {
		try (OutputStream output = new FileOutputStream("planner.pdf")) {
			LocalDate startDate = LocalDate.of(2025, AUGUST, 20);
			LocalDate endDate = LocalDate.of(2026, FEBRUARY, 14);

			//noinspection ExtractMethodRecommender
			java.util.List<DateTitleFromTo> holidays = new ArrayList<>();
			// Holidays usually last n weeks + 2 days (weekend)
			holidays.add(new DateTitleFromTo("Herfstvakantie", LocalDate.of(2025, OCTOBER, 11), 9));
			holidays.add(new DateTitleFromTo("Kerstvakantie", LocalDate.of(2025, DECEMBER, 20), 16));
			holidays.add(new DateTitleFromTo("1e Kerstdag", LocalDate.of(2025, DECEMBER, 25)));
			holidays.add(new DateTitleFromTo("2e Kerstdag", LocalDate.of(2025, DECEMBER, 26)));
			holidays.add(new DateTitleFromTo("Voorjaarsvakantie", LocalDate.of(2026, FEBRUARY, 14), 9));
			// holidays.add(new DateTitleFromTo("1e Paasdag", LocalDate.of(2026, APRIL, 5)));
			holidays.add(new DateTitleFromTo("2e Paasdag", LocalDate.of(2026, APRIL, 6)));
			holidays.add(new DateTitleFromTo("Meivakantie", LocalDate.of(2026, APRIL, 18), 16));
			holidays.add(new DateTitleFromTo("Koningsdag", LocalDate.of(2026, APRIL, 27)));
			holidays.add(new DateTitleFromTo("Bevrijdingsdag", LocalDate.of(2026, MAY, 5)));
			holidays.add(new DateTitleFromTo("Hemelvaart", LocalDate.of(2026, MAY, 14), 4));
			// holidays.add(new DateTitleFromTo("Hemelvaart", LocalDate.of(2026, MAY, 14)));
			// holidays.add(new DateTitleFromTo("(dag na Hemelvaart)", LocalDate.of(2026, MAY, 15)));
			holidays.add(new DateTitleFromTo("Pinksteren", LocalDate.of(2026, MAY, 23), 3));
			// holidays.add(new DateTitleFromTo("1e Pinksterdag", LocalDate.of(2026, MAY, 24)));
			// holidays.add(new DateTitleFromTo("2e Pinksterdag", LocalDate.of(2026, MAY, 25)));
			holidays.add(new DateTitleFromTo("Zomervakantie", LocalDate.of(2026, JULY, 11), 44));

			int firstYear = holidays.stream().map(DateTitleFromTo::from).mapToInt(LocalDate::getYear).min().getAsInt();
			int lastYear = holidays.stream().map(DateTitleFromTo::from).mapToInt(LocalDate::getYear).max().getAsInt();
			PlannerDescription plannerDescription = new PlannerDescription("Planagenda",
					"%d – %d".formatted(firstYear, lastYear), 2, 0, 0,
					7, ClassItemStructure.CLASS_ROOM_SINGLE,
					EnumSet.of(
							// StaticPage.EMERGENCY_PLAN,
							StaticPage.SCHEDULE_AND_VACATIONS,
							StaticPage.SURVIVE_FRESHMAN_YEAR,
							StaticPage.PLANNING_HAND,
							// StaticPage.SURVIVE_LEARNING,
							StaticPage.USEFUL_STUFF,
							StaticPage.STUDYING_TIPS,
							StaticPage.HOW_TO_LEARN,
							StaticPage.PREREQUISITES_LEARNING,
							StaticPage.PLANNING_INSTRUCTIONS,
							StaticPage.PERSONAL_GOALS,
							StaticPage.GRADE_LIST
					),
					startDate, endDate, holidays
			);
			// PlannerDescription plannerDescription = new PlannerDescription("Florentine", ""
			// 		2, 0, 0, 9, ClassItemStructure.CLASS_ROOM_SINGLE,
			// 		EnumSet.noneOf(StaticPage.class), allDateTitles
			// );
			new PlannerGenerator(plannerDescription).generate(output);
		}
		Desktop.getDesktop().open(new File("planner.pdf"));
	}

	/**
	 * Logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PlannerGenerator.class.getName());

	private static final float PHI = 1.618033988749f;
	private static final String LIST_SYMBOL_HYPHEN_BULLET = "⁃ ";
	private static final String LIST_SYMBOL_BULLET = "• "; // Alternative: ●

	private static final Locale LOCALE = Locale.forLanguageTag("nl-NL");
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm", LOCALE);
	private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM", LOCALE);
	private static final DateTimeFormatter WEEK_NR_FORMATTER = DateTimeFormatter.ofPattern("w", LOCALE);
	private static final DateTimeFormatter DATE_YEAR_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", LOCALE);
	private static final DateTimeFormatter DATE_NO_YEAR_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM", LOCALE);
	private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE: d", LOCALE);
	private static final DateTimeFormatter WEEKDAY_FORMAT = DateTimeFormatter.ofPattern("EEEE", LOCALE);
	/**
	 * The description of the planner to generate.
	 */
	private final PlannerDescription plannerDescription;
	private final float headingFontSize;
	private final float smallHeadingFontSize;

	/**
	 * Create a planner generator.
	 *
	 * @param plannerDescription a description of the planner to generate
	 */
	public PlannerGenerator(PlannerDescription plannerDescription) {
		TemporalAdjuster makeStartDate = TemporalAdjusters.previousOrSame(MONDAY);
		TemporalAdjuster makeEndDate = TemporalAdjusters.nextOrSame(SUNDAY);
		// Augment the list of date titles to ensure the first date is a Monday and the last date is
		// a Sunday, and keep the descriptions for the first/last days.
		this.plannerDescription = plannerDescription.fixStartAndEndDate(makeStartDate, makeEndDate);
		this.headingFontSize = WritableDocument.DEFAULT_FONT_SIZE * 16f / 11f;
		this.smallHeadingFontSize = WritableDocument.DEFAULT_FONT_SIZE * 14f / 11f;
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
		if ("Florentine".equals(plannerDescription.title())) {
			// For Florentine
			generatePlannerForFlorentine(document);
		} else if (!plannerDescription.title().isEmpty()) {
			// Generate the entire planner
			generateFullPlanner(document);
		} else {
			// Planner only
			generatePlanningWeeks(document);
		}
	}

	/**
	 * Generate a week planner with additional stuff into a {@link PdfDocument}.
	 *
	 * @param document the document to write to
	 * @throws IOException  when the planner cannot be written
	 * @throws PdfException when the planner cannot be generated
	 */
	public void generateFullPlanner(WritableDocument document) throws IOException, PdfException {
		addPageNumbersFromPage2(document);
		addTitlePage(document, plannerDescription.title(), plannerDescription.schoolYear());

		if (plannerDescription.staticPages().contains(StaticPage.EMERGENCY_PLAN)) {
			addEmergencyPlan(document);
		} else {
			// The emergency plan is a special page: is may occur on the backside of the cover.
			document.startNewPage(true);
		}

		if (plannerDescription.staticPages().contains(StaticPage.SCHEDULE_AND_VACATIONS)) {
			addClassSchedulesAndVacations(document);
		}
		if (plannerDescription.staticPages().contains(StaticPage.GRADE_LIST)) {
			addGradeList(document);
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
		if (plannerDescription.staticPages().contains(StaticPage.HOW_TO_LEARN)) {
			addHowToLearn(document);
		}
		if (plannerDescription.staticPages().contains(StaticPage.PREREQUISITES_LEARNING)) {
			addPrerequisitesForLearning(document);
		}

		/*
		 * The week planing pages must start on a left-hand page (i.e., an even numbered page) to ensure pages open
		 * with a full week in view. Add an extra timetable page if necessary to achieve this.
		 */
		boolean addPlanningInstructions = plannerDescription.staticPages().contains(StaticPage.PLANNING_INSTRUCTIONS);
		boolean addPersonalGoals = plannerDescription.staticPages().contains(StaticPage.PERSONAL_GOALS);
		int numberOfPagesWrittenTo = document.numberOfPagesWrittenTo();
		int pagesBeforePlanner = numberOfPagesWrittenTo + plannerDescription.timeTablePages() +
		                         (addPlanningInstructions ? 1 : 0) + (addPersonalGoals ? 1 : 0);
		int extraTimetablePages = 1 - pagesBeforePlanner % 2; // the # of pages before the planner must be odd.
		addTimeSpentTables(document, plannerDescription.timeTablePages() + extraTimetablePages);

		if (addPlanningInstructions) {
			addPlanningInstructions(document); // 1 page
		}

		if (addPersonalGoals) {
			addPersonalGoals(document); // 1 page
		}

		// No page numbers beyond this point.
		startNextPageAndDropSubsequentPageNumbers(document);

		addPlanningWeeks(document, plannerDescription.numClasses(), plannerDescription.dateTitles());

		addNotesAndMindmapPages(document);

		document.startNewPage(true);
		document.startNewPage(true);
		// Nearly blank back page
		Paragraph closingRemarks = document
				.createParagraph(PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC), 10f)
				.setTextAlignment(RIGHT)
				.add("Gemaakt naar ontwerp van de Huizermaat (onderdeel GSF)");
		Rectangle pageArea = document.getEffectiveArea();
		document.addInFlow(
				closingRemarks.setFixedPosition(pageArea.getLeft(), pageArea.getBottom(), pageArea.getWidth()));
	}

	private static void addPageNumbersFromPage2(WritableDocument document) {
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
	}

	private static void startNextPageAndDropSubsequentPageNumbers(WritableDocument document) {
		document.removeEventHandlers(PdfDocumentEvent.END_PAGE);
		document.startNewPage(true); // Needed because all pages have been flushed.
	}

	private void addNotesAndMindmapPages(WritableDocument document) {
		int notesPages = plannerDescription.notesPages();
		int mindmapPages = plannerDescription.mindmapPages();

		// The result must be printable as a booklet, which means a fourfold number of pages.
		// Calculate how many are missing (0-3).
		// The formula purposefully comes up two pages short to accommodate the back cover.
		int numberOfPagesWrittenTo = document.numberOfPagesWrittenTo();
		int expectedTotalNumberOfPages =
				numberOfPagesWrittenTo + notesPages + mindmapPages + 2;
		int extraPages = 3 - (expectedTotalNumberOfPages - 1) % 4;

		if (mindmapPages == 0) {
			addNotesPages(document, notesPages + extraPages);
		} else {
			addNotesPages(document, notesPages);
			addMindMapPages(document, mindmapPages + extraPages);
		}
	}

	/**
	 * Generate a week planner with special front and back pages into a {@link PdfDocument}.
	 *
	 * @param document the document to write to
	 * @throws IOException  when the planner cannot be written
	 * @throws PdfException when the planner cannot be generated
	 */
	public void generatePlannerForFlorentine(WritableDocument document) throws IOException, PdfException {
		document.drawFullPage(drawFullPageImage("/page_front.jpg"));
		document.startNewPage(false);
		document.startNewPage(true);

		/*
		 * The week planing pages must start on a left-hand page (i.e., an even numbered page) to ensure pages open
		 * with a full week in view. Add an extra timetable page to ensure the number of pages before is odd.
		 */
		int extraTimetablePages = 1 - plannerDescription.timeTablePages() % 2;
		addTimeSpentTables(document, plannerDescription.timeTablePages() + extraTimetablePages);

		document.startNewPage(true); // Needed because all pages have been flushed.

		addPlanningWeeks(document, plannerDescription.numClasses(), plannerDescription.dateTitles());

		addNotesPages(document, plannerDescription.notesPages());

		// The result must be printable as a booklet, which means a fourfold number of pages. Calculate how many are
		// missing (0-3). The formula purposefully comes up two pages short to accommodate the back cover.
		int numberOfPagesWrittenTo = document.numberOfPagesWrittenTo();
		int expectedTotalNumberOfPages = numberOfPagesWrittenTo + plannerDescription.mindmapPages() + 2;
		int extraMindmapPages = 3 - (expectedTotalNumberOfPages - 1) % 4;
		addMindMapPages(document, plannerDescription.mindmapPages() + extraMindmapPages);

		document.startNewPage(true);
		document.startNewPage(true);
		document.drawFullPage(drawFullPageImage("/page_back.jpg"));
	}

	/**
	 * Generate week planner pages into a {@link PdfDocument}.
	 *
	 * @param document the document to write to
	 * @throws IOException  when the planner cannot be written
	 * @throws PdfException when the planner cannot be generated
	 */
	public void generatePlanningWeeks(WritableDocument document) throws IOException, PdfException {
		// Contrary to the other planners, this planner is not a complete, ready-to-print document.
		// So we do not ensure the planner starts at an even-numbered page.
		// document.startNewPage(true); // Needed because all pages have been flushed.
		addPlanningWeeks(document, plannerDescription.numClasses(), plannerDescription.dateTitles());
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

		document.addInFlow(
				document.createParagraph(null, 48).add("\n" + title).setMarginBottom(0).setTextAlignment(CENTER));
		document.addInFlow(
				document.createParagraph(null, 32).add(subtitle + "\n\u00A0").setMarginBottom(0)
						.setTextAlignment(CENTER));
		document.addInFlow(titleImage.setHorizontalAlignment(HorizontalAlignment.CENTER));
		document.addInFlow(document.createParagraph().add("\n\n\n\n\n\n\n\n\n\nDeze planner is van:\n\u00A0"));
		document.addInFlow(document.createParagraph()
				.addTabStops(new TabStop(pageWidth * 0.75f, TabAlignment.LEFT, new SolidLine(.75f)))
				.add(new Tab()).add(" "));
		// ImageData logoImageData = ImageDataFactory.create(
		// 		requireNonNull(getClass().getResource("/logo-huizermaat.png")));
		// float logoWidth = pageWidth * 0.4f;
		// Image logoImage = new Image(logoImageData).setWidth(logoWidth)
		// 		.setHeight(logoWidth * logoImageData.getHeight() / logoImageData.getWidth());
		// document.addInFlow(
		// 		logoImage.setFixedPosition(pageArea.getRight() - logoWidth, pageArea.getBottom(), logoWidth));
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
				.add(bold("Lestijden en vakanties\n\u00A0").setFontSize(headingFontSize)).setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph()
				.add(italic("Lestijden\n\u00A0").setFontSize(smallHeadingFontSize)).setTextAlignment(CENTER));

		UnitValue[] columnWidths = createPercentArray(new float[]{48, 48, 8, 48, 48});
		Table timetableTable = new Table(columnWidths).setAutoLayout().setHorizontalAlignment(HorizontalAlignment.CENTER)
				.setPadding(0).setMargin(0).setMarginBottom(headingFontSize);
		for (String cellText : Arrays.asList(
				// "Normaal rooster", "Verkort rooster",
				// "1. 08:15 - 09:15", "1. 08:15 - 08:55",
				// "2. 09:15 - 10:15", "2. 08:55 - 09:35",
				// "pauze", /*       */"pauze",
				// "3. 10:30 - 11:30", "3. 09:55 - 10:35",
				// "4. 11:30 - 12:30", "4. 10:35 - 11:15",
				// "pauze", /*       */"pauze",
				// "5. 13:00 - 14:00", "5. 11:35 - 12:15",
				// "6. 14:00 - 15:00", "6. 12:15 - 12:55",
				// "pauze", /*       */"7. 12:55 - 13:35",
				// "7. 15:15 - 16:15", "8. 13:35 - 14:15"
				"Tijd", "Lesblok", null, "Tijd", "Lesblok",
				"08:15 - 08:45", "1",        null, "12:15 - 12:45", "8",
				"08:45 - 09:15", "2",        null, "12:45 - 13:15", "Pauze",
				"09:15 - 09:45", "3",        null, "13:15 - 13:45", "9",
				"09:45 - 10:15", "4",        null, "13:45 - 14:15", "10",
				"10:15 - 10:45", "Pause",    null, "14:15 - 14:45", "11",
				"10:45 - 11:15", "5",        null, "14:45 - 15:15", "12",
				"11:15 - 11:45", "6",        null, "15:15 - 15:45", "13",
				"11:45 - 12:15", "7",        null, "15:45 - 16:15", "14"
		)) {
			if (cellText != null) {
				timetableTable.addCell(createCell(document, 1, cellText).setPadding(mmToPt(2)));
			} else {
				timetableTable.addCell(createCell0(document, " ").setPadding(mmToPt(2)));
			}
		}
		document.addInFlow(timetableTable);

		document.addInFlow(document.createParagraph()
				.add(bold("\nVakanties en lesvrije dagen\n").setFontSize(headingFontSize)).setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph()
				.add(italic(plannerDescription.schoolYear()).setFontSize(smallHeadingFontSize))
				.setTextAlignment(CENTER));

		Table holidayTable = new Table(createPercentArray(new float[]{4.75f, 9.25f})).setAutoLayout()//.setFixedLayout()
				.setHorizontalAlignment(HorizontalAlignment.CENTER)
				.setPadding(0).setMargin(0).setMarginBottom(headingFontSize);
		plannerDescription.dateTitleFromToList().stream()
				.sorted(Comparator.comparing(DateTitleFromTo::from).thenComparing(DateTitleFromTo::to))
				.flatMap(dateTitleFromTo -> {
					StringBuilder buffer = new StringBuilder();
					if (dateTitleFromTo.from().equals(dateTitleFromTo.to())) {
						buffer.append(DATE_YEAR_FORMAT.format(dateTitleFromTo.from()));
					} else {
						int yearFrom = dateTitleFromTo.from().getYear();
						int yearTo = dateTitleFromTo.to().getYear();
						DateTimeFormatter fromDateFormat = yearFrom == yearTo ? DATE_NO_YEAR_FORMAT : DATE_YEAR_FORMAT;
						buffer.append(fromDateFormat.format(dateTitleFromTo.from())).append(" t/m ")
								.append(DATE_YEAR_FORMAT.format(dateTitleFromTo.to()));
					}
					String holidayText = buffer.toString();
					return Stream.of(dateTitleFromTo.text(), capitalize(holidayText));
				})
				.forEach(cellText -> holidayTable.addCell(createCell(document, 1, cellText).setPadding(mmToPt(2))));
		document.addInFlow(holidayTable);
	}

	private void addEmergencyPlan(WritableDocument document) {
		LOGGER.debug("Adding emergency plan");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Noodplan\n\u00A0").setFontSize(headingFontSize))
				.setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph());
		document.addInFlow(document.createParagraph()
				.add(bold("Wat doe je wanneer het alarmsignaal klinkt:").setFontSize(smallHeadingFontSize))
				.setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph());
		document.addInFlow(document.createParagraph());
		List list = document.createList().setListSymbol(LIST_SYMBOL_BULLET);
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Laat alles staan (dus geen tassen/jassen enz. meenemen) en ga zo snel mogelijk met je docent naar de \
				verzamelplaats (""").add(bold("= het grasveld naast de fietsenstalling")).add("""
				) en blijf daar als klas bij elkaar.
				\u00A0""")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Indien je docent niet in het lokaal aanwezig is, sluit je dan als klas direct aan bij een andere \
				docent en blijf als klas bij elkaar.
				\u00A0""")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Als je op het moment van alarm niet in je klas bent, maar bijvoorbeeld in de mediatheek, ga dan met \
				de ontruimer van de mediatheek mee naar de verzamelplaats en meld je daar bij je docent af.
				\u00A0""")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Heb je een tussenuur of een uur van lesuitval en je bent op het moment van alarm toch in school of \
				je komt net uit het winkelcentrum, meld je dan bij het meldpunt (= een BHV-er) op de verzamelplaats.
				\u00A0""")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
						Zorg er dus voor dat je zo snel mogelijk via de kortste weg in veiligheid bent,""")
				.add(bold(" en dat je je bij je docent meldt op de verzamelplaats")).add(".\n\u00A0")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
						Je mag de verzamelplaats pas verlaten als daar""").add(bold(" toestemming "))
				.add("voor gegeven is.\n\u00A0")));
		document.addInFlow(list);
	}

	private void addPrerequisitesForLearning(WritableDocument document) {
		LOGGER.debug("Adding prerequisites for learning");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Voorwaarden voor leren\n\u00A0").setFontSize(headingFontSize)));
		document.addInFlow(document.createParagraph().add("""
				Voor je ligt de planagenda. In deze agenda kun je voor jezelf overzicht creëren in wat wanneer af moet \
				zijn, maar ook wanneer je het af gaat maken. Zo ben je goed georganiseerd."""));
		document.addInFlow(document.createParagraph().add("""
						Voordat je aan de slag kunt gaan met het plannen, staan hieronder nog een aantal voorwaarden voor het \
						leren opgesteld. Deze voorwaarden zijn belangrijk om in je achterhoofd te houden tijdens het plannen, \
						lees ze daarom maar goed door.""")
				.add("\n\u00A0"));

		float indent = mmToPt(10);
		document.addInFlow(document.createParagraph()
				.add(bold("Laat je niet afleiden")));
		document.addInFlow(document.createParagraph()
				.setMarginLeft(indent)
				.add("Zorg dat alles wat je kan afleiden (denk aan telefoon, computer, te veel tabbladen open) niet " +
				     "bij jou in de buurt is.")
				.add("\n\u00A0"));
		document.addInFlow(document.createParagraph()
				.add(bold("Zoek een rustige plaats")));
		document.addInFlow(document.createParagraph()
				.setMarginLeft(indent).add("Waar je je op je gemak voelt én kan concentreren.")
				.add("\n\u00A0"));
		document.addInFlow(document.createParagraph()
				.add(bold("Houd je doel voor ogen")));
		document.addInFlow(document.createParagraph()
				.setMarginLeft(indent)
				.add("Bijvoorbeeld laten zien dat je de stof beheerst; een cijfer dat je wilt halen; " +
				     "een onvoldoende wegwerken; een bepaalde studie die je wilt gaan doen.")
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
				.add("Leer de dag voor de toets niks nieuws, maar ").add(underline("herhaal alles")).add(".")
				.add("\n\u00A0"));
		document.addInFlow(document.createParagraph()
				.add(bold("Bedenk hoe je de informatie het beste kunt leren")));
		document.addInFlow(document.createParagraph()
				.setMarginLeft(indent).add("Wat is voor jou de beste aanpak die aansluit bij hoe jij graag leert?")
				.add("\n\u00A0"));

		document.addInFlow(document.createParagraph().add(bold("\nHERHALEN, HERHALEN, HERHALEN")));
		document.addInFlow(document.createParagraph().add("""
						Je hebt het vast al héél vaak gehoord, maar herhaling van de stof die je moet leren is het \
						allerbelangrijkste. Wanneer je veel aandacht aan iets geeft, worden er verbindingen \
						aangelegd in je hersenen, waardoor je er steeds beter in wordt. In de eerste 20 minuten na \
						het leren, kan je al zo’n 40% vergeten. Dat is bijna de helft. Herhalen zorgt ervoor dat je \
						minder vergeet. Wanneer je dus veel aandacht aan iets geeft door het te herhalen, word je \
						er én\s""")
				.add(italic("beter"))
				.add(" in én ")
				.add(italic("onthoudt"))
				.add(" je het ")
				.add(italic("beter"))
				.add(". Snap je nu waarom het zo belangrijk is?")
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
				.add(bold("Hoe overleef ik het plannen?\n\u00A0").setFontSize(headingFontSize)));
		document.addInFlow(document.createParagraph().add("""
				In deze agenda kun je voor jezelf een overzicht creëren van je moet doen, wanneer het af moet zijn, \
				 en ook wanneer je het af gaat maken. Zo ben je goed georganiseerd."""));
		document.addInFlow(document.createParagraph()
				.add("Hieronder staan de stappen voor het maken en uitvoeren van een planning. ")
				.add("Deze stappen zijn belangrijk om in je achterhoofd te houden tijdens het plannen, lees ze " +
				     "daarom goed door.")
				.add("\n\u00A0"));

		document.addInFlow(document.createParagraph()
				.add(bold("\nStappenplan voor het maken van een planning:").setFontSize(headingFontSize))
				.add("\n\u00A0"));

		Image checkedCircleImage = document.loadSvgImageResource("/circle-checked.svg").setWidth(10).setHeight(10);

		List list = document.createList().setListSymbol(ListNumberingType.DECIMAL);
		list.setPostSymbolText(") ");
		list.add("Noteer toetsen en huiswerk in de planagenda.");
		list.add("Weet je niet wat het huiswerk of leerwerk is? Check de studiewijzer.");
		list.add("Geef toetsen en inlevermomenten een kleurtje (rood, roze, of geel).");
		list.add("Bekijk SOM een week vooruit. Zo weet je of je binnenkort een toets hebt.");
		list.add("Houd rekening met afspraken buiten school. Check hiervoor je tijdschema op blz. 18-20.\n" +
		         "Op dagen met veel afspraken kun je minder huiswerk maken.");
		list.add("Plan je huiswerk en toetsen in. Hak het in kleine stukjes (taken).");
		list.add("Dan ga je aan de slag. Nummer de taken. Begin bij de belangrijkste taak.");
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Taken die af zijn, vink je af met een check ").add(checkedCircleImage).add(".")));
		list.add("Heb je het aan het eind van de dag nog niet alles af? Plan deze taken opnieuw in.");
		document.addInFlow(list);
	}

	private void addPersonalGoals(WritableDocument document) {
		LOGGER.debug("Adding personal goals page");
		BiConsumer<Integer, Paragraph> titleConsumer = (i, par) -> par.add(
				bold("Wat zijn je doelen voor de komende periode?\n\n\u00A0").setFontSize(headingFontSize));
		addLinesPagesWithTitle(document, 1, titleConsumer);
	}

	private void addPlanningHand(WritableDocument document) throws IOException {
		LOGGER.debug("Adding instructions to plan by 'the hand'");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("\nDe hand-vragen:").setFontSize(headingFontSize))
				.add("\n\n")
				.add("\n\n")
				.add("\n\n")
				.add("\n\n")
				.add("\n\u00A0")
		);

		Rectangle pageArea = document.getEffectiveArea();
		Image handImage = new Image(document.loadPdfPageAsObject("/hand.pdf", 1));
		handImage.scaleToFit(pageArea.getWidth() / PHI, pageArea.getHeight() / PHI);
		// centered on the page, but then moved down 20mm
		float x = pageArea.getX() + (pageArea.getWidth() - handImage.getImageScaledWidth()) / 2f - mmToPt(25);
		float y = pageArea.getY() + (pageArea.getHeight() - handImage.getImageScaledHeight()) / 2f - mmToPt(10);
		handImage.setFixedPosition(x, y);
		document.addInFlow(handImage);

		document.addInFlow(document.createParagraph(headingFontSize)
				.add("Wat moet ik doen?")
				.setFixedPosition(x - mmToPt(1), y + mmToPt(63), mmToPt(25)));
		document.addInFlow(document.createParagraph(headingFontSize)
				.add("Waarom moet ik dat doen?\nWat kan ik ervan leren?")
				.setFixedPosition(x + mmToPt(10), y + mmToPt(110), mmToPt(35)));
		document.addInFlow(document.createParagraph(headingFontSize)
				.add("Wanneer moet het af zijn?")
				.setFixedPosition(x + mmToPt(55), y + mmToPt(120), mmToPt(40)));
		document.addInFlow(document.createParagraph(headingFontSize)
				.add("Wat heb ik nodig?")
				.setFixedPosition(x + mmToPt(75), y + mmToPt(108), mmToPt(50)));
		document.addInFlow(document.createParagraph(headingFontSize)
				.add("Wanneer ben ik klaar? Wanneer ben ik tevreden?")
				.setFixedPosition(x + mmToPt(90), y + mmToPt(85), mmToPt(60)));
	}

	private void addHowToSurviveLearning(WritableDocument document) {
		LOGGER.debug("Adding subject pages for planning");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("\nHoe overleef ik leren?").setFontSize(headingFontSize))
				.add("\n\u00A0"));

		List subjects = document.createList().setListSymbol(LIST_SYMBOL_BULLET);
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

		Table layoutTable = new Table(2).useAllAvailableWidth().setPadding(0).setMargin(0)
				.setMarginBottom(headingFontSize);
		Cell left = new Cell().setBorder(Border.NO_BORDER).setHorizontalAlignment(HorizontalAlignment.LEFT)
				.setVerticalAlignment(VerticalAlignment.TOP).setWidth(document.getEffectiveArea().getWidth() * 0.5f);
		Cell right = left.clone(false);

		left.add(document.createParagraph()
				.add(bold("\nHandige afkortingen").setFontSize(headingFontSize))
				.add("\n\u00A0"));

		Table table1 = new Table(2).setPadding(0).setMargin(0).setMarginBottom(headingFontSize)
				.setBorder(Border.NO_BORDER);
		table1.addHeaderCell(createCell0(document, "Vak")).addHeaderCell(createCell0(document, "Afkorting"));
		for (String cellText : java.util.List.of(
				"Nederlands", "Nl",
				"Engels", "En",
				"Frans", "Fr",
				"Duits", "Du",
				"Geschiedenis", "Gs",
				"Aardrijkskunde", "Ak",
				"Wiskunde", "Wi",
				"Biologie", "Bio",
				"Techniek", "Tech",
				"Levensbeschouwing / Maatschappijleer", "ML",
				"Sociale Vorming", "SV",
				"Lichamelijke Opvoeding", "LO",
				"Handvaardigheid", "Hv",
				"Muziek", "Mu",
				"Tekenen", "Te",
				"Digitale Geletterdheid", "DG"
		)) {
			table1.addCell(createCell0(document, cellText).setPaddingRight(headingFontSize));
		}
		left.add(table1);//.addInFlow(document.createParagraph().add("\u00A0"));

		right.add(document.createParagraph()
				.add(bold("\nHuiswerk Noteren").setFontSize(headingFontSize))
				.add("\n\u00A0"));

		Table table2 = new Table(createPercentArray(new float[]{3, 2})).setPadding(0).setMargin(0)
				.setMarginBottom(headingFontSize)
				.setBorder(Border.NO_BORDER);
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
			table2.addCell(createCell0(document, cellText).setPaddingRight(headingFontSize));
		}
		right.add(table2);//.addInFlow(document.createParagraph().add("\u00A0"));

		document.addInFlow(layoutTable.addCell(left).addCell(right));

		document.addInFlow(document.createParagraph()
				.add(bold("\nSlimme notities").setFontSize(headingFontSize))
				.add("\n\u00A0"));

		Table table3 = new Table(createPercentArray(new float[]{2, 3}))
				.useAllAvailableWidth().setFixedLayout()
				.setPadding(0).setMargin(0).setMarginBottom(headingFontSize).setBorder(Border.NO_BORDER);
		table3.addHeaderCell(createCell(document, "Wat"))
				.addHeaderCell(createCell(document, "Gebruikersnaam (géén wachtwoord!) / e-mail"));
		// noinspection ExtractMethodRecommender
		java.util.List<Map.Entry<String, String>> usefulLinks = java.util.List.of(
				Map.entry("SomToday (agenda)", "https://somtoday.nl/"),
				Map.entry("Zermelo (rooster)", "https://candea.zportal.nl/"),
				Map.entry("Classroom", "https://classroom.google.com/"),
				Map.entry("Schoolmail", "https://outlook.office365.com/"),
				Map.entry("E-mail mentor 1", ""),
				Map.entry("E-mail mentor 2", ""),
				Map.entry("Kluisnummer", ""),
				Map.entry("Vertrouwenspersoon 1", ""),
				Map.entry("Vertrouwenspersoon 2", "")
		);
		usefulLinks.forEach(entry ->
				table3.addCell(createCell(document, 1, LEFT, p -> {
					p.add(entry.getKey() + "\n");
					String link = entry.getValue();
					if (link.isEmpty()) {
						p.add("\u00A0");
					} else {
						p.add(new Link(link, PdfAction.createURI(link)));
					}
				})).addCell(emptyCell(document)));
		for (int i = 0; i < 10 - usefulLinks.size(); i++) {
			table3.addCell(createCell(document, "\n\u00A0")).addCell(emptyCell(document));
		}
		document.addInFlow(table3);
	}

	private void addStudyingTips(WritableDocument document) {
		LOGGER.debug("Adding studying tips");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Studietips\n\u00A0").setFontSize(headingFontSize)).setTextAlignment(CENTER));

		float pageWidth = document.getEffectiveArea().getWidth();
		Paragraph blockWithLines = document.createParagraph(null, headingFontSize)
				.addTabStops(new TabStop(pageWidth, TabAlignment.LEFT, new SolidLine(.75f)))
				.add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n")
				// .add("\n")
				;

		document.addInFlow(
				document.createParagraph().add(bold("Tips voor het leren:").setFontSize(smallHeadingFontSize)));

		document.addInFlow(document.createParagraph().add(
				"Hoe zorg je dat je niet wordt afgeleid tijdens het maken van je huiswerk of het leren " +
				"van toetsen?")
		).addInFlow(blockWithLines);
		document.addInFlow(document.createParagraph()
				.add("\nHoe kom je erachter wat je precies moet maken of leren?")
		).addInFlow(blockWithLines);
		document.addInFlow(document.createParagraph()
				.add("\nWat is voor jou een fijne plek in het huis om je huiswerk te maken of te leren?")
		).addInFlow(blockWithLines);
		document.addInFlow(document.createParagraph()
				.add("\nWat is voor jou een fijn moment op de dag om je huiswerk te maken?")
		).addInFlow(blockWithLines);
		document.addInFlow(document.createParagraph()
				.add("\nWat is voor jou een fijn moment om je tas in te pakken?")
		).addInFlow(blockWithLines);
		document.addInFlow(document.createParagraph()
				.add("\nWat ga je anders doen dan met de vorige agenda? En wat ga je hetzelfde doen? Heb je nog tips " +
				     "voor jezelf?")
		).addInFlow(blockWithLines);
	}

	private void addHowToLearn(WritableDocument document) throws IOException {
		LOGGER.debug("Adding how to learn");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Voordat je begint met leren:\n\u00A0").setFontSize(headingFontSize))
				.setTextAlignment(CENTER));

		Image heroBrainImage = new Image(ImageDataFactory.create(
				requireNonNull(getClass().getResource("/Heldenbrein.jpg"))))
				// .setMarginLeft(mmToPt(5))
				// .setMarginBottom(mmToPt(5))
				.scaleToFit(mmToPt(35), mmToPt(35));
		heroBrainImage.setProperty(Property.FLOAT, FloatPropertyValue.RIGHT);
		document.addInFlow(heroBrainImage);

		List list = document.createList().setListSymbol(LIST_SYMBOL_BULLET);
		list.add("Probeer niet alles in 1x te leren, deel het leerwerk daarom op in kleine stukjes.");
		list.add("Hoe vaker je “traint”, hoe sterker de verbindingen in je hersenen worden.");
		list.add("""
				Je kent de leerstof pas echt goed als je de stof zonder je boek of aantekeningen erbij kan \
				opschrijven/uitspreken/vertellen. Bijvoorbeeld wanneer je wordt overhoord door iemand anders of het \
				aan een medeleerling uitlegt.""");
		list.add("""
				Het duurt ongeveer 15 minuten voor je hersenen in de leermodus zijn. Zorg daarom dat je tijdens het \
				leren niet afgeleid wordt. Leg je telefoon in een andere kamer of zet je computer uit.""");
		list.add("""
				Wanneer je slaapt verwerken je hersenen alle nieuwe dingen die je hebt geleerd. Genoeg slapen is dus \
				belangrijk.""");
		document.addInFlow(list);

		document.addInFlow(document.createParagraph());
		document.addInFlow(document.createParagraph().add(bold("De voorbereiding:").setFontSize(smallHeadingFontSize)));
		document.addInFlow(document.createParagraph().add("""
				Een goede voorbereiding zort ervoor dat het leren makkelijker gaat. Dat doe je door precies te kijken \
				wat je moet leren. Dat gaat zo:"""));
		list = document.createList().setListSymbol(LIST_SYMBOL_BULLET);
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
						Als je in je werkboek kijkt, staat daar vaak wat je moet kennen en kunnen. Lees dit door en kijk voor \
						jezelf of je dit allemaal kent en kunt. Zo niet, dan moet je daar meer voor voorbereiden.
						Voor""").add(bold(" aardrijkskunde ")).add("""
						staan de leerdoelen in een extra paragraaf.
						Voor""").add(bold(" geschiedenis ")).add("""
						staat achter elke paragraaf een grijs blok met tips voor de toets.
						Voor""").add(bold(" biologie")).add(", ").add(bold("natuur-")).add(" en ")
				.add(bold("scheikunde ")).add("""
						staat aan het begin van elke paragraaf een kopje met leerdoelen.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Bij vakken met veel tekst (bijvoorbeeld geschiedenis), heeft ieder hoofdstuk een hoofdvraag en iedere \
				paragraaf heeft een deelvraag. Kun je antwoord geven op deze vragen? Zo niet, lees dan alles nogmaals \
				goed door.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Lees de tekst per alinea en schrijf in 1 of 2 zinnen op waar het stukje over gaat. \
				Niet langer dan dit! Dit is het begin van een korte""").add(italic(" samenvatting."))));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Schrijf de dikgedrukte woorden over en schrijf de betekenis erachter.")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Bekijk alle bronnen goed (ook tekeningen, kaarten, afbeeldingen). Kun je deze in je eigen woorden \
				uitleggen?""")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Voor geschiedenis: leer alle jaartallen die voorin het hoofdstuk staan, of je docent moet het \
				anders aangeven.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Voor geschiedenis: kijk ook naar de voorgaande hoofdstukken. Welke tijdvakken kwamen ervoor en hoe \
				heette de periodes?""")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Pak je werkboek er ook bij. Als het goed is heb je alles gemaakt EN nagekeken. Lees de \
				opdrachten nog een door en kijk vooral goed naar de opdrachten die je fout had.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Kijk ook nog eens naar de opdrachten in je werkboek waarbij je bronnen moet gebruiken.")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Zijn er aantekeningen gemaakt? Leer deze dan ook.""")));
		document.addInFlow(list);

		document.addInFlow(document.createParagraph());
		document.addInFlow(document.createParagraph().add(bold("Maakwerk:").setFontSize(smallHeadingFontSize)));
		list = document.createList().setListSymbol(LIST_SYMBOL_BULLET);
		list.add("Je maakwerk moet je ook nakijken, zodat je weet of je de opdrachten goed hebt gedaan en snapt.");
		list.add("Als je gaat leren, kijk dan ook naar de opdrachten in je werkboek om te checken wat je fout had " +
		         "gedaan, blijkbaar vond je dat moeilijk.");
		list.add("Als opdrachten in de les zijn besproken dan is het ook handig om dat in je werkboek met een " +
		         "uitroepteken aan te geven. Blijkbaar was dat een belangrijke vraag/vaardigheid.");
		document.addInFlow(list);

		document.addInFlow(document.createParagraph());
		document.addInFlow(document.createParagraph()
				.add(bold("Verschillende manieren om grote teksten te leren:").setFontSize(smallHeadingFontSize)));

		document.addInFlow(document.createParagraph());
		document.addInFlow(document.createParagraph().add(bold("Een Mindmap maken:")));

		Image mindmapImage = new Image(ImageDataFactory.create(
				requireNonNull(getClass().getResource("/MindMap.png"))))
				.scaleToFit(document.getEffectiveArea().getWidth() * 0.45f, mmToPt(100));
		mindmapImage.setProperty(Property.FLOAT, FloatPropertyValue.RIGHT);
		document.addInFlow(mindmapImage);

		document.addInFlow(document.createParagraph().add("""
				Een Mindmap maak je om veel begrippen en hun samenhang te leren. Iedere mindmap heeft een centraal \
				thema. Zo maak je er een:"""));
		list = document.createList().setListSymbol(ListNumberingType.DECIMAL);
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Kies je hoofdthema. Bijvoorbeeld het onderwerp van de paragraaf")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Voeg subthema's toe.")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Vul details in.")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Gebruik kleuren en afbeeldingen (zie plaatje).")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Maak verbindingen / meer zijtakken.")));
		document.addInFlow(list);

		document.addInFlow(document.createParagraph());
		document.addInFlow(document.createParagraph().add(bold("Samenvatten van een tekst:")));
		document.addInFlow(document.createParagraph().add("""
				Een samenvatting maak je van grote teksten als een mindmap niet zo handig is."""));
		document.addInFlow(document.createParagraph().add(bold("Tip: ")).add("""
				Om een goede samenvatting te kunnen schrijven is het belangrijk om te weten wat hoofd- en bijzaken \
				zijn. In een goede samenvatting staan namelijk alleen maar hoofdzaken. De belangrijkste informatie in \
				een tekst die wordt gegeven over het onderwerp noemen we hoofdzaken. Minder belangrijke informatie, \
				zoals voorbeelden, noemen we bijzaken."""));
		document.addInFlow(document.createParagraph().add(bold("Tip: ")).add("""
				Gebruik je eigen woorden. Ga dus niet telkens de belangrijkste zin uit de alinea letterlijk \
				overschrijven. Als je het in je eigen woorden formuleert, kun je het makkelijker onthouden."""));
		document.addInFlow(document.createParagraph().add(bold("Tip: ")).add("""
				Maak gebruik van de leerdoelen en tips uit je voorbereiding. Zorg ervoor dat je deze \
				leerdoelen goed kunt beantwoorden. Laat ze terugkomen in je samenvatting."""));

		document.addInFlow(document.createParagraph().add("Zo maak je een samenvatting:"));
		list = document.createList().setListSymbol(ListNumberingType.DECIMAL);
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Markeer de belangrijkste zinnen (of streep ze aan). Loop eerst de belangrijkste onderdelen van je \
				tekst na: inleiding en slot.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Verzamel de 1 of 2 zinnen die je per alinea hebt opgeschreven (zie voorbereiding).")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Orden je informatie.")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Schrijf de samenvatting.")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Check of je samenvatting alle hoofd- en deelvragen uit de tekst beantwoord (zie voorbereiding).")));
		document.addInFlow(list);

		document.addInFlow(document.createParagraph().add(bold("Andere methodes om te leren:")));
		list = document.createList().setListSymbol(LIST_SYMBOL_BULLET);
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Cornell-methode\n")).add("""
						De Cornell-methode is een manier om hoofd- en bijzaken te scheiden, en helpt bij het maken van \
						aantekeningen en samenvattingen. Op internet of in de mentorles leer je hier meer over.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Braindump (leren leren)\n")).add("""
						Braindump is een actieve leerstrategie, die helpt om te leren en om voorkennis te activeren. \
						Op internet of in de mentorles leer je hier meer over.
						""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Vragen maken\n")).add("""
						Maak voor jezelf vragen die je een paar dagen van tevoren opschrijft. Schrijf op een apart \
						blad de antwoorden en kijk of je ze na een paar dagen nog weet te beantwoorden. Dit kun je \
						ook met andere klasgenootjes doen.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Filmpjes en afbeeldingen\n")).add("""
						Zoek op internet filmpjes of afbeeldingen die je misschien kunt gebruiken. Maak er eventueel \
						vragen bij voor jezelf. Op de website van SchoolTV kun je veel informatie over veel \
						onderwerpen vinden en ook op YouTube.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Atlas\n")).add("""
						Voor aardrijkskunde kun je ook nog eens door de atlas zoeken naar kaarten die met het \
						onderwerp te maken hebben. Kun je de dingen die je geleerd hebt toepassen op dat kaartje? \
						Wat zie je allemaal, en waar heeft het mee te maken?""")));
		document.addInFlow(list);

		document.addInFlow(document.createParagraph());
		document.addInFlow(document.createParagraph()
				.add(bold("Manieren om begrippen en/of woordjes te leren (o.a. talen):").setFontSize(
						smallHeadingFontSize)));
		list = document.createList().setListSymbol(LIST_SYMBOL_BULLET);
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Overschrijven\n")).add("""
						Schrijf de woorden meerdere keren over, zo oefen je de spelling van alle woorden goed.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Flashcards\n")).add("""
						Maak kaartjes van alle woorden die je moet leren met het Nederlands aan een kant, en de andere taal \
						aan de andere kant (dit werkt ook voor begrippen en hun betekenis). \
						Hussel alle kaartjes door elkaar en bekijk de kaartjes 1 voor 1. Bedenk per kaartje wat het woord op \
						de achterkant is. Je draait het kaartje om en je checkt of je het antwoord goed had. Maak stapeltjes \
						wist ik/wist ik nog niet. Herhaal alle woorden die je nog niet wist nog een aantal keer. \
						Om de flashcards een stapje moeilijker te maken kan je ook het woord dat op de achterkant staat \
						opschrijven op een blaadje. Zo oefen je meteen met het spellen van de woorden.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Memory\n")).add("""
						Maak kaartjes van alle woorden die je moet leren in het Nederlands en de andere taal (dit werkt ook \
						voor begrippen en hun betekenis). Hussel alle kaartjes en leg ze met de tekst naar beneden. Draai \
						iedere keer 2 kaartjes om, wanneer je een match hebt haal je de kaartjes uit het spel.""")));
		Image rightArrowImage = document.loadSvgImageResource("/rightArrow.svg").setWidth(12).setHeight(8);
		// noinspection SpellCheckingInspection
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Digitaal overhoorprogramma"))
				.add(" (bv Slim stampen/Quizlet/Wozzol/Teach2000/blooket)\n").add("""
						Er zijn online veel overhoorprogramma’s te vinden. Zorg ervoor dat je altijd alle moeilijkheidsgraden \
						doorloopt:\s""")
				.add("onthouden ").add(rightArrowImage).add(" meerkeuze ").add(rightArrowImage).add(" spellen.")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Leer groepjes met hetzelfde thema tegelijk\n")).add("""
						Wanneer je woorden groepeert die hetzelfde thema hebben, is het makkelijker om ze te onthouden.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Voorbeeldzinnen verzinnen\n")).add("""
						Verzin bij alle woorden uit de woordenlijst een nieuwe zin (in die taal) waaruit de betekenis van \
						het woord blijkt.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Plaatjes/ezelsbruggetjes erbij verzinnen\n")).add("""
						Probeer om ezelsbruggetjes of plaatjes te verzinnen bij de woordenlijst die je moet leren.""")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Laten overhoren door iemand anders\n")).add("""
						Laat je door iemand die je kent overhoren. Dit kan mondeling, maar het is ook verstandig om de \
						woorden op te schrijven. Zo oefen je ook de spelling van de woorden.


						"""))); // Let op: deze lege regels laten het volgende punt niet afbreken over het pagina-einde
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add(bold("Afdekmethode\n")).add("""
						Dek de woordjes af met een blaadje. Bedenk in je hoofd wat de vertaling is, verschuif het papiertje \
						en kijk of je het antwoord goed had. Je kan ook de woorden opschrijven en de spelling controleren.""")));
		document.addInFlow(list);

		document.addInFlow(document.createParagraph());
		document.addInFlow(
				document.createParagraph().add(bold("Specifieke tips voor vakken:").setFontSize(smallHeadingFontSize)));
		document.addInFlow(document.createParagraph().add(bold("OSA:")));
		document.addInFlow(document.createParagraph().add("""
				Osa is geen gemakkelijk vak om te leren. Je krijgt heel veel informatie in een les en niet altijd \
				letterlijke vragen die in de tekst staan tijdens een toets. Er wordt namelijk verwacht van je dat je \
				de informatie kunt toepassen. Dit kan lastig zijn. Alleen doorlezen is in ieder geval niet voldoende."""));
		document.addInFlow(document.createParagraph().add(bold("Wiskunde:")));
		document.addInFlow(document.createParagraph().add("""
				Wiskunde is een vak waar je niet altijd voor kunt leren. Soms zijn er begrippen die je moet kennen, \
				deze kun je wel leren."""));
		document.addInFlow(document.createParagraph().add("Belangrijke dingen om te doen voor OSA en wiskunde zijn:"));
		list = document.createList().setListSymbol(LIST_SYMBOL_BULLET);
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Maak aantekeningen en stel vragen in de les.")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Maak een samenvatting van de stof.")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Maak oefentoetsen (zie het overzicht verderop).")));
		list.add((ListItem) new ListItem().add(document.createParagraph()
				.add("Diagnostische toets en/of de Herhaling-opdrachten in je boek maken + nakijken.")));
		list.add((ListItem) new ListItem().add(document.createParagraph().add("""
				Check dat de uitwerkingen van je maakwerk zijn nagekeken en correct zijn (verbeterd). Zo niet, check \
				bij je docent of dat het klopt. Dit is belangrijk, omdat je anders alleen een invuloefening maakt. \
				En dan kun je NIET leren, want je weet niet wat je moet leren.""")));
		document.addInFlow(list);

		document.addInFlow(document.createParagraph());
		document.addInFlow(document.createParagraph().add(bold("Handige websites:").setFontSize(smallHeadingFontSize)));
		java.util.List<String> usefulWebsitesTitles = java.util.List.of("Algemeen",
				"Frans", "Engels", "Duits", "Science", "Nederlands", "Wiskunde", "OSA");
		// noinspection SpellCheckingInspection
		java.util.List<java.util.List<Map.Entry<String, String>>> usefulWebsites = java.util.List.of(
				java.util.List.of( // Algemeen
						Map.entry("https://studygo.nl/", ""),
						Map.entry("https://quizlet.com/", ""),
						Map.entry("https://www.wozzol.nl/", ""),
						Map.entry("https://www.teach.nl/", "")
				), java.util.List.of( // Frans
						Map.entry("https://www.verbuga.eu/Mise/Mise.html", " (werkwoorden)")
				), java.util.List.of( // Engels
						Map.entry("https://readtheory.org/", "")
				), java.util.List.of( // Duits
						Map.entry("https://www.duits.de/", ""),
						Map.entry("https://www.nubeterduits.nl/website/index.php?pag=1", ""),
						Map.entry("https://deutsch-lernen.zum.de/wiki/Handlungsfelder", "")
				), java.util.List.of( // Science
						Map.entry("https://biologiepagina.nl/", " (bio)"),
						Map.entry("", "Youtube: meneer wiersma (nask)"),
						Map.entry("https://www.reken-taal.be/rekenen/conversies.htm",
								": om te oefenen met eenheden en omrekenen")
				), java.util.List.of( // Nederlands
						Map.entry("https://www.cambiumned.nl/", "")
				), java.util.List.of( // Wiskunde
						Map.entry("", "In de digitale omgeving van je boek kan je ook uitlegfilmpjes vinden"),
						Map.entry("", "Youtube: Math with Menno"),
						Map.entry("", "Youtube: Wiskundeacademie"),
						Map.entry("https://www.reken-taal.be/rekenen/conversies.htm",
								": om te oefenen met eenheden en omrekenen")
				), java.util.List.of( // OSA
						Map.entry("", "Youtube: aardrijkskunde kennisclips"),
						Map.entry("https://www.schooltv.nl/", ": histoclips over verschillende historische " +
						                                      "onderwerpen en de serie ‘Welkom in…’")
				));
		for (int i = 0; i < usefulWebsitesTitles.size(); i++) {
			String title = usefulWebsitesTitles.get(i);
			java.util.List<Map.Entry<String, String>> websites = usefulWebsites.get(i);
			document.addInFlow(document.createParagraph());
			document.addInFlow(document.createParagraph().add(bold(title + ":")));
			list = document.createList().setListSymbol(LIST_SYMBOL_BULLET);
			for (Map.Entry<String, String> entry : websites) {
				String url = entry.getKey();
				Text link = url.isEmpty() ? new Text("") : new Link(url, PdfAction.createURI(url));
				String text = entry.getValue();
				list.add((ListItem) new ListItem().add(document.createParagraph().add(link).add(text)));
			}
			document.addInFlow(list);
		}

		document.addInFlow(document.createParagraph());
		document.addInFlow(
				document.createParagraph().add(bold("Maken van oefentoetsen:").setFontSize(smallHeadingFontSize)));
		document.addInFlow(document.createParagraph().add("Waar kun je de oefentoetsen per vak vinden:"));
		Table table = new Table(createPercentArray(5)) // new float[]{2, 3}))
				.useAllAvailableWidth().setFixedLayout()
				.setPadding(0).setMargin(0).setBorder(Border.NO_BORDER);
		table.addHeaderCell(createCell(document, "Vak"))
				.addHeaderCell(createCell(document, "Classroom"))
				.addHeaderCell(createCell(document, "Digitale methode"))
				.addHeaderCell(createCell(document, "Boek"))
				.addHeaderCell(createCell(document, "Overig (graag benoemen)"));
		// noinspection SpellCheckingInspection
		for (String text : java.util.List.of(
				"Nederlands", "x", "", "", "",
				"Engels", "x", "", "x", "",
				"Frans", "", "x (le bilan)", "", "",
				"Duits", "x (soms als extra)", "x", "", "",
				"Science (bio)", "", "x", "x", "https://biologiepagina.nl/",
				"Science (nask)", "", "x", "x", "",
				"OSA (ak)", "", "x", "", "",
				"OSA (ges)", "", "x", "x", "",
				"Wiskunde", "x", "x", "x", ""
		)) {
			table.addCell(createCell(document, 1, TextAlignment.LEFT, p -> {
				if (text.isEmpty()) {
					p.add("\u00A0");
				} else if (text.startsWith("http")) {
					p.add(new Link(text, PdfAction.createURI(text)));
				} else {
					p.add(text);
				}
			}));
		}
		document.addInFlow(table);
	}

	private void addHowToSurviveTheFreshmanYear(WritableDocument document) {
		LOGGER.debug("Adding guide to survive as a freshman");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Hoe overleef ik de brugklas\n\u00A0").setFontSize(headingFontSize))
				.setTextAlignment(CENTER));

		// First heading: do not start with a newline (for the rest: do)
		document.addInFlow(document.createParagraph().add(bold("Wat moet ik doen als ik te laat kom?")));
		document.addInFlow(document.createParagraph().add("""
				Als je te laat op school bent of te laat voor een les, haal je altijd eerst een telaatbriefje bij de \
				conciërges. Met zo'n briefje mag jij de les in, of je nu geoorloofd te laat was of niet. \
				"""));

		document.addInFlow(
				document.createParagraph().add(bold("\nWat moet ik doen als ik naar de orthodontist/dokter moet?")));
		document.addInFlow(document.createParagraph().add("""
				Je ouders moeten voor de afspraak aan school laten weten welk lesuur je er niet bent. \
				Dat kan door te bellen, of ze geven je een briefje voor de conciërges mee met daarin waarom je welk \
				lesuur niet aanwezig kunt zijn. \
				Dat briefje moet je dan voor de afspraak aan een van de conciërges geven. \
				"""));

		document.addInFlow(document.createParagraph().add(bold("\nWat moet ik doen als ik ziek ben?")));
		document.addInFlow(document.createParagraph().add("""
				Als je ziek bent, dan bellen je ouders voor het eerste lesuur naar school om je ziek te melden.
				\u00A0"""));

		document.addInFlow(document.createParagraph().add(bold("Wat moet ik doen als ik ziek naar huis wil?")));
		document.addInFlow(document.createParagraph().add("""
				Wanneer je je tijdens schooltijd opeens niet lekker voelt, dan ga je naar je docent of mentor en geef \
				je aan dat je naar huis wilt, je meldt je daarna af bij de conciërges. \
				Als je thuis bent, laat je je ouders naar school bellen dat je weer veilig thuis bent. \
				"""));

		document.addInFlow(document.createParagraph().add(bold("\nWat moet ik doen als ik een toets gemist heb?")));
		document.addInFlow(document.createParagraph().add("""
				Je geeft bij je vakdocent aan dat je een toets gemist hebt en vraagt de vakdocent wanneer je de toets \
				kunt inhalen. Je vakdocent zorgt dat er een inhaaltoets voor je klaar ligt in de mediatheek."""));

		document.addInFlow(document.createParagraph().add(bold("\nNeem ik mijn jas en gymtas mee naar het lokaal?")));
		document.addInFlow(document.createParagraph().add("""
				Op school heeft elke leerling een eigen kluisje. Hierin kun je je gymtas, je jas en je telefoon \
				bewaren. Tijdens de gymles bewaar je je computer en je telefoon in je kluisje. \
				"""));

		document.addInFlow(
				document.createParagraph().add(bold("\nZijn alle gymlessen in de gymzalen?")));
		document.addInFlow(document.createParagraph().add("""
				Nee, vanaf april tot aan de herfstvakantie gymmen we buiten op de sportvelden en de atletiekbaan, \
				op twee minuten van de school. \
				"""));

		document.addInFlow(document.createParagraph().add(bold("\nGebruikt elke leerling een computer?")));
		document.addInFlow(document.createParagraph().add("""
				Ja, op school gebruiken we een computer ter ondersteuning van het onderwijs; we gebruiken dus \
				ook boeken. Een computer lijkt op een laptop. \
				Vanaf de projectweek zullen we ze in de lessen gaan gebruiken en moet je dus altijd een opgeladen \
				computer bij je hebben. \
				"""));

		document.addInFlow(document.createParagraph().add(bold("\nMag je je telefoon gebruiken tijdens de les?")));
		document.addInFlow(document.createParagraph().add("""
				Nee, tijdens de lessen bewaar je je telefoon, terwijl die uit is, in je kluis. \
				Een telefoon leidt immers veel te veel af van wat er tijdens de les gebeurt. \
				Als je internet nodig hebt voor een schoolopdracht, gebruik je je computer. \
				"""));

		document.addInFlow(document.createParagraph().add(bold("\nWaarom geven we huiswerk?")));
		document.addInFlow(document.createParagraph().add("""
				We geven huiswerk omdat je dan nog eens rustig kunt oefenen met de stof die in de les is behandeld. \
				Of je maakt juist huiswerk om je voor te bereiden op de les die komen gaat. \
				Reken op één à anderhalf uur per dag (ook in het weekend). \
				Als je goed meedoet tijdens de les, scheelt dat in wat je thuis moet doen. Op school kun je bovendien \
				de docent of medeleerlingen om hulp vragen. Het is vooral handig om leerwerk rustig thuis te doen. \
				"""));

		document.addInFlow(document.createParagraph().add(bold("\nWat zijn de afspraken in de leszone?")));
		document.addInFlow(document.createParagraph().add(
				"Zodra je door de klapdeuren een leszone in loopt, gelden de volgende afspraken:"));
		List learningZoneRules = document.createList().setListSymbol(LIST_SYMBOL_HYPHEN_BULLET);
		learningZoneRules.add("Je praat zachtjes en loopt rustig.");
		learningZoneRules.add("Je hebt geen jas aan (deze ligt al in je kluis).");
		learningZoneRules.add("Je hebt je telefoon niet bij je (deze ligt al in je kluis).");
		learningZoneRules.add("Je eet en drinkt alleen buiten leszone.");
		document.addInFlow(learningZoneRules);
	}

	private void addGradeList(WritableDocument document) {
		LOGGER.debug("Adding grade list");
		document.startNewPage(false);
		document.addInFlow(document.createParagraph()
				.add(bold("Cijferlijst:\n").setFontSize(headingFontSize)));
		document.addInFlow(document.createParagraph());
		document.addInFlow(document.createParagraph());

		float gradeSize = mmToPt(10);
		float headerPaddingTop = 4;
		float headerPaddingBottom = 1;
		float cellPadding = 11;
		UnitValue[] columnWidths = new UnitValue[]{
				UnitValue.createPointValue(mmToPt(50)),
				// UnitValue.createPointValue(gradeSize),
				// UnitValue.createPointValue(gradeSize),
				UnitValue.createPointValue(gradeSize),
				UnitValue.createPointValue(gradeSize),
				UnitValue.createPointValue(gradeSize),
				UnitValue.createPointValue(gradeSize),
				UnitValue.createPointValue(gradeSize),
				UnitValue.createPointValue(gradeSize),
				UnitValue.createPointValue(gradeSize),
				UnitValue.createPointValue(gradeSize),
				UnitValue.createPointValue(mmToPt(40))
		};
		Table table = new Table(columnWidths).useAllAvailableWidth().setFixedLayout().setPadding(0).setMargin(0)
				.setMarginBottom(headingFontSize);
		table.addCell(document.createParagraph().add(bold("Vak"))
				.setPaddings(headerPaddingTop, 0, headerPaddingBottom, 0));
		for (int c = 0; c < columnWidths.length - 2; c++) {
			table.addCell(emptyCell(document).setPaddingBottom(cellPadding));
		}
		table.addCell(document.createParagraph().add(bold("Eindcijfer"))
				.setPaddings(headerPaddingTop, 0, headerPaddingBottom, 0));

		for (int r = 0; r < 20; r++) {
			for (int c = 0; c < columnWidths.length; c++) {
				table.addCell(emptyCell(document).setPaddingBottom(cellPadding));
			}
		}
		document.addInFlow(table);
	}

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
				.add(bold("Tijdschema:\n").setFontSize(headingFontSize)));
		document.addInFlow(document.createParagraph()
				.add("Hoe is jouw week gevuld met school, hobby’s en sporten? Vul dit hieronder in. Geef elk " +
				     "tijdsblok een ander kleurtje."));

		UnitValue[] columnWidths = createPercentArray(8);
		Table table = new Table(columnWidths).useAllAvailableWidth().setFixedLayout().setPadding(0).setMargin(0)
				.setMarginBottom(headingFontSize);
		LocalTime startOfDay = LocalTime.of(8, 0);
		LocalTime endOfDay = LocalTime.of(23, 0);
		table.addCell(emptyCell(document));
		for (DayOfWeek value : DayOfWeek.values()) {
			table.addCell(capitalize(WEEKDAY_FORMAT.format(value)));
		}
		for (LocalTime time = startOfDay; !time.isAfter(endOfDay); time = time.plusMinutes(30)) {
			table.addCell(
					new Cell().add(document.createParagraph().add(time.format(TIME_FORMAT)).setMultipliedLeading(1.15f))
							.setTextAlignment(RIGHT).setPaddings(3, 6, -3, 0));
			for (int c = 1; c < columnWidths.length; c++) {
				table.addCell(emptyCell(document));
			}
		}
		document.addInFlow(table);
	}

	private static @NotNull String capitalize(String text) {
		int[] codePoints = text.codePoints().toArray();
		codePoints[0] = Character.toTitleCase(codePoints[0]);
		return new String(codePoints, 0, codePoints.length);
	}

	private void addPlanningWeeks(WritableDocument document, int numClassSlots,
	                              NavigableMap<LocalDate, String> dateTitles)
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
		for (LocalDate monday = dateTitles.firstKey(); !monday.isAfter(
				lastDateThatMustBePresent); monday = monday.plusWeeks(1)) {
			int extraPadding = 9;
			LOGGER.debug("Adding planning week starting on {}", monday);

			ClassItemStructure classItemStructure = plannerDescription.classItemStructure();
			int numberOfWeeks = (int) WEEKS.between(dateTitles.firstKey(), monday);
			if (classItemStructure == null) {
				ClassItemStructure[] choices = new ClassItemStructure[]{ClassItemStructure.CLASS_ROOM_SINGLE,
						ClassItemStructure.CLASS_ROOM_DOUBLE,
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

			Table table = new Table(columnWidths).useAllAvailableWidth().setPadding(0).setMargin(0)
					.setMarginBottom(headingFontSize)
					.setFixedLayout();
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
				table.addCell(createCell(document, 1, RIGHT,
						p -> p.add("\n\n\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
			}
			document.addInFlow(table);

			// Right page: Thursday to Sunday

			document.startNewPage(false);
			document.addInFlow(document.createParagraph().add("\u00A0"));

			table = new Table(columnWidths).useAllAvailableWidth().setPadding(0).setMargin(0)
					.setMarginBottom(headingFontSize)
					.setFixedLayout();
			for (int c = 0; c < 3; c++) {
				// Thursday to Saturday
				table.addCell(createDateCellWithText(document, dateTitles, columnWidth, monday.plusDays(c + 3)));
			}
			for (int r = 0; r < numClassSlots; r++) {
				table.addCell(getCreateClassCell(document, 2, extraPadding, classItemStructure, r + 1));
				table.addCell(getCreateClassCell(document, 2, extraPadding, classItemStructure, r + 1));
				// These planning cells are less high than the others, but will grow to the same size as the numbered
				// cells.
				// The other two sets of planning sets must create their own height correctly.
				if (r == 0) {
					table.addCell(createCell(document, 1, "Planning"));
					table.addCell(createCell(document, 1, RIGHT,
							p -> p.add("\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				} else {
					table.addCell(createCell(document, 2, RIGHT,
							p -> p.add("\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				}
			}
			table.addCell(createCell(document, 1, "Planning"));
			table.addCell(createCell(document, 1, "Planning"));
			// Sunday
			table.addCell(createDateCellWithText(document, dateTitles, columnWidth, monday.plusDays(6)));
			for (int r = 0; r < numPlanningSlots; r++) {
				table.addCell(createCell(document, 2, RIGHT,
						p -> p.add("\n\n\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				table.addCell(createCell(document, 2, RIGHT,
						p -> p.add("\n\n\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				if (r == 0) {
					table.addCell(createCell(document, 1, "Planning"));
					table.addCell(createCell(document, 1, RIGHT,
							p -> p.add("\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				} else {
					table.addCell(createCell(document, 2, RIGHT,
							p -> p.add("\n\u00A0").add(emptyCircleImage).add("\u00A0")).setVerticalAlignment(BOTTOM));
				}
			}
			document.addInFlow(table);
		}
	}

	private Cell getCreateClassCell(WritableDocument document, int rowspan, int extraPadding,
	                                ClassItemStructure classItemStructure, int classHour) {
		float extraPadding1 = extraPadding + document.getFontSize() * 2f;
		float extraPadding2 = ((extraPadding + document.getFontSize()) / 2f) - 0.5f;
		float extraPadding3 = (extraPadding / 3f) - 1f; // magic :(
		float extraPadding4 = extraPadding3 - 0.577f;
		if (classItemStructure == ClassItemStructure.SINGLE_FIELD) {
			return createCell(document, rowspan, classHour + "\n\n\u00A0").setPaddingBottom(extraPadding);
		}
		Table t =
				new Table(createPercentArray(new float[]{1, 3})).useAllAvailableWidth().setFixedLayout().setPadding(0)
						.setMargin(0)
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

	private Cell createDateCellWithText(WritableDocument document, NavigableMap<LocalDate, String> dateTitles,
	                                    float columnWidth, LocalDate date) {
		String dayText = Optional.ofNullable(dateTitles.floorEntry(date).getValue()).filter(s -> !s.isEmpty())
				.orElse("\u00A0");
		return createCell(document, 1, TextAlignment.LEFT,
				p -> p.add(date.format(DAY_FORMAT) + "\n").add(text(document, columnWidth, "…", dayText)));
	}

	private Text text(WritableDocument document, float width,
	                  @SuppressWarnings("SameParameterValue") String truncatedTextSuffix, String textToFit) {
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
		return createCell(document, rowspan, TextAlignment.LEFT,
				paragraph -> paragraph.add(requireNonNullElse(text, "\u00A0")));
	}

	private Cell createCell0(WritableDocument document, String text) {
		return createCell(document, 1, text).setBorder(Border.NO_BORDER);
	}

	private Cell createCell(WritableDocument document, int rowspan, TextAlignment alignment,
	                        Consumer<Paragraph> paragraphConsumer) {
		Paragraph paragraph = document.createParagraph(document.getFontSize() - 0.5f).setMargin(0)
				.setMultipliedLeading(1);
		paragraphConsumer.accept(paragraph);
		Cell cell = new Cell(rowspan, 1).setMargin(0);
		return cell.setTextAlignment(requireNonNull(alignment)).add(paragraph);
	}

	/**
	 * Clear a cell border according to the nibble: the last four bits of nibble, if unset, clear (remove) the cell
	 * border. The four bits (MSB to LSB) are to keep (if set) or clear (if unset) the top, right, bottom and left
	 * borders respectively.
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
			String header = i == 0 ? "\nRUIMTE VOOR AANTEKENINGEN\n\n\u00A0" : "\n\n\n\u00A0";
			par.add(header).setTextAlignment(CENTER);
		};
		addLinesPagesWithTitle(document, numberOfNotesPages, titleConsumer);
	}

	private void addLinesPagesWithTitle(WritableDocument document, int numberOfNotesPages,
	                                           BiConsumer<Integer, Paragraph> titleConsumer) {
		float pageWidth = document.getEffectiveArea().getWidth();
		Paragraph blockWithLines = document.createParagraph(null, smallHeadingFontSize)
				.addTabStops(new TabStop(pageWidth, TabAlignment.LEFT, new SolidLine(.75f)))
				.add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n")
				.add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n").add("\n");
		for (int i = 0; i < numberOfNotesPages; i++) {
			document.startNewPage(false);
			Paragraph paragraph = document.createParagraph();
			titleConsumer.accept(i, paragraph);
			document.addInFlow(paragraph);
			document.addInFlow(blockWithLines).addInFlow(blockWithLines).addInFlow(blockWithLines)
					.addInFlow(blockWithLines);
		}
	}

	private void addMindMapPages(WritableDocument document, int numberOfMindMapPages) {
		LOGGER.debug("Adding {} mind map pages", numberOfMindMapPages);
		for (int i = 0; i < numberOfMindMapPages; i++) {
			document.startNewPage(false);
			String header = i == 0 ? "\nRUIMTE VOOR MINDMAPS\n\n\u00A0" : "\n\n\n\u00A0";
			document.addInFlow(document.createParagraph().add(header).setTextAlignment(CENTER));
			document.draw((canvas, area) -> canvas.getPdfCanvas()
					.setLineWidth(0.5f).setColor(ColorConstants.BLACK, false)
					.rectangle(document.getEffectiveArea())
					.stroke());
		}
	}
}
