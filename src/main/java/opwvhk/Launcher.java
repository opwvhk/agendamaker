package opwvhk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.miginfocom.swing.MigLayout;
import opwvhk.planner.ClassItemStructure;
import opwvhk.planner.DateTitle;
import opwvhk.planner.PlannerDescription;
import opwvhk.planner.PlannerGenerator;
import opwvhk.planner.StaticPage;
import opwvhk.swing.DesktopApp;
import opwvhk.swing.FormBuilder;
import org.jetbrains.annotations.NotNull;

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
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import static opwvhk.planner.ClassItemStructure.CLASS_ROOM_SINGLE;

public class Launcher extends DesktopApp {
	public static final String APPLICATION_NAME = "Agendamaker";
	private static final Locale LOCALE = Locale.forLanguageTag("NL-nl");
	private static final String DATE_FORMAT_PATTERN = "d MMM yyyy";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN)
			.withLocale(LOCALE);
	private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT = ThreadLocal.withInitial(
			() -> new SimpleDateFormat(DATE_FORMAT_PATTERN, LOCALE));
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
	private static final String ERROR_TITLE = "Oeps... vraag een programmeur...";

	public static void main(String[] args) throws IOException, FontFormatException {
		new Launcher().start();
	}

	private final Preferences lastUsedSettings;

	private JSpinner startDateSpinner;
	private JSpinner endDateSpinner;
	private JSpinner numClassesSpinner;
	private AtomicReference<ClassItemStructure> selectedClassItemStructure;
	private JTable dateTitlesTable;
	private JFrame mainWindow;

	public Launcher() {
		super(APPLICATION_NAME, "/icons/schedule2_64.png", "/icons/schedule2_16.png", "/icons/schedule2_24.png",
				"/icons/schedule2_32.png", "/icons/schedule2_128.png", "/icons/schedule2_256.png");
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		lastUsedSettings = preferences.node("lastUsed");
	}

	@Override
	protected String getAboutText() {
		return """
				Agendamaker v0.1
				Genereert de week-pagina's voor schoolagenda's op A4-formaat.
				""";
	}

	public void start() throws IOException, FontFormatException {
		JLabel header = new JLabel(APPLICATION_NAME);

		header.setFont(loadTrueTypeFont("Caveat-Regular", Font.BOLD, (float) 36));
		header.setHorizontalAlignment(SwingConstants.CENTER);
		header.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

		JButton generatePlannerButton = createButton("Maak PDF", this::generatePlanner);
		JButton saveButton = createButton("Bewaar invoer", e -> System.out.println(createPlannerDescription(e)));

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(generatePlannerButton);

		JPanel mainInputPanel = createMainInputPanel();
		JComponent dateTitlesPanel = createDateTitlesPanel();
		Box content = hbox(vbox(mainInputPanel, Box.createVerticalGlue()), dateTitlesPanel);
		mainInputPanel.setMinimumSize(mainInputPanel.getPreferredSize());
		mainInputPanel.setMaximumSize(mainInputPanel.getPreferredSize());

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

	private @NotNull JPanel createMainInputPanel() {

		LocalDate[] initialPeriod = correctPeriod(new LocalDate[]{
				parseOrDefault(lastUsedSettings.get("startDate", null), LocalDate.now()),
				parseOrDefault(lastUsedSettings.get("endDate", null), LocalDate.now())
		});
		startDateSpinner = createDateSpinner(initialPeriod[0]);
		endDateSpinner = createDateSpinner(initialPeriod[1]);

		JLabel actualPlannerPeriodLabel = new JLabel("");
		ChangeListener plannerPeriodChangeListener = e -> {
			LocalDate startDate = toLocalDate((Date) startDateSpinner.getValue());
			LocalDate endDate = toLocalDate((Date) endDateSpinner.getValue());
			LocalDate[] period = correctPeriod(new LocalDate[]{startDate, endDate});
			if (!startDate.equals(period[0]) || !endDate.equals(period[1])) {
				actualPlannerPeriodLabel.setText(
						"(%s t/m %s)".formatted(DATE_FORMATTER.format(period[0]),
								DATE_FORMATTER.format(period[1])));
			} else {
				actualPlannerPeriodLabel.setText("");
			}
		};
		plannerPeriodChangeListener.stateChanged(null); // Set initial label
		startDateSpinner.addChangeListener(
				e -> ((SpinnerDateModel) endDateSpinner.getModel()).setStart((Date) startDateSpinner.getValue()));
		startDateSpinner.addChangeListener(plannerPeriodChangeListener);
		endDateSpinner.addChangeListener(
				e -> ((SpinnerDateModel) startDateSpinner.getModel()).setEnd((Date) endDateSpinner.getValue()));
		endDateSpinner.addChangeListener(plannerPeriodChangeListener);

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
				public void mouseClicked(MouseEvent e) {
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
		builder.add(0, 0, new JLabel("Startdatum"), startDateSpinner);
		builder.add(0, 1, new JLabel("Einddatum"), endDateSpinner);
		builder.add(0, 2, 1, actualPlannerPeriodLabel);
		builder.add(0, 3, new JLabel("Aantal lesuren"), numClassesSpinner, spinnerConstraints);
		int i = 4;
		for (ClassItemStructure cis : ClassItemStructure.values()) {
			int ordinal = cis.ordinal();
			builder.add(0, i++, new JLabel(ordinal == 0 ? "Structuur" : ""), structureChoices.get(ordinal));
		}

		JPanel mainInputPanel = builder.build();
		mainInputPanel.setBorder(BorderFactory.createTitledBorder("Data en layout"));

		return mainInputPanel;
	}

	private @NotNull JPanel createDateTitlesPanel() {
		dateTitlesTable = createDateTitlesTable();

		JScrollPane scrollPane = new JScrollPane(dateTitlesTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		// noinspection SpellCheckingInspection
		JPanel buttonPanel = new JPanel(new MigLayout("nogrid, fillx, aligny 100%, gapy unrel"));
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
			model.addRow(new Object[]{toDate(LocalDate.now()), toDate(LocalDate.now()), ""});
		}), "tag yes");

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(buttonPanel, BorderLayout.SOUTH);
		panel.add(scrollPane, BorderLayout.CENTER);
		panel.setBorder(BorderFactory.createTitledBorder("Belangrijke periodes / vakanties"));
		return panel;
	}

	private static @NotNull JButton createButton(String text, ActionListener actionListener) {
		JButton removeSelectedRowsButton = new JButton(text);
		removeSelectedRowsButton.addActionListener(actionListener);
		return removeSelectedRowsButton;
	}

	private @NotNull JTable createDateTitlesTable() {
		String storedDateTitles = lastUsedSettings.get("dateTitles", """
				[
				  {"from" : "2024-09-02", "to" : "2024-10-25", "text" : ""},
				  {"from" : "2024-10-26", "to" : "2024-11-03", "text" : "Herfstvakantie"},
				  {"from" : "2024-11-04", "to" : "2024-12-20", "text" : ""},
				  {"from" : "2024-12-21", "to" : "2025-01-05", "text" : "Kerstvakantie"},
				  {"from" : "2024-12-25", "to" : "2024-12-25", "text" : "1e Kerstdag"},
				  {"from" : "2024-12-26", "to" : "2024-12-26", "text" : "2e Kerstdag"},
				  {"from" : "2025-01-06", "to" : "2025-02-14", "text" : ""},
				  {"from" : "2025-02-15", "to" : "2025-02-23", "text" : "Voorjaarsvakantie"},
				  {"from" : "2025-02-24", "to" : "2025-04-18", "text" : ""},
				  {"from" : "2025-04-19", "to" : "2025-05-05", "text" : "Meivakantie"},
				  {"from" : "2025-04-21", "to" : "2025-04-21", "text" : "1e Paasdag"},
				  {"from" : "2025-04-22", "to" : "2025-04-22", "text" : "2e Paasdag"},
				  {"from" : "2025-04-27", "to" : "2025-04-27", "text" : "Koningsdag"},
				  {"from" : "2025-05-06", "to" : "2025-05-28", "text" : ""},
				  {"from" : "2025-05-29", "to" : "2025-05-29", "text" : "Hemelvaart"},
				  {"from" : "2025-05-30", "to" : "2025-05-30", "text" : "dag na Hemelvaart (vrij)"},
				  {"from" : "2025-05-31", "to" : "2025-06-07", "text" : ""},
				  {"from" : "2025-06-08", "to" : "2025-06-08", "text" : "1e Pinksterdag"},
				  {"from" : "2025-06-09", "to" : "2025-06-09", "text" : "2e Pinksterdag"},
				  {"from" : "2025-06-10", "to" : "2025-07-11", "text" : ""},
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
		Class<?>[] COLUMN_TYPES = {Date.class, Date.class, String.class};
		DefaultTableModel dateTitlesModel = new DefaultTableModel(COLUMN_NAMES, 0) {
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return COLUMN_TYPES[columnIndex];
			}
		};
		for (DateTitleFromTo row : dateTitleFromToList) {
			dateTitlesModel.addRow(new Object[]{toDate(row.from()), toDate(row.to()), row.text()});
		}
		JTable dateTitlesTable = new JTable(dateTitlesModel);
		dateTitlesTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		dateTitlesTable.getColumnModel().getColumn(0).setMinWidth(110);
		dateTitlesTable.getColumnModel().getColumn(0).setMaxWidth(150);
		dateTitlesTable.getColumnModel().getColumn(0).setCellEditor(
				new DateCellEditor(null, row -> ((Date) dateTitlesModel.getValueAt(row, 1))));
		dateTitlesTable.getColumnModel().getColumn(1).setMinWidth(110);
		dateTitlesTable.getColumnModel().getColumn(1).setMaxWidth(150);
		dateTitlesTable.getColumnModel().getColumn(1).setCellEditor(
				new DateCellEditor(row -> ((Date) dateTitlesModel.getValueAt(row, 0)), null));
		dateTitlesTable.getColumnModel().getColumn(2).setMinWidth(200);
		dateTitlesTable.setDefaultRenderer(Date.class, new DateCellRenderer());
		dateTitlesTable.setDefaultEditor(Date.class, new DateCellEditor());
		return dateTitlesTable;
	}

	private @NotNull PlannerDescription createPlannerDescription(ActionEvent event) {
		LocalDate startDate = toLocalDate((Date) startDateSpinner.getValue());
		LocalDate endDate = toLocalDate((Date) endDateSpinner.getValue());
		int numClasses = (Integer) numClassesSpinner.getValue();
		ClassItemStructure classItemStructure = selectedClassItemStructure.get();
		TableModel dateTitlesModel = dateTitlesTable.getModel();
		List<DateTitleFromTo> dateTitleFromToList = IntStream.range(0, dateTitlesModel.getRowCount())
				.mapToObj(row -> {
					LocalDate from = toLocalDate((Date) dateTitlesModel.getValueAt(row, 0));
					LocalDate to = toLocalDate((Date) dateTitlesModel.getValueAt(row, 1));
					String text = (String) dateTitlesModel.getValueAt(row, 2);
					// No fields are null
					return new DateTitleFromTo(from, to, text == null ? "" : text);
				})
				.sorted(Comparator.comparing(DateTitleFromTo::from))
				.toList();
		System.out.printf("dateTitleFromToList = %s\n", dateTitleFromToList);
		NavigableMap<LocalDate, String> textsByStartDate = new TreeMap<>();
		for (DateTitleFromTo dateTitleFromTo : dateTitleFromToList) {
			LocalDate from = dateTitleFromTo.from();
			LocalDate to = dateTitleFromTo.to().plusDays(1); // Start of next text
			NavigableMap<LocalDate, String> subMap = textsByStartDate.subMap(from, true, to, true);
			Map.Entry<LocalDate, String> lastEntry = subMap.lastEntry();
			// Is the last text entry split? Then restore it.
			// (no need to test for closing empty string: the result would be the same)
			String lastEntryText = lastEntry != null ? lastEntry.getValue() : "";
			subMap.clear();
			textsByStartDate.put(from, dateTitleFromTo.text());
			textsByStartDate.put(to, lastEntryText);
		}
		textsByStartDate.putIfAbsent(startDate, "");
		textsByStartDate.putIfAbsent(endDate, "");
		List<DateTitle> dateTitles = textsByStartDate.entrySet().stream()
				.dropWhile(entry -> entry.getKey().isBefore(startDate))
				.takeWhile(entry -> !entry.getKey().isAfter(endDate))
				.map(entry -> new DateTitle(entry.getKey(), entry.getValue()))
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
		return new PlannerDescription("", "", 0, 0, 0, numClasses, classItemStructure,
				EnumSet.noneOf(StaticPage.class), dateTitles);
	}

	private void generatePlanner(ActionEvent event) {
		JFileChooser fileChooser = new JFileChooser(new File("."));
		fileChooser.setDialogTitle("Kies een PDF-bestand om de planner in op te slaan");
		fileChooser.setApproveButtonText("Opslaan");
		int result = fileChooser.showSaveDialog(mainWindow);
		File selectedFile = result == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
		if (selectedFile == null) {
			return;
		}
		try {
			try (OutputStream output = new FileOutputStream(selectedFile)) {
				PlannerDescription plannerDescription = createPlannerDescription(event);
				PlannerGenerator plannerGenerator = new PlannerGenerator(plannerDescription);
				plannerGenerator.generate(output);
			}
			openFile(selectedFile);
		} catch (IOException e) {
			showErrorFor((Component) event.getSource(), e);
		}
	}

	private static void showErrorFor(Component component, Exception exception) {
		StringWriter sw = new StringWriter();
		exception.printStackTrace(new PrintWriter(sw));
		String errorMessage = sw.toString();
		Window window = SwingUtilities.getWindowAncestor(component);
		JOptionPane.showMessageDialog(window, errorMessage, ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
	}

	private static LocalDate[] correctPeriod(LocalDate[] range) {
		assert range != null && range.length == 2;
		return new LocalDate[]{
				range[0] == null ? null : range[0].with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
				range[1] == null ? null : range[1].with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
		};
	}

	private static LocalDate parseOrDefault(String text, LocalDate defaultValue) {
		if (text == null) {
			return defaultValue;
		} else {
			return LocalDate.parse(text, Launcher.DATE_FORMATTER);
		}
	}

	private static Date toDate(LocalDate localDate) {
		// java.util.Date dates depend on the system time zone
		return localDate == null ? null : Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private static LocalDate toLocalDate(Date date) {
		// java.util.Date dates depend on the system time zone
		return date == null ? null : LocalDate.from(date.toInstant().atZone(ZoneId.systemDefault()));
	}

	private static @NotNull JSpinner createDateSpinner(LocalDate localDate) {
		return createDateSpinner(localDate, null, null);
	}

	@SuppressWarnings("SameParameterValue")
	private static @NotNull JSpinner createDateSpinner(LocalDate localDate, LocalDate minimum, LocalDate maximum) {
		SpinnerDateModel model = new SpinnerDateModel(toDate(localDate), toDate(minimum), toDate(maximum),
				Calendar.DAY_OF_MONTH);
		JSpinner dateSpinner = new JSpinner(model);
		dateSpinner.setLocale(LOCALE); // The spinner locale MUST be set before creating the editor!
		dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, DATE_FORMAT_PATTERN));
		return dateSpinner;
	}

	private static class DateCellEditor extends DefaultCellEditor {
		private final JSpinner spinner;
		private final IntFunction<Date> minValueLookup;
		private final IntFunction<Date> maxValueLookup;

		public DateCellEditor() {
			this(null, null);
		}

		public DateCellEditor(IntFunction<Date> minValueLookup, IntFunction<Date> maxValueLookup) {
			super(new JTextField());
			this.minValueLookup = minValueLookup;
			this.maxValueLookup = maxValueLookup;
			spinner = createDateSpinner(LocalDate.now());
			editorComponent = spinner;
			this.clickCountToStart = 2;
			delegate = new EditorDelegate() {
				public void setValue(Object value) {
					spinner.setValue(value);
				}

				public Object getCellEditorValue() {
					return spinner.getValue();
				}
			};
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
		                                             int column) {
			super.getTableCellEditorComponent(table, value, isSelected, row, column);
			SpinnerDateModel model = (SpinnerDateModel) spinner.getModel();
			model.setStart(minValueLookup == null ? null : minValueLookup.apply(row));
			model.setEnd(maxValueLookup == null ? null : maxValueLookup.apply(row));
			return spinner;
		}
	}

	private static class DateCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
		                                               boolean hasFocus, int row, int column) {
			String dateString = SIMPLE_DATE_FORMAT.get().format(value);
			return super.getTableCellRendererComponent(table, dateString, isSelected, hasFocus, row, column);
		}
	}

}
