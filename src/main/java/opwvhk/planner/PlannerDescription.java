package opwvhk.planner;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.EnumSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/// Description of a planner.
///
/// The `dateTitles` is special: the lowest and highest dates are used as start and end date for the
/// planner, and map to a text describing the period starting on that date, e.g. "Fall Holiday". The
/// first date after that usually has an empty string as text, starting a normal school period.
///
/// @param title              tile of the planner, e.g. "My Planner"
/// @param schoolYear         description of the school year, e.g. "2025 – 2026"
/// @param timeTablePages     the number of timetable pages
/// @param notesPages         the number of notes pages
/// @param mindmapPages       the number of mind map pages
/// @param numClasses         the number of classes per day
/// @param classItemStructure what class items look like
/// @param staticPages        the static pages to include
/// @param dateTitles         the names of all special periods, mapped by start date
/// @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe — Kind</a>
public record PlannerDescription(String title, String schoolYear, int timeTablePages, int notesPages, int mindmapPages,
                                 int numClasses, ClassItemStructure classItemStructure, EnumSet<StaticPage> staticPages,
                                 NavigableMap<LocalDate, String> dateTitles) {

	public PlannerDescription fixStartAndEndDate(TemporalAdjuster makeStartDate, TemporalAdjuster makeEndDate) {
		NavigableMap<LocalDate, String> sortedDateTitles = new TreeMap<>(dateTitles);

		Map.Entry<LocalDate, String> firstEntry = sortedDateTitles.firstEntry();
		LocalDate startDate = firstEntry.getKey().with(makeStartDate);
		if (!startDate.equals(firstEntry.getKey())) {
			sortedDateTitles.put(startDate, sortedDateTitles.remove(firstEntry.getKey()));
		}

		Map.Entry<LocalDate, String> lastEntry = sortedDateTitles.lastEntry();
		LocalDate endDate = lastEntry.getKey().with(makeEndDate);
		if (!endDate.equals(lastEntry.getKey())) {
			sortedDateTitles.put(endDate, sortedDateTitles.remove(lastEntry.getKey()));
		}

		return new PlannerDescription(title, schoolYear, timeTablePages, notesPages, mindmapPages,
				numClasses, classItemStructure, staticPages,
				sortedDateTitles.subMap(startDate, true, endDate, true));
	}
}
