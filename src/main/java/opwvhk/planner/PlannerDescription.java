package opwvhk.planner;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static java.util.Comparator.naturalOrder;

/**
 * Description of a planner.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe — Kind</a>
 */
public record PlannerDescription(String title, String subtitle, int timeTablePages, int notesPages, int mindmapPages,
                                 int numClasses, ClassItemStructure classItemStructure, EnumSet<StaticPage> staticPages,
                                 List<DateTitle> dateTitles) {
	public PlannerDescription(String title, String subtitle, int timeTablePages, int notesPages, int mindmapPages,
	                          int numClasses, ClassItemStructure classItemStructure, EnumSet<StaticPage> staticPages,
	                          Map<LocalDate, String> dateTitles) {
		this(title, subtitle, timeTablePages, notesPages, mindmapPages, numClasses, classItemStructure, staticPages,
				asDateTitleList(dateTitles));
	}

	private static List<DateTitle> asDateTitleList(Map<LocalDate, String> dateTitles) {
		return dateTitles.entrySet().stream().map(e -> new DateTitle(e.getKey(), e.getValue())).toList();
	}

	public NavigableMap<LocalDate, String> sortedDateTitles() {
		final NavigableMap<LocalDate, String> sortedDateTitles = new TreeMap<>(naturalOrder());
		dateTitles.forEach(dt -> sortedDateTitles.put(dt.date(), dt.text()));
		return sortedDateTitles;
	}
}
