package opwvhk.planner;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.BlockElement;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.properties.FontKerning;
import com.itextpdf.layout.properties.Leading;
import com.itextpdf.layout.properties.Property;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.renderer.IRenderer;
import com.itextpdf.svg.converter.SvgConverter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class WritableDocument implements Closeable {
	/*
	Useful links:
	https://stackoverflow.com/questions/49598325/itext-7-paragraph-height-as-it-would-be-rendered/49600997#49600997
	https://kb.itextpdf.com/home/it7kb/faq/how-to-add-text-inside-a-rectangle
	*/

	/**
	 * The number of points in a millimeter. 72 pt/in / 25.4 mm/in ~= 2.83 pt/mm
	 */
	private static final float MM_IN_POINTS = 72 / 25.4f;
	private static final float DEFAULT_LINE_SPACING = 4 / 3f;
	public static final float DEFAULT_FONT_SIZE = 11f;

	private final PdfDocument pdfDocument;
	final Document document;

	private final PdfFont font;
	private final float fontSize;

	private boolean pageIsEmpty;

	private WritableDocument(final PageSize pageSize, final OutputStream output) throws IOException {
		font = loadFont(StandardFonts.HELVETICA);
		fontSize = DEFAULT_FONT_SIZE;

		PdfWriter pdfWriter = new PdfWriter(output);
		pdfDocument = new PdfDocument(pdfWriter);
		pdfDocument.getCatalog().setPageLayout(PdfName.TwoColumnLeft);
		pdfDocument.setDefaultPageSize(pageSize);
		pdfDocument.addNewPage(); // We'll output at least one page.

		document = new Document(pdfDocument).setFont(font).setFontSize(fontSize).setFontKerning(FontKerning.YES);
		document.setProperty(Property.LEADING, new Leading(Leading.FIXED, DEFAULT_LINE_SPACING * fontSize));

		pageIsEmpty = true;
	}

	public static PdfFont loadFont(String fontResourceOrName) throws IOException {
		if (StandardFonts.isStandardFont(fontResourceOrName)) {
			return PdfFontFactory.createFont(fontResourceOrName);
		}
		ClassLoader loader = Optional.ofNullable(Thread.currentThread().getContextClassLoader())
				.orElse(WritableDocument.class.getClassLoader());
		try (InputStream fontStream = loader.getResourceAsStream(fontResourceOrName)) {
			byte[] ttfBytes = requireNonNull(fontStream).readAllBytes();
			PdfFont pdfFont = PdfFontFactory.createFont(ttfBytes, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
			pdfFont.setSubset(false);
			return pdfFont;
		}
	}

	public WritableDocument(final PageSize pageSize, float innerMargin, float outerMargin, float topBottomMargin, final OutputStream output)
			throws IOException {
		this(pageSize, output);
		document.setMargins(topBottomMargin, outerMargin, topBottomMargin, innerMargin);
		pdfDocument.addEventHandler(PdfDocumentEvent.START_PAGE, event -> {
			PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
			int pageNumber = pdfDocument.getPageNumber(docEvent.getPage());
			if (pageNumber % 2 == 0) {
				document.setMargins(topBottomMargin, innerMargin, topBottomMargin, outerMargin);
			} else {
				document.setMargins(topBottomMargin, outerMargin, topBottomMargin, innerMargin);
			}
		});
	}

	public WritableDocument addEventHandler(String eventType, IEventHandler eventHandler) {
		// Note: PDF events are not fired in sync with document manipulations, so removing event handlers is unreliable.
		pdfDocument.addEventHandler(eventType, eventHandler);
		return this;
	}

	public WritableDocument removeEventHandlers(String eventType) {
		// Note: PDF events are not fired in sync with document manipulations, so flush first.

		for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
			pdfDocument.getPage(i).flush();
		}
		// noinspection EqualsWhichDoesntCheckParameterClass
		pdfDocument.removeEventHandler(eventType, new IEventHandler() {
			@Override
			public void handleEvent(Event event) {
				// Nothing to do
			}

			@Override
			@SuppressWarnings("EqualsDoesntCheckParameterClass")
			public boolean equals(Object obj) {
				//noinspection Contract
				return true;
			}
		});
		pdfDocument.addNewPage(); // Ensure there's at least one next page.
		return this;
	}

	@Override
	public void close() {
		// Also closes pdfDocument and pdfWriter.
		document.close();
	}

	/**
	 * Start a new page in the document. Mirrors the left and right margins.
	 */
	public void startNewPage(boolean alsoStartNewPageIfLastOneIsEmpty) {
		if (alsoStartNewPageIfLastOneIsEmpty || !pageIsEmpty) {
			document.add(new AreaBreak());//AreaBreakType.NEXT_PAGE));
			pageIsEmpty = true;
		}
		if (pdfDocument.getNumberOfPages() == 0) {
			PdfPage pdfPage = pdfDocument.addNewPage();
		}
	}

	public float getFontSize() {
		return fontSize;
	}

	/**
	 * Return the number of pages written to so far. This may or may not be the complete number of pages in the final document.
	 *
	 * @return the number of pages written to
	 */
	public int numberOfPagesWrittenTo() {
		document.flush();
		return pdfDocument.getNumberOfPages();
	}

	public Rectangle getEffectiveArea() {
		final PageSize pageSize;
		if (pdfDocument.getNumberOfPages() == 0) {
			pageSize = pdfDocument.getDefaultPageSize();
		} else {
			PdfPage page = pdfDocument.getLastPage();
			PdfDictionary pdfObject = page.getPdfObject();
			if (pdfObject.containsKey(PdfName.MediaBox)) {
				pageSize = new PageSize(page.getPageSize());
			} else {
				pageSize = pdfDocument.getDefaultPageSize();
			}
		}
		return document.getPageEffectiveArea(pageSize);
	}

	public Image loadSvgImageResource(String resourceName) throws IOException {
		try (InputStream stream = getClass().getResourceAsStream(resourceName)) {
			return SvgConverter.convertToImage(requireNonNull(stream), pdfDocument);
		}
	}

	public PdfFormXObject loadPdfPageAsObject(String resourceName, int pageNumber) throws IOException {
		try (InputStream stream = getClass().getResourceAsStream(resourceName)) {
			PdfDocument pdfResource = new PdfDocument(new PdfReader(requireNonNull(stream)));
			PdfPage pageResource = pdfResource.getPage(pageNumber);
			return pageResource.copyAsFormXObject(pdfDocument);
		}
	}

	public WritableDocument drawFullPage(BiConsumer<Canvas, Rectangle> consumer) {
		startNewPage(false);
		drawOnPdfPage(pdfDocument.getLastPage(), consumer);
		return this;
	}

	public WritableDocument draw(BiConsumer<Canvas, Rectangle> consumer) {
		drawOnPdfPage(pdfDocument.getLastPage(), consumer);
		return this;
	}

	public WritableDocument draw(PdfDocumentEvent event, BiConsumer<Canvas, Rectangle> consumer) {
		drawOnPdfPage(event.getPage(), consumer);
		return this;
	}

	protected void drawOnPdfPage(PdfPage page, BiConsumer<Canvas, Rectangle> consumer) {
		try (Canvas canvas = new Canvas(page, page.getPageSize())) {
			canvas.setFont(font).setFontSize(fontSize);
			consumer.accept(canvas, getEffectiveArea());
			pageIsEmpty = false;
		}
	}

	public WritableDocument addInFlow(IBlockElement element) {
		document.add(element);
		pageIsEmpty = false;
		return this;
	}

	public WritableDocument addInFlow(Image element) {
		document.add(element);
		pageIsEmpty = false;
		return this;
	}

	public WritableDocument addInFlow(AreaBreak element) {
		document.add(element);
		pageIsEmpty = false;
		return this;
	}

	public Paragraph createParagraph() {
		return createParagraph(font, fontSize);
	}

	public Paragraph createParagraph(Border border) {
		return createParagraph(font, fontSize).setBorder(requireNonNull(border));
	}

	public Paragraph createParagraph(float fontSize) {
		return createParagraph(font, fontSize);
	}

	public Paragraph createParagraph(PdfFont font, float fontSize) {
		return createElement(font, fontSize, Paragraph::new).setFixedLeading(fontSize * DEFAULT_LINE_SPACING);
	}

	public <E extends BlockElement<E>> E createElement(PdfFont font, float fontSize, Supplier<E> creator) {
		return creator.get().setPadding(0).setMargin(0).setMarginBottom(6).setFont(font == null ? this.font : font)
				.setFontSize(fontSize < 0 ? this.fontSize : fontSize).setFontKerning(FontKerning.YES);
	}

	public List createList() {
		return createElement(font, fontSize, List::new);
	}

	/**
	 * Add a block element at the top of the specified area. Returns the remaining part of the area, below the added element.
	 *
	 * @param area    the area to render the element on
	 * @param element the element to add
	 * @return the remaining area
	 */
	public Rectangle addParagraph(Rectangle area, BlockElement<?> element) {
		// Determine the element width (if set), or use the full area width to render the paragraph.
		final float width = pointValue(element.getWidth(), area.getWidth());
		final Rectangle bbox = requireNonNull(calculateBBox(element, area.clone().setWidth(width)), "The element doesn't fit.");
		return addBoxedElement(area, new ElementAndExpectedBoundingBox(element, bbox));
	}

	/**
	 * Add the first of a series of block elements that fits the specified area. Returns the remaining part of the area, below the added element.
	 *
	 * @param area     the area to render the first fitting element on
	 * @param elements the stream of element candidates
	 * @return the remaining area
	 */
	public Rectangle addFirstFittingParagraph(Rectangle area, Stream<BlockElement<?>> elements) {
		// Find the first element that fits, then add that.
		final ElementAndExpectedBoundingBox boxedElement = elements.map(element -> {
			// Determine the element width (if set), or use the full area width to render the paragraph.
			final float width = pointValue(element.getWidth(), area.getWidth());
			final Rectangle bbox = calculateBBox(element, area.clone().setWidth(width));
			return new ElementAndExpectedBoundingBox(element, bbox);
			//}).filter(boxed -> boxed.getBbox() != null && area.contains(boxed.getBbox())).findFirst().orElseThrow(); // Throw if no element meets the
			// criteria
		}).filter(boxed -> boxed.bbox() != null).findFirst().orElseThrow(); // Throw if no element meets the criteria

		return addBoxedElement(area, boxedElement);
	}

	/**
	 * Add a block element at the top of the specified area. Returns the remaining part of the area, below the added element.
	 *
	 * @param area         the area to render the element on
	 * @param boxedElement the element (and its expected bounding box) to add
	 * @return the remaining area
	 */
	private Rectangle addBoxedElement(Rectangle area, ElementAndExpectedBoundingBox boxedElement) {
		final BlockElement<?> element = boxedElement.element;
		final Rectangle bbox = boxedElement.bbox;
		// The bbox is including extra padding/margins/border, but the element width excludes these.
		float strokeAndBorderWidth = strokeAndBorderWidth(element);
		final float left = bbox.getLeft() + strokeAndBorderWidth;
		final float bottom = bbox.getBottom() + strokeAndBorderWidth + pointValue(element.getPaddingBottom());
		final float contentWidth = bbox.getWidth() - 2 * strokeAndBorderWidth - pointValue(element.getPaddingLeft()) - pointValue(element.getPaddingRight());
		element.setFixedPosition(left, bottom, contentWidth);
		// element.setHeight(bbox.getHeight());
		final Rectangle remainingArea = area.clone().decreaseHeight(bbox.getHeight());

		document.add(element);
		pageIsEmpty = false;
		return remainingArea;
	}

	/**
	 * Convert a UnitValue to a pointValue. If the unit value is missing, the provided maximum is used as the default.
	 *
	 * @param unitValue the value to convert
	 * @param maxValue  the maximum value, used to resolve percentages and constrain the result
	 * @return the value in pt
	 */
	private float pointValue(UnitValue unitValue, float maxValue) {
		final float rawResult;
		if (unitValue == null) {
			rawResult = maxValue;
		} else if (unitValue.isPercentValue()) {
			rawResult = maxValue * unitValue.getValue() / 100.0f;
		} else {
			rawResult = unitValue.getValue();
		}
		return Math.max(0, Math.min(maxValue, rawResult));
	}

	private float pointValue(UnitValue unitValue) {
		if (unitValue == null) {
			return 0;
		}
		if (unitValue.isPointValue()) {
			return unitValue.getValue();
		}
		throw new IllegalArgumentException("Not a point value");
	}

	/**
	 * Calculate the bounding box of a block element, as it would be rendered on the available area.
	 *
	 * @param element       the element whose height to calculate
	 * @param availableArea the available ares
	 * @return the bounding box of the rendered element, or {@code null} if the element doesn't fit
	 */
	public Rectangle calculateBBox(BlockElement<?> element, Rectangle availableArea) {
		// Determine a (fictive) layout area of the right size.
		final LayoutContext layoutContext = new LayoutContext(new LayoutArea(1, availableArea));

		// Create renderer tree, and do not forget setParent().
		final IRenderer rendererSubTree = element.createRendererSubTree().setParent(document.getRenderer());

		// Determine the rendering result. If the content doesn't fit, the resulting height is "not a number".
		LayoutResult result = rendererSubTree.layout(layoutContext);
		return result.getStatus() == LayoutResult.FULL ? result.getOccupiedArea().getBBox() : null;
	}

	private float strokeAndBorderWidth(BlockElement<?> element) {
		float extraWidth = 0;

		final Float strokeWidth = element.getStrokeWidth();
		if (strokeWidth != null) {
			extraWidth += strokeWidth;
		}

		final Border border = element.getProperty(Property.BORDER);
		if (border != null) {
			extraWidth += border.getWidth();
		}

		return extraWidth;
	}


	public static float mmToPt(final float mm) {
		return (float) Math.floor(mm * MM_IN_POINTS);
	}

	/**
	 * Determine the maximum of the widths of all texts.
	 *
	 * @param texts the texts to size up
	 * @return the largest of the text widths
	 */
	public float getTextWidth(final String... texts) {
		return getTextWidth(font, fontSize, texts);
	}

	/**
	 * Determine the maximum of the widths of all texts.
	 *
	 * @param texts the texts to size up
	 * @return the largest of the text widths
	 */
	public float getTextWidth(final Iterable<String> texts) {
		return getTextWidth(font, fontSize, texts);
	}

	/**
	 * Determine the maximum of the widths of all texts.
	 *
	 * @param font     the font for which to determine the text width
	 * @param fontSize the font size for which to determine the text width
	 * @param texts    the texts to size up
	 * @return the largest of the text widths
	 */
	public static float getTextWidth(final PdfFont font, float fontSize, final String... texts) {
		return getTextWidth(font, fontSize, Arrays.asList(texts));
	}

	/**
	 * Determine the maximum of the widths of all texts.
	 *
	 * @param font     the font for which to determine the text width
	 * @param fontSize the font size for which to determine the text width
	 * @param texts    the texts to size up
	 * @return the largest of the text widths
	 */
	public static float getTextWidth(final PdfFont font, float fontSize, final Iterable<String> texts) {
		float width = 0;
		for (String text : texts) {
			width = max(width, font.getWidth(text, fontSize));
		}
		return width;
	}

	public static String[] arrayConcat(final String[]... textArrays) {
		int size = 0;
		for (final String[] textArray : textArrays) {
			size += textArray.length;
		}
		final String[] result = new String[size];

		int index = 0;
		for (final String[] textArray : textArrays) {
			System.arraycopy(textArray, 0, result, index, textArray.length);
			index += textArray.length;
		}
		return result;
	}

	private record ElementAndExpectedBoundingBox(BlockElement<?> element, Rectangle bbox) {
	}
}
