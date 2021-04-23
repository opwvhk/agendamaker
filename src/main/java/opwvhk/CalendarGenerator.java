package opwvhk;

import opwvhk.planner.DateTitle;
import opwvhk.planner.PlannerDescription;
import opwvhk.planner.PlannerGenerator;

import java.io.*;
import java.time.LocalDate;

import static java.time.Month.*;
import static java.util.Arrays.asList;

/**
 * Class to test various stuff with.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe — Kind</a>
 */
public final class CalendarGenerator {
	/**
	 * Utility class: do not instantiate.
	 */
	private CalendarGenerator() {
		// No-op.
	}

	/**
	 * Application entry point.
	 *
	 * @param args the command line arguments
	 * @throws Exception when something goes wrong
	 */
	public static void main(final String... args) throws Exception {
		final PlannerDescription plannerDescription = new PlannerDescription("Planagenda 2020", "toetsperiode 1", 1, asList(
			new DateTitle(LocalDate.of(2020, SEPTEMBER, 29), ""),
			new DateTitle(LocalDate.of(2020, OCTOBER, 12), "Herfstvakantie"),
			new DateTitle(LocalDate.of(2020, OCTOBER, 19), ""),
			new DateTitle(LocalDate.of(2020, NOVEMBER, 4), "TOETSWEEK"),
			new DateTitle(LocalDate.of(2020, NOVEMBER, 7), ""),
			new DateTitle(LocalDate.of(2020, NOVEMBER, 11), "")
		));

		try (final OutputStream output = new FileOutputStream("test.pdf", false)) {
			new PlannerGenerator(plannerDescription).generate(output);
		}
	}
}
