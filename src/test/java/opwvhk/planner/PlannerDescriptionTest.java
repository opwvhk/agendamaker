package opwvhk.planner;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class PlannerDescriptionTest {
	@Test
	void testHolidayHandling() {
		List<DateTitleFromTo> holidays = List.of(
				new DateTitleFromTo("Herfstvakantie", LocalDate.parse("2025-10-11"), 9),
				new DateTitleFromTo("Kerstvakantie", LocalDate.parse("2025-12-20"), 16),
				new DateTitleFromTo("1e Kerstdag", LocalDate.parse("2025-12-25")),
				new DateTitleFromTo("2e Kerstdag", LocalDate.parse("2025-12-26")),
				new DateTitleFromTo("Voorjaarsvakantie", LocalDate.parse("2026-02-14"), 9),
				new DateTitleFromTo("1e Paasdag", LocalDate.parse("2026-04-05")),
				new DateTitleFromTo("2e Paasdag", LocalDate.parse("2026-04-06")),
				new DateTitleFromTo("Meivakantie", LocalDate.parse("2026-04-18"), 16),
				new DateTitleFromTo("Koningsdag", LocalDate.parse("2026-04-27")),
				new DateTitleFromTo("Bevrijdingsdag", LocalDate.parse("2026-05-05")),
				new DateTitleFromTo("Hemelvaart", LocalDate.parse("2026-05-14")),
				new DateTitleFromTo("(dag na Hemelvaart)", LocalDate.parse("2026-05-15")),
				new DateTitleFromTo("1e Pinksterdag", LocalDate.parse("2026-05-24")),
				new DateTitleFromTo("2e Pinksterdag", LocalDate.parse("2026-05-25")),
				new DateTitleFromTo("Zomervakantie", LocalDate.parse("2026-07-11"), 44)
		);

		LocalDate startDate = LocalDate.parse("2025-08-20");
		LocalDate endDate = LocalDate.parse("2026-02-20");
		PlannerDescription plannerDescription = new PlannerDescription(null, null, 0, 0, 0, 0, null, EnumSet.noneOf(
				StaticPage.class), startDate, endDate, holidays);

		assertThat(plannerDescription.dateTitles()).containsExactly(
				entry(LocalDate.parse("2025-08-20"), ""),
				entry(LocalDate.parse("2025-10-11"), "Herfstvakantie"),
				entry(LocalDate.parse("2025-10-20"), ""),
				entry(LocalDate.parse("2025-12-20"), "Kerstvakantie"),
				entry(LocalDate.parse("2025-12-25"), "1e Kerstdag"),
				entry(LocalDate.parse("2025-12-26"), "2e Kerstdag"),
				entry(LocalDate.parse("2025-12-27"), "Kerstvakantie"),
				entry(LocalDate.parse("2026-01-05"), ""),
				entry(LocalDate.parse("2026-02-14"), "Voorjaarsvakantie"),
				entry(LocalDate.parse("2026-02-20"), "Voorjaarsvakantie")
		);
	}
}
