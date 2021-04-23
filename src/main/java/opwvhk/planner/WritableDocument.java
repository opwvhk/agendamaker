package opwvhk.planner;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.property.AreaBreakType;
import com.itextpdf.layout.property.FontKerning;
import com.itextpdf.layout.property.Property;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.renderer.IRenderer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
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
	private final Document document;

	private final PdfFont font;
	private final float fontSize;

	private BiConsumer<Integer, Document> newPageHandler;

	private WritableDocument(final PageSize pageSize, final OutputStream output)
		throws IOException {
		PdfWriter pdfWriter = new PdfWriter(output);
		pdfDocument = new PdfDocument(pdfWriter);
		pdfDocument.getCatalog().setPageLayout(PdfName.TwoColumnLeft);
		pdfDocument.setDefaultPageSize(pageSize);
		document = new Document(pdfDocument);

		font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
		fontSize = DEFAULT_FONT_SIZE;

		newPageHandler = (ignored1, ignored2) -> {};
	}

	public WritableDocument(final PageSize pageSize, float innerMargin, float outerMargin, float topBottomMargin, final OutputStream output)
		throws IOException {
		this(pageSize, output);
		withMargins(innerMargin, outerMargin, topBottomMargin);
	}

	public WritableDocument(final PageSize pageSize, final OutputStream output, final BiConsumer<Integer, Document> newPageHandler)
		throws IOException {
		this(pageSize, output);
		withNewPageHandler(newPageHandler);
	}

	/**
	 * Start a new page in the document. Mirrors the left & right margins.
	 */
	public void startNewPage() {
		if (pdfDocument.getNumberOfPages() == 0) {
			pdfDocument.addNewPage();
		} else {
			document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
		}
		newPageHandler.accept(pdfDocument.getNumberOfPages(), document);
	}


	@Override
	public void close() throws IOException {
		// Also closes pdfDocument and pdfWriter.
		document.close();
	}

	public Rectangle getEffectiveArea() {
		final PageSize pageSize;
		if (pdfDocument.getNumberOfPages() == 0) {
			pageSize = pdfDocument.getDefaultPageSize();
		} else {
			pageSize = new PageSize(pdfDocument.getLastPage().getPageSize());
		}
		return document.getPageEffectiveArea(pageSize);
	}

	public WritableDocument withMargins(float innerMargin, float outerMargin, float topBottomMargin) {
		return withNewPageHandler((pageNumber, document) -> {
			if (pageNumber % 2 == 0) {
				document.setMargins(topBottomMargin, innerMargin, topBottomMargin, outerMargin);
			} else {
				document.setMargins(topBottomMargin, outerMargin, topBottomMargin, innerMargin);
			}
		});
	}

	public WritableDocument withNewPageHandler(BiConsumer<Integer, Document> newPageHandler) {
		this.newPageHandler = requireNonNull(newPageHandler);
		this.newPageHandler.accept(max(pdfDocument.getNumberOfPages(), 1), document);
		return this;
	}

	public WritableDocument draw(BiConsumer<PdfCanvas, Rectangle> consumer) {
		final PdfPage page = pdfDocument.getLastPage();
		PdfCanvas pdfCanvas = new PdfCanvas(page);
		final Rectangle pageSize = page.getPageSize();
		pdfCanvas.saveState();
		try {
			consumer.accept(pdfCanvas, pageSize);
		} finally {
			pdfCanvas.restoreState();
		}
		return this;
	}

	public WritableDocument addInFlow(IBlockElement element) {
		document.add(element);
		return this;
	}

	public WritableDocument addInFlow(Image element) {
		document.add(element);
		return this;
	}

	public WritableDocument addInFlow(AreaBreak element) {
		document.add(element);
		return this;
	}

	public Paragraph createParagraph() {
		return createParagraph(font, fontSize);
	}

	public Paragraph createParagraph(Border border) {
		return createParagraph(font, fontSize)
			.setBorder(requireNonNull(border));
	}

	public Paragraph createParagraph(float fontSize) {
		return createParagraph(font, fontSize);
	}

	public Paragraph createParagraph(PdfFont font, float fontSize) {
		return createElement(font, fontSize, Paragraph::new)
			.setFixedLeading(fontSize * DEFAULT_LINE_SPACING);
	}

	public <E extends BlockElement<E>> E createElement(PdfFont font, float fontSize, Supplier<E> creator) {
		return creator.get()
			.setPadding(0)
			.setMargin(0)
			.setMarginBottom(6)
			.setFont(font == null ? this.font : font)
			.setFontSize(fontSize < 0 ? this.fontSize : fontSize)
			.setFontKerning(FontKerning.YES);
	}


	public List createList() {
		return createElement(font, fontSize, List::new)
			;
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
			//}).filter(boxed -> boxed.getBbox() != null && area.contains(boxed.getBbox())).findFirst().orElseThrow(); // Throw if no element meets the criteria
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
		//element.setHeight(bbox.getHeight());
		final Rectangle remainingArea = area.clone().decreaseHeight(bbox.getHeight());

		document.add(element);
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
	 * @return the bounding box of the rendered of the element, or {@code null} if the element doesn't fit
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


	public static float mmToPt(final float points) {
		return (float) Math.floor(points * MM_IN_POINTS);
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

	private record ElementAndExpectedBoundingBox(BlockElement<?> element, Rectangle bbox) {}
}
