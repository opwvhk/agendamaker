package opwvhk;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.Month.APRIL;
import static java.time.Month.AUGUST;
import static java.time.Month.DECEMBER;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static java.time.Month.MARCH;
import static java.time.Month.MAY;
import static java.time.Month.NOVEMBER;
import static java.time.Month.OCTOBER;
import static java.time.Month.SEPTEMBER;
import static java.time.temporal.TemporalAdjusters.dayOfWeekInMonth;
import static java.time.temporal.TemporalAdjusters.firstInMonth;

public class Holidays {
	// The only supported locale (for now?).
	private static final Locale LOCALE = Locale.forLanguageTag("nl-NL");

	private final EnumSet<Type> types;
	private final SequencedMap<LocalDate, EnumMap<Type, String>> cache;
	private final SequencedSet<Integer> cachedYears;

	public Holidays(Locale locale, int initialYear, Type... types) {
		this(locale, initialYear, EnumSet.copyOf(List.of(types)));
	}

	public Holidays(Locale locale, int initialYear, EnumSet<Type> types) {
		if (!locale.equals(LOCALE)) {
			throw new IllegalArgumentException("Unsupported Locale: must be nl-NL");
		}
		this.types = types;
		cache = new TreeMap<>();
		cachedYears = new TreeSet<>();
		determineHolidaysForYear(initialYear);
	}

	private void determineHolidaysForYear(int year) {
		LocalDate easter = easter(year);

		if (types.contains(Type.HOLIDAY)) {
			addDate(LocalDate.of(year, JANUARY, 1), Type.HOLIDAY, "Nieuwjaarsdag");
			addDate(LocalDate.of(year, DECEMBER, 25), Type.HOLIDAY, "Eerste Kerstdag");
			addDate(LocalDate.of(year, DECEMBER, 26), Type.HOLIDAY, "Tweede Kerstdag");

			LocalDate kingsDay = null;
			String kingsDayName = null;
			if (year >= 2014) {
				kingsDay = LocalDate.of(year, APRIL, 27);
				kingsDayName = "Koningsdag";
			} else if (year >= 1949) {
				kingsDay = LocalDate.of(year, APRIL, 30);
				kingsDayName = "Koninginnedag";
			} else if (year >= 1891) {
				kingsDay = LocalDate.of(year, AUGUST, 31);
				kingsDayName = "Koninginnedag";
			} else if (year >= 1885) {
				kingsDay = LocalDate.of(year, AUGUST, 31);
				kingsDayName = "Prinsessedag";
			}
			if (kingsDay != null) {
				if (kingsDay.getDayOfWeek() == SUNDAY) {
					kingsDay = kingsDay.plusDays(year >= 1980 ? -1 : 1);
				}
				addDate(kingsDay, Type.HOLIDAY, kingsDayName);
			}

			if (year >= 1945 && year % 5 == 0) {
				addDate(LocalDate.of(year, MAY, 5), Type.HOLIDAY, "Bevrijdingsdag");
			}

			addDate(easter.plusDays(0), Type.HOLIDAY, "Eerste Paasdag");
			addDate(easter.plusDays(1), Type.HOLIDAY, "Tweede Paasdag");
			addDate(easter.plusDays(39), Type.HOLIDAY, "Hemelvaart");
			addDate(easter.plusDays(49), Type.HOLIDAY, "Eerste Pinksterdag");
			addDate(easter.plusDays(50), Type.HOLIDAY, "Tweede Pinksterdag");

		}
		if (types.contains(Type.SPECIAL)) {
			addDate(LocalDate.of(year, JANUARY, 6), Type.SPECIAL, "Driekoningen");
			addDate(LocalDate.of(year, FEBRUARY, 14), Type.SPECIAL, "Valentijnsdag");
			addDate(LocalDate.of(year, MAY, 1), Type.SPECIAL, "Dag van de arbeid");
			addDate(LocalDate.of(year, MAY, 4), Type.SPECIAL, "Dodenherdenking");
			if (year >= 1945 && year % 5 != 0) {
				addDate(LocalDate.of(year, MAY, 5), Type.SPECIAL, "Bevrijdingsdag");
			}
			addDate(LocalDate.of(year, OCTOBER, 4), Type.SPECIAL, "Dierendag");
			addDate(LocalDate.of(year, NOVEMBER, 11), Type.SPECIAL, "Sint Maarten");
			addDate(LocalDate.of(year, DECEMBER, 5), Type.SPECIAL, "Sinterklaas");
			addDate(LocalDate.of(year, DECEMBER, 31), Type.SPECIAL, "Oudejaarsdag");
			addDate(easter.minusDays(2), Type.SPECIAL, "Goede Vrijdag");

			LocalDate secondSundayInMay = LocalDate.of(year, MAY, 1).with(dayOfWeekInMonth(2, SUNDAY));
			addDate(secondSundayInMay, Type.SPECIAL, "Moederdag");
			LocalDate thirdSundayInJune = LocalDate.of(year, JUNE, 1).with(dayOfWeekInMonth(3, SUNDAY));
			addDate(thirdSundayInJune, Type.SPECIAL, "Vaderdag");
			LocalDate thirdTuesdayInSeptember = LocalDate.of(year, SEPTEMBER, 1).with(dayOfWeekInMonth(3, TUESDAY));
			addDate(thirdTuesdayInSeptember, Type.SPECIAL, "Prinsjesdag");
		}
		if (types.contains(Type.NORMAL)) {
			LocalDate date = LocalDate.of(year, JANUARY, 1).with(firstInMonth(SATURDAY));
			if (date.getDayOfMonth() == 7) {
				date = date.minusDays(6);
			}

			LocalDate nextYear = LocalDate.ofYearDay(year + 1, 1);
			while (date.isBefore(nextYear)) {
				if (date.getDayOfWeek() == SATURDAY) {
					addDate(date, Type.NORMAL, "Zaterdag");
					date = date.plusDays(1);
				} else {
					addDate(date, Type.NORMAL, "Zondag");
					date = date.plusDays(6);
				}
			}
		}
		if (types.contains(Type.OTHER)) {
			addDate(LocalDate.of(year, OCTOBER, 31), Type.OTHER, "Halloween");
			addDate(easter.minusDays(49), Type.OTHER, "Carnaval");
			addDate(easter.minusDays(48), Type.OTHER, "Carnaval");
			addDate(easter.minusDays(47), Type.OTHER, "Carnaval");
		}

		cachedYears.add(year);
	}

	private void addDate(LocalDate date, Type type, String description) {
		cache.computeIfAbsent(date, ignored -> new EnumMap<>(Type.class)).put(type, description);
	}

	/// Determine the date of Easter in a year. Uses the "Meeus/Jones/Butcher" algorithm.
	///
	/// @param year a year
	/// @return the date of Easter for the given year
	/// @see <a
	/// href="https://www.algorithm-archive.org/contents/computus/computus.html">https://www.algorithm-archive
	/// .org/contents/computus/computus.html</a>
	/// @see <a
	/// href="https://en.wikipedia.org/wiki/Date_of_Easter#Anonymous_Gregorian_algorithm">https://en.wikipedia.org/wiki/Date_of_Easter</a>
	private LocalDate easter(int year) {
		// Year's position on the 19 year metonic cycle
		int a = year % 19;
		// Century index
		int k = year / 100;
		// Shift of metonic cycle, add a day offset every 300 years
		int p = (13 + 8 * k) / 25;
		// Correction for non-observed leap days
		int q = k / 4;
		// Correction to starting point of calculation each century
		int M = (15 - p + k - q) % 30;
		// Number of days from March 21st until the full moon
		int d = (19 * a + M) % 30;
		// Finding the next Sunday
		// Century-based offset in weekly calculation
		int N = (4 + k - q) % 7;
		// Correction for leap days
		int b = year % 4;
		int c = year % 7;
		// Days from d to next Sunday
		int e = (2 * b + 4 * c + 6 * d + N) % 7;
		// Historical corrections for April 26 and 25
		if ((d == 29 && e == 6) || (d == 28 && e == 6 && a > 10)) {
			e = -1;
		}
		// Determination of the correct month for Easter
		int day = 22 + d + e;
		if (day > 31) {
			return LocalDate.of(year, APRIL, day - 31);
		} else {
			return LocalDate.of(year, MARCH, day);
		}
	}

	public EnumMap<Type, String> describe(LocalDate date) {
		int year = date.getYear();
		if (!cachedYears.contains(year)) {
			determineHolidaysForYear(year);
		}
		EnumMap<Type, String> descriptions = cache.get(date);
		return descriptions == null ? new EnumMap<>(Type.class) : descriptions;
	}

	/// Holiday types. There are regular holidays (days off), special days, other days and normal days (like weekends).
	public enum Type {
		HOLIDAY, SPECIAL, OTHER, NORMAL
	}
}
