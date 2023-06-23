package opwvhk.planner;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static java.util.Comparator.naturalOrder;

/**
 * Description of a planner.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe — Kind</a>
 */
public record PlannerDescription(String title, String subtitle, int timeTablePages, int notesPages, int mindmapPages, List<DateTitle> dateTitles) {
	public NavigableMap<LocalDate, String> sortedDateTitles() {
		final NavigableMap<LocalDate, String> sortedDateTitles = new TreeMap<>(naturalOrder());
		dateTitles.forEach(dt -> sortedDateTitles.put(dt.date(), dt.text()));
		return sortedDateTitles;
	}
}
