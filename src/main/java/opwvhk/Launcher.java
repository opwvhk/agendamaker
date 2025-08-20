package opwvhk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.optionalusertools.DateChangeListener;
import com.github.lgooddatepicker.tableeditors.DateTableEditor;
import com.github.lgooddatepicker.zinternaltools.HighlightInformation;
import opwvhk.planner.ClassItemStructure;
import opwvhk.planner.DateTitleFromTo;
import opwvhk.planner.PlannerDescription;
import opwvhk.planner.PlannerGenerator;
import opwvhk.planner.StaticPage;
import opwvhk.swing.DesktopApp;
import opwvhk.swing.FormBuilder;
import opwvhk.swing.NarrowJEditorPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import static opwvhk.planner.ClassItemStructure.CLASS_ROOM_SINGLE;

public class Launcher extends DesktopApp {
	public static final String APPLICATION_NAME = "Agendamaker";
	private static final Locale LOCALE = Locale.forLanguageTag("nl-NL");
	private static final String DATE_FORMAT_PATTERN = "d MMM yyyy";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN)
			.withLocale(LOCALE);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
	private static final String ERROR_TITLE = "Oeps... vraag een programmeur...";
	private static final Color COLOR_HOLIDAY = new Color(219, 231, 242);
	private static final Color COLOR_SPECIAL = new Color(203, 253, 203);
	private static final Color COLOR_OTHER = new Color(195, 230, 252);
	private static final Color COLOR_NORMAL = Color.WHITE; // default

	/// A random date (never seen) to bootstrap the TableDateEditor pairs with.
	private static final LocalDate FAR_AWAY_DATE = LocalDate.MIN;
	public static void main(String[] args) {
		new Launcher().start();
	}


	private final Preferences lastUsedSettings;
	private final Holidays holidays;

	private DatePicker startDatePicker;
	private DatePicker endDatePicker;
	private JSpinner numClassesSpinner;
	private AtomicReference<ClassItemStructure> selectedClassItemStructure;
	private JTable dateTitlesTable;
	private JFrame mainWindow;

	public Launcher() {
		super(APPLICATION_NAME, "/icons/schedule2_64.png", "/icons/schedule2_16.png", "/icons/schedule2_24.png",
				"/icons/schedule2_32.png", "/icons/schedule2_128.png", "/icons/schedule2_256.png");
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		lastUsedSettings = preferences.node("lastUsed");
		// The initial year is not that important; it only serves as a starting point for iteration.
		holidays = new Holidays(LOCALE, 2025, Holidays.Type.HOLIDAY, Holidays.Type.SPECIAL, Holidays.Type.OTHER, Holidays.Type.NORMAL);
	}

	@Override
	protected String getAboutText() {
		return """
				Agendamaker v0.1
				Genereert de week-pagina's voor schoolagenda's op A4-formaat.
				""";
	}

	public void start() {
		JComponent header = createHeader();

		JButton generatePlannerButton = createButton("Maak agenda", this::generatePlanner);
		JButton saveButton = createButton("Bewaar invoer", e -> System.out.println(createPlannerDescription(e)));

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(generatePlannerButton);

		JPanel mainInputPanel = createMainInputPanel();
		JComponent dateTitlesPanel = createDateTitlesPanel();
		Box content = hbox(vbox(mainInputPanel, Box.createVerticalGlue()), dateTitlesPanel);

		// Build and display the window

		mainWindow = createMainWindow();
		// mainWindow.setLocationRelativeTo(null); // Center on screen
		// noinspection SpellCheckingInspection
		mainWindow.getRootPane().putClientProperty("apple.awt.fullscreenable", true);
		BorderLayout mainWindowLayout = new BorderLayout();
		mainWindow.setLayout(mainWindowLayout);
		mainWindow.add(header, BorderLayout.NORTH);
		mainWindow.add(content, BorderLayout.CENTER);
		mainWindow.add(buttonPanel, BorderLayout.SOUTH);

		ensureMinimumSize(mainWindow);
		mainWindow.pack();
		mainWindow.setVisible(true);
	}

	private @NotNull JComponent createHeader() {
		String htmlText = """
				<html lang="nl"><body>
				<h2>Hoe te gebruiken</h2>
				<p>
				Deze agendamaker genereert een PDF-bestand op basis van de invoer hieronder. Alles wat geen knop is,
				is een invoerveld. Beide knoppen rechtsonder slaan de huidige invoer op als voorkeursinstellingen.
				</p><p>
				Een paar extra opmerkingen:
				</p><ul><li>
				De namen van de periodes in de tabel rechts verschijnen als dagtekst in de planner.
				Niet alle periodes komen in de planner: de start- en einddatum links bepalen het begin en einde.
				</li><li>
				Het aantal lesuren plus het aantal taken is 13, en er moeten minimaal 3 van elk zijn.
				Het aantal lesuren is ook het aantal taken voor zaterdag, het aantal taken wordt ook gebruikt voor
				zondag.
				</li><li>
				De structuur toont hoe de lesuren eruit zien. Linksboven staat altijd het lesuur, de keuze bepaalt het
				aantal schrijflijnen of dat het een groot vak is.
				<!--
				</li><li>
				</li><li>
				</li><li>-->
				</li></ul>
				</body></html>""";

		JEditorPane textArea = new NarrowJEditorPane();
		textArea.setEditable(false);
		textArea.setContentType("text/html");
		textArea.setText(htmlText);
		textArea.setBorder(BorderFactory.createEmptyBorder(2, 5, 5, 5));
		return textArea;
	}

	private @NotNull JPanel createMainInputPanel() {

		LocalDate[] initialPeriod = correctPeriod(new LocalDate[]{
				parseOrDefault(lastUsedSettings.get("startDate", null), LocalDate.now()),
				parseOrDefault(lastUsedSettings.get("endDate", null), LocalDate.now())
		});
		Pair<DatePicker> datePickerPair = createDatePickerPair(initialPeriod[0], initialPeriod[1]);
		startDatePicker = datePickerPair.left;
		endDatePicker = datePickerPair.right;

		JLabel actualPlannerPeriodLabel = new JLabel("");
		actualPlannerPeriodLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		Dimension labelSize = preferredSizeOf("(wordt 25 sept 2025 t/m 25 sept 2025)");
		labelSize.setSize(labelSize.getWidth(), labelSize.getHeight() + 4);
		actualPlannerPeriodLabel.setMinimumSize(labelSize);
		actualPlannerPeriodLabel.setPreferredSize(labelSize);
		DateChangeListener plannerPeriodChangeListener = ignored -> {
			LocalDate startDate = startDatePicker.getDate();
			LocalDate endDate = endDatePicker.getDate();
			LocalDate[] period = correctPeriod(new LocalDate[]{startDate, endDate});
			if (!startDate.equals(period[0]) || !endDate.equals(period[1])) {
				actualPlannerPeriodLabel.setText(
						"(wordt %s t/m %s)".formatted(DATE_FORMATTER.format(period[0]),
								DATE_FORMATTER.format(period[1])));
			} else {
				actualPlannerPeriodLabel.setText("");
			}
		};
		plannerPeriodChangeListener.dateChanged(null); // Set initial label
		startDatePicker.addDateChangeListener(plannerPeriodChangeListener);
		endDatePicker.addDateChangeListener(plannerPeriodChangeListener);

		numClassesSpinner = new JSpinner(new SpinnerNumberModel(lastUsedSettings.getInt("numClasses", 7), 3, 10, 1));
		ClassItemStructure classItemStructure = ClassItemStructure.valueOf(
				lastUsedSettings.get("classItemStructure", CLASS_ROOM_SINGLE.name()));
		selectedClassItemStructure = new AtomicReference<>(classItemStructure);
		List<ImageIcon> icons = loadIcons(ImageIcon::new, "/structure_single_rectangle.png",
				"/structure_rect_1_line.png", "/structure_rect_2_lines.png", "/structure_rect_3_lines.png");
		List<JComponent> structureChoices = new ArrayList<>();
		ButtonGroup structureChoiceGroup = new ButtonGroup();
		for (ClassItemStructure cis : ClassItemStructure.values()) {
			JRadioButton structureButton = new JRadioButton("", classItemStructure == cis);
			structureButton.getModel().setActionCommand(cis.name());
			structureButton.addActionListener(e -> selectedClassItemStructure.set(cis));
			structureChoiceGroup.add(structureButton);
			JLabel structureImage = new JLabel(icons.get(cis.ordinal()), SwingConstants.LEFT);
			structureImage.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					selectedClassItemStructure.set(cis);
					structureButton.setSelected(true);
				}
			});
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			panel.add(structureButton);
			panel.add(structureImage);
			structureChoices.add(panel);
		}

		GridBagConstraints spinnerConstraints = new GridBagConstraints();
		spinnerConstraints.anchor = GridBagConstraints.EAST;
		FormBuilder builder = new FormBuilder();
		builder.add(0, 0, new JLabel("Startdatum"), startDatePicker);
		builder.add(0, 1, new JLabel("Einddatum"), endDatePicker);
		builder.add(0, 2, 1, actualPlannerPeriodLabel);
		builder.add(0, 3, new JLabel("Aantal lesuren"), numClassesSpinner, spinnerConstraints);
		int i = 4;
		for (ClassItemStructure cis : ClassItemStructure.values()) {
			int ordinal = cis.ordinal();
			builder.add(0, i++, new JLabel(ordinal == 0 ? "Structuur" : ""), structureChoices.get(ordinal));
		}

		JPanel mainInputPanel = builder.build();
		mainInputPanel.setBorder(BorderFactory.createTitledBorder("Data en layout"));

		Dimension size = mainInputPanel.getPreferredSize();
		mainInputPanel.setMinimumSize(size);
		mainInputPanel.setMaximumSize(size);

		return mainInputPanel;
	}

	private @NotNull JPanel createDateTitlesPanel() {
		dateTitlesTable = createDateTitlesTable();
		int minimumTableWidth = dateTitlesTable.getMinimumSize().width;

		JScrollPane scrollPane = new JScrollPane(dateTitlesTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setMinimumSize(enclose(scrollPane.getMinimumSize(), new Dimension(minimumTableWidth, 0)));
		scrollPane.setPreferredSize(
				enclose(scrollPane.getPreferredSize(), scrollPane.getMinimumSize(), new Dimension(0, 500)));

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(createButton("Verwijder geselecteerde datumregels", e1 -> {
			DefaultTableModel model = (DefaultTableModel) dateTitlesTable.getModel();
			int[] selectedRows = dateTitlesTable.getSelectedRows();
			// Indices are in ascending order; loop backwards to avoid changing them
			for (int i = selectedRows.length - 1; i >= 0; i--) {
				model.removeRow(selectedRows[i]);
			}
		}), "tag no");
		buttonPanel.add(createButton("Voeg datumregel toe", e -> {
			DefaultTableModel model = (DefaultTableModel) dateTitlesTable.getModel();
			int lastRow = model.getRowCount() - 1;
			if (lastRow >= 0) {
				LocalDate lastDate0 = (LocalDate) model.getValueAt(lastRow, 0);
				LocalDate lastDate1 = (LocalDate) model.getValueAt(lastRow, 1);
				LocalDate lastDate = max(lastDate0, lastDate1);
				model.addRow(new Object[]{lastDate, lastDate, ""});
			} else {
				model.addRow(new Object[]{startDatePicker.getDate(), startDatePicker.getDate(), ""});
			}
		}), "tag yes");

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(buttonPanel, BorderLayout.SOUTH);
		panel.add(scrollPane, BorderLayout.CENTER);
		panel.setBorder(BorderFactory.createTitledBorder("Belangrijke periodes / vakanties"));
		return panel;
	}

	private @NotNull JTable createDateTitlesTable() {
		String storedDateTitles = lastUsedSettings.get("dateTitles", """
				[
				  {"from" : "2024-10-26", "to" : "2024-11-03", "text" : "Herfstvakantie"},
				  {"from" : "2024-12-21", "to" : "2025-01-05", "text" : "Kerstvakantie"},
				  {"from" : "2024-12-25", "to" : "2024-12-25", "text" : "1e Kerstdag"},
				  {"from" : "2024-12-26", "to" : "2024-12-26", "text" : "2e Kerstdag"},
				  {"from" : "2025-02-15", "to" : "2025-02-23", "text" : "Voorjaarsvakantie"},
				  {"from" : "2025-04-19", "to" : "2025-05-05", "text" : "Meivakantie"},
				  {"from" : "2025-04-21", "to" : "2025-04-21", "text" : "1e Paasdag"},
				  {"from" : "2025-04-22", "to" : "2025-04-22", "text" : "2e Paasdag"},
				  {"from" : "2025-04-27", "to" : "2025-04-27", "text" : "Koningsdag"},
				  {"from" : "2025-05-29", "to" : "2025-05-29", "text" : "Hemelvaart"},
				  {"from" : "2025-05-30", "to" : "2025-05-30", "text" : "dag na Hemelvaart (vrij)"},
				  {"from" : "2025-06-08", "to" : "2025-06-08", "text" : "1e Pinksterdag"},
				  {"from" : "2025-06-09", "to" : "2025-06-09", "text" : "2e Pinksterdag"},
				  {"from" : "2025-07-12", "to" : "2025-07-19", "text" : "Zomervakantie"}
				]
				""");
		List<DateTitleFromTo> dateTitleFromToList;
		try {
			dateTitleFromToList = OBJECT_MAPPER.readValue(storedDateTitles, new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		String[] COLUMN_NAMES = {"Vanaf", "T/m", "Omschrijving"};
		Class<?>[] COLUMN_TYPES = {LocalDate.class, LocalDate.class, String.class};
		DefaultTableModel dateTitlesModel = new DefaultTableModel(COLUMN_NAMES, 0) {
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return COLUMN_TYPES[columnIndex];
			}
		};
		for (DateTitleFromTo row : dateTitleFromToList) {
			dateTitlesModel.addRow(new Object[]{row.from(), row.to(), row.text()});
		}
		JTable dateTitlesTable = new JTable(dateTitlesModel);
		dateTitlesTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		TableColumn column0 = dateTitlesTable.getColumnModel().getColumn(0);
		TableColumn column1 = dateTitlesTable.getColumnModel().getColumn(1);
		TableColumn column2 = dateTitlesTable.getColumnModel().getColumn(2);

		column0.setMinWidth(160);
		column0.setPreferredWidth(200);
		column0.setMaxWidth(200);
		column1.setMinWidth(160);
		column1.setPreferredWidth(200);
		column1.setMaxWidth(200);
		column2.setMinWidth(250);
		column2.setPreferredWidth(400);

		Pair<DateTableEditor> editorPair = createDateTableEditorPair(dateTitlesModel, 0, 1);
		column0.setCellEditor(editorPair.left());
		column1.setCellEditor(editorPair.right());
		Pair<DateTableEditor> editorPair2 = createDateTableEditorPair(dateTitlesModel, 0, 1);
		column0.setCellRenderer(editorPair2.left());
		column1.setCellRenderer(editorPair2.right());

		return dateTitlesTable;
	}

	private @NotNull PlannerDescription createPlannerDescription(ActionEvent event) {
		LocalDate startDate = startDatePicker.getDate();
		LocalDate endDate = endDatePicker.getDate();
		int numClasses = (Integer) numClassesSpinner.getValue();
		ClassItemStructure classItemStructure = selectedClassItemStructure.get();
		TableModel dateTitlesModel = dateTitlesTable.getModel();
		List<DateTitleFromTo> dateTitleFromToList = IntStream.range(0, dateTitlesModel.getRowCount())
				.mapToObj(row -> {
					LocalDate from = (LocalDate) dateTitlesModel.getValueAt(row, 0);
					LocalDate to = (LocalDate) dateTitlesModel.getValueAt(row, 1);
					String text = (String) dateTitlesModel.getValueAt(row, 2);
					// No fields are null
					return new DateTitleFromTo(text == null ? "" : text, min(from, to), max(from, to));
				})
				.sorted(Comparator.comparing(DateTitleFromTo::from))
				.toList();

		lastUsedSettings.put("startDate", DATE_FORMATTER.format(startDate));
		lastUsedSettings.put("endDate", DATE_FORMATTER.format(endDate));
		lastUsedSettings.putInt("numClasses", numClasses);
		lastUsedSettings.put("classItemStructure", classItemStructure.name());
		try {
			lastUsedSettings.put("dateTitles", OBJECT_MAPPER.writeValueAsString(dateTitleFromToList));
		} catch (JsonProcessingException e) {
			showErrorFor((Component) event.getSource(), e);
		}
		return new PlannerDescription("", "", 0, 0, 0, numClasses,
				classItemStructure, EnumSet.noneOf(StaticPage.class), startDate, endDate, dateTitleFromToList);
	}

	private void generatePlanner(ActionEvent event) {
		File selectedPdfFile = chooseFile(new File("."), "Opslaan",
				"Kies een PDF-bestand om de planner in op te slaan")
				.map(Launcher::ensurePdfExtension).orElse(null);
		if (selectedPdfFile == null) {
			return;
		}
		try {
			try (OutputStream output = new FileOutputStream(selectedPdfFile)) {
				PlannerDescription plannerDescription = createPlannerDescription(event);
				PlannerGenerator plannerGenerator = new PlannerGenerator(plannerDescription);
				plannerGenerator.generate(output);
			}
			openFile(selectedPdfFile);
		} catch (IOException e) {
			showErrorFor((Component) event.getSource(), e);
		}
	}

	@SuppressWarnings("SameParameterValue")
	private @NotNull Optional<File> chooseFile(File currentDirectory, String approveButtonText, String dialogTitle) {
		JFileChooser fileChooser = new JFileChooser(currentDirectory);
		fileChooser.setDialogTitle(dialogTitle);
		fileChooser.setApproveButtonText(approveButtonText);
		int result = fileChooser.showSaveDialog(mainWindow);
		return result == JFileChooser.APPROVE_OPTION ? Optional.of(fileChooser.getSelectedFile()) : Optional.empty();
	}

	private static File ensurePdfExtension(File file) {
		return file != null && !file.getName().endsWith(".pdf") ? new File(file.getAbsolutePath() + ".pdf") : file;
	}

	private static void ensureMinimumSize(JFrame frame) {
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				Rectangle bounds = frame.getBounds();
				Dimension minSize = frame.getMinimumSize();
				if (bounds.width < minSize.width) {
					bounds.width = minSize.width;
				}
				if (bounds.height < minSize.height) {
					bounds.height = minSize.height;
				}
				frame.setBounds(bounds); // Won't repaint unless it is resized and/or moved
			}
		});
	}

	private static Box vbox(Component... components) {
		Box box = Box.createVerticalBox();
		for (Component component : components) {
			box.add(component);
		}
		return box;
	}

	private static Box hbox(Component... components) {
		Box box = Box.createHorizontalBox();
		for (Component component : components) {
			box.add(component);
		}
		return box;
	}

	private static Dimension enclose(Dimension... dimensions) {
		Dimension result = new Dimension();
		for (Dimension dim : dimensions) {
			result.width = Math.max(result.width, dim.width);
			result.height = Math.max(result.height, dim.height);
		}
		return result;
	}

	private static void showErrorFor(Component component, Exception exception) {
		StringWriter sw = new StringWriter();
		exception.printStackTrace(new PrintWriter(sw));
		String errorMessage = sw.toString();
		Window window = SwingUtilities.getWindowAncestor(component);
		JOptionPane.showMessageDialog(window, errorMessage, ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
	}

	private static LocalDate parseOrDefault(String text, LocalDate defaultValue) {
		if (text == null) {
			return defaultValue;
		} else {
			return LocalDate.parse(text, Launcher.DATE_FORMATTER);
		}
	}

	@SuppressWarnings("SameParameterValue")
	private static Dimension preferredSizeOf(String text) {
		return new JLabel(text).getPreferredSize();
	}

	private static @NotNull JButton createButton(String text, ActionListener actionListener) {
		JButton removeSelectedRowsButton = new JButton(text);
		removeSelectedRowsButton.addActionListener(actionListener);
		return removeSelectedRowsButton;
	}

	private DatePicker configure(DatePicker datePicker) {
		DatePickerSettings settings = datePicker.getSettings();
		settings.setLocale(LOCALE);
		settings.setAllowEmptyDates(false);
		settings.setHighlightPolicy(this::highlightDate);

		JButton toggleButton = datePicker.getComponentToggleCalendarButton();
		ImageIcon icon = loadIcon(ImageIcon::new, "/icons/calendar-date-small.png");
		toggleButton.setIcon(icon);
		toggleButton.setText("");

		JTextField dateTextField = datePicker.getComponentDateTextField();
		dateTextField.setMargin(new Insets(0, 0, 0, 0));

		return datePicker;
	}

	private Pair<DatePicker> createDatePickerPair(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("The from date must not be after the to date");
		}

		DatePicker fromPicker = configure(new DatePicker());
		fromPicker.setDate(from);

		DatePicker toPicker = configure(new DatePicker());
		toPicker.setDate(to);

		fromPicker.getSettings().setVetoPolicy(date -> !toPicker.getDate().isBefore(date));
		toPicker.getSettings().setVetoPolicy(date -> !fromPicker.getDate().isAfter(date));

		return new Pair<>(fromPicker, toPicker);
	}

	@SuppressWarnings("SameParameterValue")
	private Pair<DateTableEditor> createDateTableEditorPair(TableModel dateTitlesModel, int columnFrom, int columnTo) {
		DateTableEditor fromEditor = new DateTableEditor(true, true, true);
		fromEditor.clickCountToEdit = 2;

		DateTableEditor toEditor = new DateTableEditor(true, true, true);
		toEditor.clickCountToEdit = 2;

		// Also configures the date pickers
		DatePicker fromPicker = configure(fromEditor.getDatePicker());
		DatePicker toPicker = configure(toEditor.getDatePicker());

		fromPicker.setDate(FAR_AWAY_DATE); // Used because a date needs to be selected when setting the veto policy...
		toPicker.setDate(FAR_AWAY_DATE);
		fromPicker.getSettings().setVetoPolicy(date ->
				isValidDate(fromPicker, date, dateTitlesModel, columnFrom, columnTo,
						(candidate, compare) -> !candidate.isAfter(compare))
		);
		toPicker.getSettings().setVetoPolicy(date ->
				isValidDate(toPicker, date, dateTitlesModel, columnTo, columnFrom,
						(candidate, compare) -> !candidate.isBefore(compare))
		);

		return new Pair<>(fromEditor, toEditor);
	}

	/// Determines if a date is valid for a pair of columns in the given table model and current editor date.
	///
	/// Valid dated must conform to the following:
	/// * the value is unique within the column
	/// * the value passes the `validityCheck` with the value in the comparison column of the same row
	///
	/// @param picker        the date picker being validated
	/// @param newValue      a possible value to test
	/// @param model         the table model to use
	/// @param editColumn    the column being edited
	/// @param compareColumn the column to compare to
	/// @param validityCheck a check to test if the new value is valid in combination with the
	/// @return true iff the value is valid
	private static boolean isValidDate(DatePicker picker, LocalDate newValue, TableModel model, int editColumn,
	                                   int compareColumn, BiPredicate<LocalDate, LocalDate> validityCheck) {
		// Use the value from the text field to lookup the current value.
		// This is more reliable than getDate(), as that is lagging in our table scenario.
		LocalDate previousValue = getDateFromTextField(picker);
		// Special case: self is always OK
		if (newValue.equals(previousValue)) {
			return true;
		}
		// First find our row
		int editingRow = findRow(model, editColumn, previousValue, -1);
		// Then test uniqueness
		int newValueRow = findRow(model, editColumn, newValue, editingRow);
		if (newValueRow >= 0) {
			return false; // Value is already present in the column
		}
		// Finally compare against the value in the other column
		if (editingRow >= 0) {
			LocalDate comparedValue = (LocalDate) model.getValueAt(editingRow, compareColumn);
			return validityCheck.test(newValue, comparedValue);
		}
		return true;
	}

	private static @Nullable LocalDate getDateFromTextField(DatePicker picker) {
		String text = picker.getText();
		return Stream.concat(Stream.of(
						picker.getSettings().getFormatForDatesCommonEra(),
						picker.getSettings().getFormatForDatesBeforeCommonEra()),
				picker.getSettings().getFormatsForParsing().stream()
		).flatMap(format -> {
			try {
				LocalDate parsed = LocalDate.parse(text, format);
				return format.format(parsed).equals(text) ? Stream.of(parsed) : Stream.empty();
			} catch (Exception ignored) {
				return Stream.empty();
			}
		}).findFirst().orElse(null);
	}

	private static int findRow(TableModel model, int column, Object value, int rowToSkip) {
		for (int row = 0; row < model.getRowCount(); row++) {
			if (row == rowToSkip) {
				continue;
			}
			Object rowValue = model.getValueAt(row, column);
			if (rowValue.equals(value)) {
				return row;
			}
		}
		return -1;
	}

	private static LocalDate[] correctPeriod(LocalDate[] range) {
		assert range != null && range.length == 2;
		return new LocalDate[]{
				range[0] == null ? null : range[0].with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
				range[1] == null ? null : range[1].with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
		};
	}

	private static LocalDate min(LocalDate d0, LocalDate d1) {
		return d0.isBefore(d1) ? d0 : d1;
	}

	private static LocalDate max(LocalDate d0, LocalDate d1) {
		return d0.isBefore(d1) ? d1 : d0;
	}

	HighlightInformation highlightDate(LocalDate date) {
		EnumMap<Holidays.Type, String> descriptions = holidays.describe(date);
		if (!descriptions.isEmpty()) {
			HighlightInformation info = new HighlightInformation();
			info.colorBackground = switch (descriptions.keySet().iterator().next()) {
				case HOLIDAY -> COLOR_HOLIDAY;
				case SPECIAL -> COLOR_SPECIAL;
				case OTHER -> COLOR_OTHER;
				default -> COLOR_NORMAL;
			};
			info.colorText = Color.BLACK; // Also default
			info.tooltipText = String.join("\n", descriptions.values());
			return info;
		}
		return null;
	}

	private record Pair<T>(T left, T right) {}
}
