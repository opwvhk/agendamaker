package opwvhk.planner;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

/// Description of a planner.
///
/// The `dateTitles` is special: the lowest and highest dates are used as start and end date for the
/// planner, and map to a text describing the period starting on that date, e.g. "Fall Holiday". The
/// first date after that usually has an empty string as text, starting a normal school period.
///
/// @param title               tile of the planner, e.g. "My Planner"
/// @param schoolYear          description of the school year, e.g. "2025 – 2026"
/// @param timeTablePages      the number of timetable pages
/// @param notesPages          the number of notes pages
/// @param mindmapPages        the number of mind map pages
/// @param numClasses          the number of classes per day
/// @param classItemStructure  what class items look like
/// @param staticPages         the static pages to include
/// @param startDate           the first date in the planner
/// @param endDate             the last date of the planner
/// @param dateTitleFromToList the names of all special periods, with their first and last dates
/// @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe — Kind</a>
public record PlannerDescription(String title, String schoolYear, int timeTablePages, int notesPages, int mindmapPages,
                                 int numClasses, ClassItemStructure classItemStructure, EnumSet<StaticPage> staticPages,
                                 LocalDate startDate, LocalDate endDate, List<DateTitleFromTo> dateTitleFromToList) {

	public PlannerDescription {
		require(title == null || schoolYear != null, "If there is a title, schoolYear cannot be null");
		require(timeTablePages >= 0, "The number of time table pages cannot be negative.");
		require(notesPages >= 0, "The number of notes pages cannot be negative.");
		require(mindmapPages >= 0, "The number of mindmap pages cannot be negative.");
		require(numClasses >= 0, "The number of classes cannot be negative.");
		requireNonNull(staticPages, "The set of static pages cannot be null.");
		requireNonNull(startDate, "The start date cannot be null.");
		requireNonNull(endDate, "The end date cannot be null.");
		require(startDate.isBefore(endDate), "The start date must be before end date.");
		requireNonNull(dateTitleFromToList, "The list of special periods cannot be null.");

		// Test the periods don't partially overlap (subperiods are OK).
		// We sort periods by ascending start and descending end to enable easier testing & processing in dateTitles().

		List<DateTitleFromTo> periods = new ArrayList<>(dateTitleFromToList);
		periods.sort(comparing(DateTitleFromTo::from).thenComparing(comparing(DateTitleFromTo::to).reversed()));
		LocalDate previousEnd = null;
		for (DateTitleFromTo period : periods) {
			LocalDate end = getLocalDate(period, previousEnd);
			if (previousEnd == null || previousEnd.isBefore(end)) {
				previousEnd = end;
			}
		}
	}

	private static LocalDate getLocalDate(DateTitleFromTo period, LocalDate previousEnd) {
		LocalDate start = period.from();
		LocalDate end = period.to();
		if (previousEnd != null && !start.isAfter(previousEnd) && end.isAfter(previousEnd)) {
			throw new IllegalArgumentException(
					"The given periods must not partially overlap: every pair of periods must " +
					"either be disjoint, or one must be fully contained in the other. For " +
					"fully overlapping periods, the inner periods takes precedence. For " +
					"partially overlapping periods, there is no simple conflict resolution.");
		}
		return end;
	}

	public PlannerDescription fixStartAndEndDate(TemporalAdjuster makeStartDate, TemporalAdjuster makeEndDate) {
		return new PlannerDescription(title, schoolYear, timeTablePages, notesPages, mindmapPages, numClasses,
				classItemStructure, staticPages, startDate.with(makeStartDate), endDate.with(makeEndDate),
				dateTitleFromToList);
	}

	public NavigableMap<LocalDate, String> dateTitles() {
		List<DateTitleFromTo> periodTexts = new ArrayList<>(dateTitleFromToList);
		// Sort by start (ascending), then end (descending): this ensures the algorithm below works correctly.
		periodTexts.sort(comparing(DateTitleFromTo::from).thenComparing(comparing(DateTitleFromTo::to).reversed()));
		NavigableMap<LocalDate, String> textsByStartDate = new TreeMap<>();
		for (DateTitleFromTo dateTitleFromTo : periodTexts) {
			// As we know that the periods are (now) sorted by ascending start and descending end and do not partially
			// overlap, we only need to check that:
			// * If the last period started before ours, we must continue it afterwards (if needed).
			// * If there was no last period before ours, or is ended already, we simply end.
			// * No other cases exist.
			LocalDate from = dateTitleFromTo.from();
			LocalDate to = dateTitleFromTo.to().plusDays(1); // Start of next text

			String lastEntryText = emptyIfNull(floorValue(textsByStartDate, to));
			textsByStartDate.put(from, dateTitleFromTo.text());
			textsByStartDate.putIfAbsent(to, lastEntryText);
		}
		NavigableMap<LocalDate, String> dateTitles = textsByStartDate.subMap(startDate, true, endDate, true);
		dateTitles.putIfAbsent(startDate, emptyIfNull(floorValue(textsByStartDate, startDate)));
		dateTitles.putIfAbsent(endDate, emptyIfNull(floorValue(textsByStartDate, endDate)));
		return dateTitles;
	}

	private <K, V> V floorValue(NavigableMap<K, V> map, K key) {
		Map.Entry<K, V> entry = map.floorEntry(key);
		return entry == null ? null : entry.getValue();
	}

	private String emptyIfNull(String value) {
		return value == null ? "" : value;
	}

	private void require(boolean condition, String errorMessage) {
		if (!condition) {
			throw new IllegalArgumentException(errorMessage);
		}
	}
}
