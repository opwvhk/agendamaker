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
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.ActionEvent;
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
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.IntFunction;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import static opwvhk.planner.ClassItemStructure.CLASS_ROOM_DOUBLE;
import static opwvhk.planner.ClassItemStructure.CLASS_ROOM_SINGLE;
import static opwvhk.planner.ClassItemStructure.CLASS_ROOM_TRIPLE;
import static opwvhk.planner.ClassItemStructure.SINGLE_FIELD;
import static opwvhk.planner.StaticPage.EMERGENCY_PLAN;
import static opwvhk.planner.StaticPage.HOW_TO_LEARN;
import static opwvhk.planner.StaticPage.PERSONAL_GOALS;
import static opwvhk.planner.StaticPage.PLANNING_HAND;
import static opwvhk.planner.StaticPage.PLANNING_INSTRUCTIONS;
import static opwvhk.planner.StaticPage.PREREQUISITES_LEARNING;
import static opwvhk.planner.StaticPage.SCHEDULE_AND_VACATIONS;
import static opwvhk.planner.StaticPage.STUDYING_TIPS;
import static opwvhk.planner.StaticPage.SURVIVE_FRESHMAN_YEAR;
import static opwvhk.planner.StaticPage.SURVIVE_LEARNING;
import static opwvhk.planner.StaticPage.USEFUL_STUFF;

public class PlannerGeneratorLauncher extends DesktopApp {
	public static final String APPLICATION_NAME = "Agendamaker";
	private static final EnumMap<ClassItemStructure, String> CLASS_ITEM_STRUCTURES = new EnumMap<>(
			ClassItemStructure.class);
	private static final EnumMap<StaticPage, String> STATIC_PAGES = new EnumMap<>(StaticPage.class);
	private static final Locale LOCALE = Locale.forLanguageTag("NL-nl");
	private static final String DATE_FORMAT_PATTERN = "d MMM yyyy";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN)
			.withLocale(LOCALE);
	private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT = ThreadLocal.withInitial(
			() -> new SimpleDateFormat(DATE_FORMAT_PATTERN, LOCALE));
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
	private static final String ERROR_TITLE = "Oeps... vraag een programmeur...";

	static {
		CLASS_ITEM_STRUCTURES.put(SINGLE_FIELD, "Enkel vak");
		CLASS_ITEM_STRUCTURES.put(CLASS_ROOM_SINGLE, "Vak & 1 regel");
		CLASS_ITEM_STRUCTURES.put(CLASS_ROOM_DOUBLE, "Vak & 2 regels");
		CLASS_ITEM_STRUCTURES.put(CLASS_ROOM_TRIPLE, "Vak & 3 regels");
		STATIC_PAGES.put(EMERGENCY_PLAN, "Noodplan");
		STATIC_PAGES.put(SCHEDULE_AND_VACATIONS, "Lestijden en vakanties");
		STATIC_PAGES.put(SURVIVE_FRESHMAN_YEAR, "Hoe overleef ik de brugklas");
		STATIC_PAGES.put(PLANNING_HAND, "De hand-vragen");
		STATIC_PAGES.put(SURVIVE_LEARNING, "Hoe overleef ik leren?");
		STATIC_PAGES.put(USEFUL_STUFF, "Handige afkortingen en zo");
		STATIC_PAGES.put(STUDYING_TIPS, "Studietips");
		STATIC_PAGES.put(HOW_TO_LEARN, "Voor je begint met leren");
		STATIC_PAGES.put(PREREQUISITES_LEARNING, "Voorwaarden voor leren");
		STATIC_PAGES.put(PLANNING_INSTRUCTIONS, "Hoe overleef ik het plannen?");
		STATIC_PAGES.put(PERSONAL_GOALS, "Persoonlijke doelen");
	}

	public static void main(String[] args) {
		new PlannerGeneratorLauncher().start();
	}

	private final Preferences lastUsedSettings;

	private JSpinner startDateSpinner;
	private JSpinner endDateSpinner;
	private JTextField titleField;
	private JTextField subtitleField;
	private JSpinner timetablesSpinner;
	private JSpinner notePagesSpinner;
	private JSpinner mindmapPagesSpinner;
	private JSpinner numClassesSpinner;
	private JComboBox<ClassItemStructure> classItemStructureComboBox;
	private JCheckBox[] staticPagesCheckBoxes;
	private JTable dateTitlesTable;
	private JFrame mainWindow;

	public PlannerGeneratorLauncher() {
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

	@SuppressWarnings("GrazieInspection")
	public void start() {
		JLabel header = new JLabel(APPLICATION_NAME);
		// noinspection SpellCheckingInspection
		header.setFont(new Font("Snell Roundhand", Font.BOLD, 36));
		header.setHorizontalAlignment(SwingConstants.CENTER);
		header.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

		// noinspection SpellCheckingInspection
		JPanel buttonPanel = new JPanel(new MigLayout("nogrid, fillx, aligny 100%, gapy unrel"));
		// The above layout is for a button bar; these tags help with platform dependent ordering:
		// The tags: ok, cancel, help (usually on the right), help2 (sometimes placed left), yes, no, apply, next,
		// back, finish, left (normally placed far left).
		JButton generatePlannerButton = new JButton("Maak PDF");
		generatePlannerButton.addActionListener(this::generatePlanner);
		buttonPanel.add(generatePlannerButton, "tag finish");
		JButton saveButton = new JButton("Bewaar invoer");
		saveButton.addActionListener(e -> System.out.println(createPlannerDescription(e)));
		buttonPanel.add(saveButton, "tag apply");

		JPanel mainInputPanel = createMainInputPanel();

		JPanel staticPagesPanel = createStaticPagesPanel();

		JComponent dateTitlesPanel = createDateTitlesPanel();

		JPanel content = new JPanel(new MigLayout());
		content.add(mainInputPanel);
		content.add(dateTitlesPanel, "grow, push, span 1 2, wrap");
		content.add(staticPagesPanel);

		// Build and display the window

		mainWindow = createMainWindow();
		mainWindow.setLocationRelativeTo(null); // Center on screen
		// noinspection SpellCheckingInspection
		mainWindow.getRootPane().putClientProperty("apple.awt.fullscreenable", true);
		BorderLayout mainWindowLayout = new BorderLayout();
		mainWindowLayout.setHgap(8);
		mainWindowLayout.setVgap(8);
		mainWindow.setLayout(mainWindowLayout);
		mainWindow.add(header, BorderLayout.NORTH);
		mainWindow.add(content, BorderLayout.CENTER);
		mainWindow.add(buttonPanel, BorderLayout.SOUTH);
		mainWindow.pack();
		mainWindow.setMinimumSize(mainWindow.getPreferredSize());
		mainWindow.setVisible(true);
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
						"(%s t/m %s)".formatted(DATE_FORMATTER.format(period[0]), DATE_FORMATTER.format(period[1])));
			} else {
				actualPlannerPeriodLabel.setText("");
			}
		};
		plannerPeriodChangeListener.stateChanged(null); // Set label
		startDateSpinner.addChangeListener(
				e -> ((SpinnerDateModel) endDateSpinner.getModel()).setStart((Date) startDateSpinner.getValue()));
		startDateSpinner.addChangeListener(plannerPeriodChangeListener);
		endDateSpinner.addChangeListener(
				e -> ((SpinnerDateModel) startDateSpinner.getModel()).setEnd((Date) endDateSpinner.getValue()));
		endDateSpinner.addChangeListener(plannerPeriodChangeListener);

		titleField = new JTextField(lastUsedSettings.get("title", null));
		subtitleField = new JTextField(lastUsedSettings.get("subtitle", null));
		timetablesSpinner = new JSpinner(new SpinnerNumberModel(lastUsedSettings.getInt("timetables", 2), 0, 99, 1));
		notePagesSpinner = new JSpinner(new SpinnerNumberModel(lastUsedSettings.getInt("notesPages", 3), 0, 99, 1));
		mindmapPagesSpinner = new JSpinner(
				new SpinnerNumberModel(lastUsedSettings.getInt("mindmapPages", 3), 0, 99, 1));
		numClassesSpinner = new JSpinner(new SpinnerNumberModel(lastUsedSettings.getInt("numClasses", 7), 3, 10, 1));
		ComboBoxModel<ClassItemStructure> comboBoxModel = new DefaultComboBoxModel<>(ClassItemStructure.values());
		comboBoxModel.setSelectedItem(
				ClassItemStructure.valueOf(lastUsedSettings.get("classItemStructure", CLASS_ROOM_SINGLE.name())));
		classItemStructureComboBox = new JComboBox<>(comboBoxModel);
		classItemStructureComboBox.setRenderer(new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(JList<?> list,
			                                              Object value,
			                                              int index,
			                                              boolean isSelected,
			                                              boolean cellHasFocus) {
				ClassItemStructure cis = (ClassItemStructure) value;
				value = CLASS_ITEM_STRUCTURES.getOrDefault(cis, cis.name());
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		});

		JPanel mainInputPanel = new JPanel(new MigLayout());
		mainInputPanel.setBorder(BorderFactory.createTitledBorder("Titel en layout"));
		JLabel label8 = new JLabel("Startdatum");
		mainInputPanel.add(label8, "sg dateLabels");
		mainInputPanel.add(startDateSpinner, "sg dateSpinners");
		JLabel label7 = new JLabel("Einddatum");
		mainInputPanel.add(label7, "sg dateLabels, gap unrelated");
		mainInputPanel.add(endDateSpinner, "sg dateSpinners, wrap");
		JLabel label6 = new JLabel("Titel");
		mainInputPanel.add(label6);
		mainInputPanel.add(titleField, "span, grow, wrap");
		JLabel label5 = new JLabel("Ondertitel");
		mainInputPanel.add(label5);
		mainInputPanel.add(subtitleField, "span, grow, wrap");
		JLabel label4 = new JLabel("Tijdtabellen");
		mainInputPanel.add(label4);
		mainInputPanel.add(timetablesSpinner);
		JLabel label3 = new JLabel("Aantal lesuren");
		mainInputPanel.add(label3, "gap unrelated");
		mainInputPanel.add(numClassesSpinner, "wrap");
		JLabel label2 = new JLabel("Notitie-pagina's");
		mainInputPanel.add(label2);
		mainInputPanel.add(notePagesSpinner);
		JLabel label1 = new JLabel("Structuur");
		mainInputPanel.add(label1, "gap unrelated");
		mainInputPanel.add(classItemStructureComboBox, "wrap");
		JLabel label = new JLabel("Mindmap-pagina's");
		mainInputPanel.add(label);
		mainInputPanel.add(mindmapPagesSpinner);
		mainInputPanel.add(actualPlannerPeriodLabel, "span");
		return mainInputPanel;
	}

	private @NotNull JPanel createStaticPagesPanel() {
		// noinspection GrazieInspection
		long defaultStaticPages = asBitSet(EnumSet.of(
				EMERGENCY_PLAN,
				SCHEDULE_AND_VACATIONS,
				SURVIVE_FRESHMAN_YEAR,
				PLANNING_HAND,
				// SURVIVE_LEARNING,
				USEFUL_STUFF,
				STUDYING_TIPS,
				HOW_TO_LEARN,
				PREREQUISITES_LEARNING,
				PLANNING_INSTRUCTIONS,
				PERSONAL_GOALS
		));
		EnumSet<StaticPage> staticPages = fromBitSet(StaticPage.class,
				lastUsedSettings.getLong("staticPages", defaultStaticPages));

		JPanel staticPagesPanel = new JPanel(new MigLayout());
		staticPagesPanel.setBorder(BorderFactory.createTitledBorder("Vaste pagina's"));
		staticPagesCheckBoxes = new JCheckBox[StaticPage.values().length];
		int i = 0;
		for (StaticPage sp : StaticPage.values()) {
			JCheckBox jCheckBox = new JCheckBox(STATIC_PAGES.getOrDefault(sp, sp.name()));
			jCheckBox.setSelected(staticPages.contains(sp));
			staticPagesPanel.add(jCheckBox, i % 2 == 0 ? null : "wrap");
			staticPagesCheckBoxes[i++] = jCheckBox;
		}
		return staticPagesPanel;
	}

	private @NotNull JPanel createDateTitlesPanel() {
		dateTitlesTable = createDateTitlesTable();

		JScrollPane scrollPane = new JScrollPane(dateTitlesTable);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		JButton removeSelectedRowsButton = new JButton("Verwijder geselecteerde datumregels");
		removeSelectedRowsButton.addActionListener(e -> {
			DefaultTableModel model = (DefaultTableModel) dateTitlesTable.getModel();
			int[] selectedRows = dateTitlesTable.getSelectedRows();
			// Indices are in ascending order; loop backwards to avoid changing them
			for (int i = selectedRows.length - 1; i >= 0; i--) {
				model.removeRow(selectedRows[i]);
			}
		});
		JButton addRowButton = new JButton("Voeg datumregel toe");
		addRowButton.addActionListener(e -> {
			DefaultTableModel model = (DefaultTableModel) dateTitlesTable.getModel();
			model.addRow(new Object[]{toDate(LocalDate.now()), toDate(LocalDate.now()), ""});
		});
		// noinspection SpellCheckingInspection
		JPanel buttonPanel = new JPanel(new MigLayout("nogrid, fillx, aligny 100%, gapy unrel"));
		buttonPanel.add(removeSelectedRowsButton, "tag no");
		buttonPanel.add(addRowButton, "tag yes");

		JPanel panel = new JPanel(new MigLayout());
		panel.add(buttonPanel, "south");
		panel.add(scrollPane, "grow");
		panel.setBorder(BorderFactory.createTitledBorder("Belangrijke periodes / vakanties"));
		return panel;
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
		dateTitlesTable.setOpaque(true);
		dateTitlesTable.getTableHeader().setOpaque(true);
		dateTitlesTable.getTableHeader().setForeground(dateTitlesTable.getForeground());
		dateTitlesTable.getTableHeader().setBackground(dateTitlesTable.getBackground());
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
		String title = titleField.getText();
		String subtitle = subtitleField.getText();
		int timetables = (Integer) timetablesSpinner.getValue();
		int notePages = (Integer) notePagesSpinner.getValue();
		int mindmapPages = (Integer) mindmapPagesSpinner.getValue();
		int numClasses = (Integer) numClassesSpinner.getValue();
		ClassItemStructure classItemStructure =
				ClassItemStructure.values()[classItemStructureComboBox.getSelectedIndex()];
		EnumSet<StaticPage> staticPages = EnumSet.noneOf(StaticPage.class);
		for (int i = 0; i < StaticPage.values().length; i++) {
			JCheckBox checkBox = staticPagesCheckBoxes[i];
			if (checkBox.isSelected()) {
				staticPages.add(StaticPage.values()[i]);
			}
		}
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
				.map(entry -> new DateTitle(entry.getKey(), entry.getValue()))
				.toList();

		lastUsedSettings.put("startDate", DATE_FORMATTER.format(startDate));
		lastUsedSettings.put("endDate", DATE_FORMATTER.format(endDate));
		lastUsedSettings.put("title", title);
		lastUsedSettings.put("title", title);
		lastUsedSettings.put("subtitle", subtitle);
		lastUsedSettings.putInt("timetables", timetables);
		lastUsedSettings.putInt("notePages", notePages);
		lastUsedSettings.putInt("mindmapPages", mindmapPages);
		lastUsedSettings.putInt("numClasses", numClasses);
		lastUsedSettings.put("classItemStructure", classItemStructure.name());
		lastUsedSettings.putLong("staticPages", asBitSet(staticPages));
		try {
			lastUsedSettings.put("dateTitles", OBJECT_MAPPER.writeValueAsString(dateTitleFromToList));
		} catch (JsonProcessingException e) {
			showErrorFor((Component) event.getSource(), e);
		}
		return new PlannerDescription(title, subtitle, timetables, notePages, mindmapPages, numClasses,
				classItemStructure,
				staticPages, dateTitles);
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

	private static <E extends Enum<E>> EnumSet<E> fromBitSet(@SuppressWarnings("SameParameterValue") Class<E> enumType,
	                                                         long bitset) {
		E[] enumConstants = enumType.getEnumConstants();
		EnumSet<E> enumSet = EnumSet.noneOf(enumType);
		for (int i = 0; i < enumConstants.length; i++) {
			if ((bitset & 1L << i) != 0) {
				enumSet.add(enumConstants[i]);
			}
		}
		return enumSet;
	}

	private static <E extends Enum<E>> long asBitSet(EnumSet<E> enumSet) {
		if (enumSet.isEmpty()) {
			return 0;
		} else {
			// noinspection unchecked: the type parameter of the method ensures this is safe
			Class<E> enumType = (Class<E>) enumSet.iterator().next().getClass();
			E[] enumConstants = enumType.getEnumConstants();
			long bitset = 0;
			for (int i = 0; i < enumConstants.length; i++) {
				if (enumSet.contains(enumConstants[i])) {
					bitset |= 1L << i;
				}
			}
			return bitset;
		}
	}

	private static LocalDate parseOrDefault(String text, LocalDate defaultValue) {
		if (text == null) {
			return defaultValue;
		} else {
			return LocalDate.parse(text, PlannerGeneratorLauncher.DATE_FORMATTER);
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
