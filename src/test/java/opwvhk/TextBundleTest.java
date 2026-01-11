package opwvhk;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextBundleTest {

	private static final Locale LOCALE_NL = Locale.forLanguageTag("nl-NL");
	private static final Locale LOCALE_EN = Locale.forLanguageTag("en-GB");

	@Test
	void testSimpleMessage() {
		TextBundle bundleEn = new TextBundle("texts", LOCALE_EN);
		TextBundle bundleNl = new TextBundle("texts", LOCALE_NL);

		assertThat(bundleEn.message("timetable")).isEqualTo("Timetable");
		assertThat(bundleNl.message("timetable")).isEqualTo("Lestijden");
	}

	@Test
	void testParameterizedMessage() {
		TextBundle bundleEn = new TextBundle("texts", LOCALE_EN);
		TextBundle bundleNl = new TextBundle("texts", LOCALE_NL);

		assertThat(bundleEn.message("month", "abc")).isEqualTo("Month: abc");
		assertThat(bundleNl.message("month", "abc")).isEqualTo("Maand: abc");
	}

	@Test
	void testMissingMessage() {
		TextBundle bundleEn = new TextBundle("texts", LOCALE_EN);
		TextBundle bundleNl = new TextBundle("texts", LOCALE_NL);

		assertThrows(java.util.MissingResourceException.class, () -> bundleEn.message("missing"));
		assertThrows(java.util.MissingResourceException.class, () -> bundleNl.message("missing"));
	}
}
