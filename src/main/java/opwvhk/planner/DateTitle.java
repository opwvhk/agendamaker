package opwvhk.planner;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * A date with a title. Used in {@link PlannerDescription}.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe — Kind</a>
 */
public record DateTitle(LocalDate date ,String text) implements Serializable { }
