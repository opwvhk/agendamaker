package opwvhk.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Unknown why, but Sprint Boot 2.5.0-M1 and 2.5.0-M3 fail to handle the welcome page properly. This is a manual fix.
 */
@Controller
@RequestMapping("/")
public class WelcomePageForwarder {

	@GetMapping
	public String get() {
		return "forward:index.html";
	}
}
