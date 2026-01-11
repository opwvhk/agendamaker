package opwvhk;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class TextBundle {
	private final ResourceBundle resourceBundle;

	public TextBundle(String baseName, Locale locale) {
		this(ResourceBundle.getBundle(baseName, locale));
	}

	public TextBundle(ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
	}

	public String message(String key) {
		return resourceBundle.getString(key);
	}

	public String[] messageArray(String key) {
		if (resourceBundle.containsKey(key)) {
			String formattedList = resourceBundle.getString(key);
			return formattedList.split(",");
		} else {
			List<String> items = new ArrayList<>();
			int index = 0;
			String prefix = key + ".";
			while (resourceBundle.containsKey(prefix + index)) {
				items.add(resourceBundle.getString(prefix + index));
				index++;
			}
			return items.toArray(String[]::new);
		}
	}

	public String message(String key, Object... parameters) {
		String format = resourceBundle.getString(key);
		// We could cache the MessageFormat instances, but keys are not yet reused often.
		return new MessageFormat(format, resourceBundle.getLocale()).format(parameters);
	}
}
