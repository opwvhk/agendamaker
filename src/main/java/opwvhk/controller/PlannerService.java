package opwvhk.controller;

import opwvhk.planner.PlannerDescription;
import opwvhk.planner.PlannerGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Main (single) resource. Handles the entire application.
 *
 * @author <a href="mailto:oscar@westravanholthe.nl">Oscar Westra van Holthe — Kind</a>
 */
@RestController
@RequestMapping("/rest")
public class PlannerService {
	/**
	 * Logger for this class.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PlannerService.class.getName());

	@GetMapping(produces = "text/plain")
	public String test() {
		return "REST service is available";
	}

	@PostMapping(produces = "application/pdf", consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<StreamingResponseBody> generatePlanner(
		@RequestBody final PlannerDescription plannerDescription) {
		LOGGER.debug("Entering generatePlanner({})", plannerDescription);

		final ResponseEntity<StreamingResponseBody> response = ResponseEntity.ok()
			.header("Content-Disposition", "attachment; filename=\"planner.pdf\"")
			.body(output -> {
				try {
					new PlannerGenerator(plannerDescription).generate(output);
				} catch (final Exception e) {
					throw new IOException("Failed to write planner", e);
				}
			});
		LOGGER.debug("Exiting generatePlanner: {}", response);
		return response;
	}
}
