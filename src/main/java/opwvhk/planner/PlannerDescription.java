package opwvhk.planner;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.EnumSet;
import java.util.List;
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
/// @param startDate          the first date in the planner
/// @param endDate            the last date of the planner
/// @param dateTitleFromToList the names of all special periods, with their first and last dates
/// @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe — Kind</a>
public record PlannerDescription(String title, String schoolYear, int timeTablePages, int notesPages, int mindmapPages,
                                 int numClasses, ClassItemStructure classItemStructure, EnumSet<StaticPage> staticPages,
                                 LocalDate startDate, LocalDate endDate, List<DateTitleFromTo> dateTitleFromToList) {

	public PlannerDescription fixStartAndEndDate(TemporalAdjuster makeStartDate, TemporalAdjuster makeEndDate) {
		return new PlannerDescription(title, schoolYear, timeTablePages, notesPages, mindmapPages, numClasses,
				classItemStructure, staticPages, startDate.with(makeStartDate), endDate.with(makeEndDate),
				dateTitleFromToList);
	}

	public NavigableMap<LocalDate, String> dateTitles() {
		NavigableMap<LocalDate, String> textsByStartDate = new TreeMap<>();
		for (DateTitleFromTo dateTitleFromTo : dateTitleFromToList) {
			LocalDate from = dateTitleFromTo.from();
			LocalDate to = dateTitleFromTo.to().plusDays(1); // Start of next text
			Map.Entry<LocalDate, String> lastEntry = textsByStartDate.floorEntry(from);
			// Is the last text entry split? Then restore it.
			// (no need to test for empty string / end of last entry: the result would be the same)
			String lastEntryText = lastEntry != null ? lastEntry.getValue() : "";
			textsByStartDate.put(from, dateTitleFromTo.text());
			textsByStartDate.put(to, lastEntryText);
		}
		return textsByStartDate;
	}

}
