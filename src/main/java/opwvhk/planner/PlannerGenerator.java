package opwvhk.planner;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.PdfException;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TabAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.itextpdf.kernel.geom.PageSize.A4;
import static com.itextpdf.layout.borders.Border.NO_BORDER;
import static com.itextpdf.layout.property.TextAlignment.*;
import static java.lang.Math.min;
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
	/**
	 * Logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PlannerGenerator.class.getName());

	private static final Locale LOCALE = new Locale("nl", "NL");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", LOCALE);
	/**
	 * The description of the planner to generate.
	 */
	private final PlannerDescription plannerDescription;
	/**
	 * The first date to render. This is the first date in {@link #plannerDescription} rounded down to a monday.
	 */
	private final LocalDate startDate;
	/**
	 * The number of wee first date not to render. This is the last date in {@link #plannerDescription}, increased to the next possible {@link #startDate}.
	 */
	private final int numberOfWeeks;
	/**
	 * The number of notes pages to add. May be higher than specified by {@link #plannerDescription}, because the total number of pages must be a multiple of 4.
	 */
	private final int numberOfNotesPages;

	/**
	 * Create a planner generator.
	 *
	 * @param plannerDescription a description of the planner to generate
	 */
	public PlannerGenerator(final PlannerDescription plannerDescription) {
		this.plannerDescription = plannerDescription;
		NavigableMap<LocalDate, String> dateTitles = plannerDescription.sortedDateTitles();
		startDate = dateTitles.firstKey().with(TemporalAdjusters.previousOrSame(MONDAY));

		LocalDate endDateExclusive = dateTitles.lastKey().with(TemporalAdjusters.nextOrSame(SUNDAY)).plusDays(1);
		numberOfWeeks = (int) WEEKS.between(startDate, endDateExclusive);

		// Minimal number of pages (so far): 2 pages per week, notes pages, plus 3 static pages (title page, explanations page, blank back page)
		final int minPages = 2 * numberOfWeeks + plannerDescription.notesPages() + 3;
		// The result must be printable as a booklet, which means a fourfold number of pages.
		// So the num ber of notes pages is increased with the number of pages needed to make the total page count a multiple of four.
		numberOfNotesPages = plannerDescription.notesPages() + 3 - (minPages - 1) % 4;
	}

	/**
	 * Generate a week planner into an {@link OutputStream}.
	 *
	 * @param output the stream to write to
	 * @throws IOException  when the planner cannot be written
	 * @throws PdfException when the planner cannot be generated
	 */
	public void generate(final OutputStream output) throws IOException, PdfException {
		float topBottomMargin = mmToPt(20);
		float margin = mmToPt(20);
		try (final WritableDocument document = new WritableDocument(A4, margin, margin, topBottomMargin, output)) {
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
	public void generate(final WritableDocument document) throws IOException, PdfException {
		//document.startNewPage();
		addTitlePage(document, plannerDescription.title(), plannerDescription.subtitle());
		addPlanningInstructions(document);

		addPlanningWeeks(document, startDate, numberOfWeeks, plannerDescription.sortedDateTitles());

		addNotesPages(document, numberOfNotesPages);

		// Blank back page
		document.startNewPage();
	}

	private void addTitlePage(WritableDocument document, String title, String subtitle) {
		LOGGER.debug("Adding title page");
		final Rectangle pageArea = document.getEffectiveArea();
		float pageWidth = pageArea.getWidth();

		final ImageData titleImageData = ImageDataFactory.create(requireNonNull(getClass().getResource("/Planner.png")));
		float titleScaledWidth = pageWidth * 0.75f;
		float titleScaledHeight = titleScaledWidth * titleImageData.getHeight() / titleImageData.getWidth();
		final Image titleImage = new Image(titleImageData).setWidth(titleScaledWidth).setHeight(titleScaledHeight);

		final ImageData logoImageData = ImageDataFactory.create(requireNonNull(getClass().getResource("/logo-huizermaat.png")));
		final float logoWidth = pageWidth * 0.4f;
		final Image logoImage = new Image(logoImageData).setWidth(logoWidth).setHeight(logoWidth * logoImageData.getHeight() / logoImageData.getWidth());

		document.addInFlow(document.createParagraph(null, 48).add("\n" + title).setMarginBottom(0).setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph(null, 32).add(subtitle + "\n\n").setMarginBottom(0).setTextAlignment(CENTER));
		document.addInFlow(titleImage.setHorizontalAlignment(HorizontalAlignment.CENTER));
		document.addInFlow(document.createParagraph().add("\n\n\n\n\n\n\n\n\n\nDeze planner is van:\n\n"));
		document.addInFlow(document.createParagraph().addTabStops(new TabStop(pageWidth * 0.75f, TabAlignment.LEFT, new SolidLine(.75f)))
			.add(new Tab()).add(" "));
		document.addInFlow(logoImage.setFixedPosition(pageArea.getRight() - logoWidth, pageArea.getBottom(), logoWidth));
		document.startNewPage();
	}

	private void addPlanningInstructions(WritableDocument document) {
		LOGGER.debug("Adding planning instructions");
		document.addInFlow(document.createParagraph().add(bold("\nVOORWAARDEN VOOR LEREN").setFontSize(16)).setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph().add('\n' + """
			Voor je ligt de planagenda voor de komende tijd. In deze agenda kun je voor jezelf overzicht creëren in wat wanneer af moet zijn, maar ook wanneer
			je het af gaat maken. Zo ben je goed georganiseerd!""".replace("\n", " ")).setTextAlignment(CENTER));
		document.addInFlow(document.createParagraph().add("""
			Voordat je aan de slag kunt gaan met het plannen, staan hieronder nog een aantal voorwaarden voor het leren opgesteld. Deze voorwaarden zijn
			belangrijk om in je achterhoofd te houden tijdens het plannen, lees ze daarom maar goed door.""".replace("\n", " ") + "\n\n")
			.setTextAlignment(CENTER));

		final float indent = mmToPt(10);
		document.addInFlow(document.createParagraph().add(bold("Leer als je fit bent")));
		document.addInFlow(document.createParagraph().setMarginLeft(indent).add("Ben je moe? Powernap! Daarna leer je beter.").add("\n\u00A0"));
		document.addInFlow(document.createParagraph().add(bold("Zoek een rustige plaats")));
		document.addInFlow(document.createParagraph().setMarginLeft(indent).add("Waar je je op je gemak voelt én je kan concentreren.").add("\n\u00A0"));
		document.addInFlow(document.createParagraph().add(bold("Houd je doel voor ogen")));
		document.addInFlow(document.createParagraph().setMarginLeft(indent).add("Bijvoorbeeld een cijfer dat je wilt halen; een onvoldoende wegwerken; " +
			"een bepaalde studie die je wilt gaan doen.").add("\n\u00A0"));
		document.addInFlow(document.createParagraph().add(bold("Neem er de tijd voor")));
		document.addInFlow(document.createParagraph().setMarginLeft(indent).add("Als je tijdsdruk ervaart en/of niet ontspannen bent, neem je minder " +
			"informatie op.").add("\n\u00A0"));
		document.addInFlow(document.createParagraph().add(bold("Maak een planning")));
		document.addInFlow(document.createParagraph().setMarginLeft(indent).add("Kijk goed naar wat je moet leren en bedenk hoeveel tijd je daarvoor nodig " +
			"hebt. Een dag voor de toets niks nieuws leren maar ").add(underline("alles herhalen")).add(".").add("\n\u00A0"));
		document.addInFlow(document.createParagraph().add(bold("Bedenk hoe je de informatie het beste kunt leren")));
		document.addInFlow(document.createParagraph().setMarginLeft(indent).add("Wat is voor jou de beste aanpak die aansluit bij hoe jij graag leert?")
			.add("\n\u00A0"));

		document.addInFlow(document.createParagraph().add(bold("\n\nHERHALEN, HERHALEN, HERHALEN")));
		//noinspection SpellCheckingInspection
		document.addInFlow(document.createParagraph()
			.add("""
				Je hebt het vast al héél vaak gehoord, maar herhaling van de stof die je moet leren, is het allerbelangrijkste. Wanneer je veel aandacht aan
				iets geeft, worden er verbindingen aangelegd in je hersenen, waardoor je er steeds beter in wordt! In de eerste 20 minuten na het leren, kan
				je al zo’n 40% vergeten. Dat is bijna de helft. Herhalen zorgt ervoor dat je minder vergeet. Wanneer je dus veel aandacht aan iets geeft door
				het te herhalen, wordt je er én beter in én\s""".replace("\n", " "))
			.add(italic("beter"))
			.add(" in én ")
			.add(italic("onthoudt"))
			.add(" je het ")
			.add(italic("beter"))
			.add("! Snap je nu waarom het zo belangrijk is?")
		);
		document.addInFlow(document.createParagraph().add(bold("\n\nEn dan kun je nu aan de slag, succes!")).setTextAlignment(CENTER));
	}

	private Text bold(String text) {
		return new Text(text).setBold();
	}

	private Text italic(String text) {
		return new Text(text).setItalic();
	}

	@SuppressWarnings("SameParameterValue")
	private Text underline(String text) {
		return new Text(text).setUnderline();
	}

	private void addPlanningWeeks(WritableDocument document, LocalDate startDate, int numberOfWeeks, NavigableMap<LocalDate, String> dateTitles) {
		LOGGER.debug("Adding planning weeks");
		final float margin = mmToPt(12.5f);
		document.withMargins(margin, margin, margin).startNewPage();

		final SolidBorder thickBorder = new SolidBorder(1.5f);

		final UnitValue[] columnWidths = Stream.of(6f, 6f, 38f, 30f, 14f, 6f).map(UnitValue::createPercentValue).toArray(UnitValue[]::new);
		for (int i = 0; i < numberOfWeeks; i++) {
			LocalDate monday = startDate.plusWeeks(i);
			int numNotesRows = 5;
			int numNotesRowsWeekend = 6;
			LOGGER.debug("Adding planning week starting on {}", monday);

			for (int dow = 0; dow < 5; dow++) {
				final LocalDate date = monday.plusDays(dow);

				final Table table = new Table(columnWidths).useAllAvailableWidth().setPadding(0).setMargin(0).setMarginBottom(16).setFixedLayout();
				addDateHeader(document, table, thickBorder, dateText(date), titleForDate(date, dateTitles));
				for (int lessonRow = 1; lessonRow <= 7; lessonRow++) {
					table.addCell(createCell(document, 1, lessonRow + ".", CENTER));
					table.addCell(emptyCell(document, 5));
				}
				addNotesRows(document, thickBorder, table, numNotesRows, true);
				document.addInFlow(table);
			}

			final Table table = new Table(columnWidths).useAllAvailableWidth().setPadding(0).setMargin(0).setMarginBottom(16).setFixedLayout();
			final LocalDate saturday = monday.plusDays(5);
			addDateHeader(document, table, thickBorder, twoDatesText(saturday), titleForDate(saturday, dateTitles));
			addNotesRows(document, thickBorder, table, numNotesRowsWeekend, true);
			addNotesRows(document, thickBorder, table, numNotesRowsWeekend, false);
			document.addInFlow(table);
		}
	}

	@SuppressWarnings("SameParameterValue")
	private void addNotesRows(WritableDocument document, SolidBorder thickBorder, Table table, int numNotesRows, boolean withHeader) {
		if (withHeader) {
			table.addCell(createCell(document, 2, "Vak:", LEFT).setBorderTop(thickBorder));
			table.addCell(createCell(document, 1, "Welk onderdeel ga ik voorbereiden:", LEFT).setBorderTop(thickBorder));
			table.addCell(createCell(document, 1, "Hoe ga ik dat voorbereiden?", LEFT).setBorderTop(thickBorder));
			table.addCell(createCell(document, 1, "Hoeveel tijd kost dit me?", LEFT).setBorderTop(thickBorder));
			table.addCell(emptyCell(document, 1).setBorderTop(thickBorder));
		} else {
			table.addCell(emptyCell(document, 2).setBorderTop(thickBorder));
			table.addCell(emptyCell(document, 1).setBorderTop(thickBorder));
			table.addCell(emptyCell(document, 1).setBorderTop(thickBorder));
			table.addCell(emptyCell(document, 1).setBorderTop(thickBorder));
			table.addCell(emptyCell(document, 1).setBorderTop(thickBorder));
		}
		for (int dayNotesRow = withHeader ? 0 : 1; dayNotesRow < numNotesRows; dayNotesRow++) {
			table.addCell(emptyCell(document, 2));
			table.addCell(emptyCell(document, 1));
			table.addCell(emptyCell(document, 1));
			table.addCell(emptyCell(document, 1));
			table.addCell(emptyCell(document, 1));
		}
	}

	private void addDateHeader(WritableDocument document, Table table, SolidBorder thickBorder, String dateAsText, String titleForDate) {
		table.addCell(emptyCell(document, 1)
			.setBorderTop(thickBorder).setBorderBottom(thickBorder)
			.setBorderRight(NO_BORDER));
		table.addCell(createCell(document, 4, LEFT, paragraph -> paragraph
			.addTabStops(new TabStop(1000, TabAlignment.RIGHT)) // Use any insanely high number
			.add(new Text(dateAsText))
			.add(new Tab())
			.add(new Text(titleForDate)))
			.setBorderTop(thickBorder).setBorderBottom(thickBorder)
			.setBorderLeft(NO_BORDER).setBorderRight(NO_BORDER));
		table.addCell(emptyCell(document, 1)
			.setBorderTop(thickBorder).setBorderBottom(thickBorder)
			.setBorderLeft(NO_BORDER));
	}

	private String dateText(LocalDate date) {
		return capitalize(DATE_FORMAT.format(date));
	}

	private String capitalize(String text) {
		return Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	private String twoDatesText(LocalDate firstDate) {
		final String dateText1 = DATE_FORMAT.format(firstDate);
		final String dateText2 = DATE_FORMAT.format(firstDate.plusDays(1));
		return capitalize(removeCommonSuffix(dateText1, dateText2) + " en " + dateText2);
	}

	private String removeCommonSuffix(String textToReduce, String textToCompareWith) {
		final int length1 = textToReduce.length();
		final int length2 = textToCompareWith.length();
		final int longestPossibleSuffixLength = min(length1, length2);
		for (int i = longestPossibleSuffixLength; i > 0; i--) {
			final int index1 = length1 - i;
			if (textToReduce.charAt(index1) == ' ' && textToReduce.substring(index1).equals(textToCompareWith.substring(length2 - i))) {
				return textToReduce.substring(0, index1);
			}
		}
		return textToReduce;
	}

	private String titleForDate(LocalDate date, NavigableMap<LocalDate, String> dateTitles) {
		final Map.Entry<LocalDate, String> dateTitleEntry;
		if (dateTitles.floorKey(date) == null) {
			dateTitleEntry = dateTitles.firstEntry();
		} else {
			dateTitleEntry = dateTitles.floorEntry(date);
		}
		if (dateTitleEntry.getValue() == null || dateTitleEntry.getValue().isBlank()) {
			return "";
		} else {
			return dateTitleEntry.getValue().strip();
		}
	}

	private Cell emptyCell(WritableDocument document, int colspan) {
		return createCell(document, colspan, null, LEFT);
	}

	private Cell createCell(WritableDocument document, int colspan, String text, TextAlignment alignment) {
		return createCell(document, colspan, alignment, paragraph -> paragraph.add(requireNonNullElse(text, "\u00A0")));
	}

	private Cell createCell(WritableDocument document, int colspan, TextAlignment alignment, Consumer<Paragraph> paragraphConsumer) {
		final Paragraph paragraph = document.createParagraph(10.5f).setMargin(0).setMultipliedLeading(1);
		paragraphConsumer.accept(paragraph);
		return new Cell(1, colspan).setTextAlignment(requireNonNull(alignment)).add(paragraph);
	}

	private void addNotesPages(WritableDocument document, int numberOfNotesPages) {
		LOGGER.debug("Adding {} notes pages", numberOfNotesPages);
		float pageWidth = document.getEffectiveArea().getWidth();
		final Paragraph blockWithLines = document.createParagraph(null, 14)
			.addTabStops(new TabStop(pageWidth, TabAlignment.LEFT, new SolidLine(.75f)))
			.add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n")
			.add(new Tab()).add("\n").add(new Tab()).add("\n").add(new Tab()).add("\n").add("\n");
		for (int i = 0; i < numberOfNotesPages; i++) {
			document.startNewPage();
			final String header = i == 0 ? "\nRUIMTE VOOR AANTEKENINGEN\n\n\n" : "\n\n\n\n";
			document.addInFlow(document.createParagraph().add(header).setTextAlignment(CENTER));
			document.addInFlow(blockWithLines).addInFlow(blockWithLines).addInFlow(blockWithLines).addInFlow(blockWithLines);
		}
	}
}
