package opwvhk.planner;

import java.time.LocalDate;

import static java.util.Objects.requireNonNull;

public record DateTitleFromTo(String text, LocalDate from, LocalDate to) {
	public DateTitleFromTo {
		requireNonNull(text, "text must not be null");
		requireNonNull(from, "from must not be null");
		requireNonNull(to, "to must not be null");
	}

	public DateTitleFromTo(String text, LocalDate from, int numberOfDays) {
		// -1 because from is the first day
		this(text, from, from.plusDays(numberOfDays - 1));
	}

	public DateTitleFromTo(String text, LocalDate date) {
		this(text, date, date);
	}
}
