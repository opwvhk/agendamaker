package opwvhk.planner;

import java.time.LocalDate;

public record DateTitleFromTo(String text, LocalDate from, LocalDate to) {
	public DateTitleFromTo(String text, LocalDate from, int numberOfDays) {
		// -1 because from is the first day
		this(text, from, from.plusDays(numberOfDays - 1));
	}

	public DateTitleFromTo(String text, LocalDate date) {
		this(text, date, date);
	}
}
