package opwvhk.swing;

import org.intellij.lang.annotations.MagicConstant;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.swing.*;

import static java.awt.Desktop.Action.APP_ABOUT;
import static java.awt.Desktop.Action.OPEN;
import static java.awt.Taskbar.Feature.ICON_IMAGE;
import static java.util.Objects.requireNonNull;

public abstract class DesktopApp {
	private final String applicationName;
	protected final List<Image> applicationIcons;
	private final Desktop desktop;
	private final Taskbar taskbar;

	protected DesktopApp(String applicationName, String... iconResourceNames) {
		initializeAwtBeforeAnyAwtClassesAreInitialized(applicationName);
		desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		taskbar = Taskbar.isTaskbarSupported() ? Taskbar.getTaskbar() : null;
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		applicationIcons = loadIcons(toolkit::getImage, iconResourceNames);

		this.applicationName = applicationName;

		if (desktop != null && desktop.isSupported(APP_ABOUT) && !applicationIcons.isEmpty() &&
		    getAboutText() != null) {
			Icon icon = new ImageIcon(applicationIcons.getFirst());
			desktop.setAboutHandler(e -> JOptionPane.showMessageDialog(null,
					getAboutText(), null, JOptionPane.INFORMATION_MESSAGE, icon));
		}
		Comparator<Image> compareImagesBySize = Comparator.comparing(
				image -> Math.max(image.getWidth(null), image.getHeight(null)));
		// noinspection DataFlowIssue: the filter prevents an NPE
		applicationIcons.stream().max(compareImagesBySize)
				.filter(ignored -> taskbar != null && taskbar.isSupported(ICON_IMAGE))
				.ifPresent(taskbar::setIconImage);
	}

	protected String getAboutText() {
		return null;
	}

	protected <T> List<T> loadIcons(Function<URL, T> iconLoader, String... iconResourceNames) {
		return Stream.of(iconResourceNames)
				.map(icon -> requireNonNull(getClass().getResource(icon), () -> "Icon not found: " + icon))
				.map(iconLoader)
				.toList();
	}

	protected Font loadTrueTypeFont(String fontName,
	                                @MagicConstant(flags = {Font.PLAIN, Font.BOLD, Font.ITALIC}) int fontStyle,
	                                float fontSize) throws FontFormatException, IOException {
		try (InputStream fontStream = getClass().getResourceAsStream("/" + fontName + ".ttf")) {
			Font font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(fontStream, "Cannot find font"));
			return font.deriveFont(fontStyle, fontSize);
		}
	}

	protected void openFile(File file) throws IOException {
		if (desktop != null && desktop.isSupported(OPEN)) {
			desktop.open(file);
		}
	}

	protected JFrame createMainWindow() {
		return createWindow(applicationName, WindowCloseOperation.EXIT_APPLICATION);
	}

	protected JFrame createWindow(String title) {
		return createWindow(title, WindowCloseOperation.HIDE);
	}

	protected JFrame createWindow(String title, WindowCloseOperation windowCloseOperation) {
		JFrame frame = new JFrame(title);
		frame.setIconImages(applicationIcons);
		// noinspection MagicConstant
		frame.setDefaultCloseOperation(windowCloseOperation.magicConstant);

		return frame;
	}

	public enum WindowCloseOperation {
		IGNORE(WindowConstants.DO_NOTHING_ON_CLOSE), HIDE(WindowConstants.HIDE_ON_CLOSE),
		DISPOSE(WindowConstants.DISPOSE_ON_CLOSE), EXIT_APPLICATION(WindowConstants.EXIT_ON_CLOSE);

		private final int magicConstant;

		WindowCloseOperation(int magicConstant) {
			this.magicConstant = magicConstant;
		}
	}

	private static void initializeAwtBeforeAnyAwtClassesAreInitialized(String applicationName) {
		if (System.getProperty("os.name").contains("Mac")) {
			// Set the application name
			System.setProperty("apple.awt.application.name", applicationName);
			// Need for macos global menubar
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			// Use OSX Aqua look&feel
			System.setProperty("apple.awt.brushMetalLook", "true");
			// Use system appearance
			System.setProperty("apple.awt.application.appearance", "system");
		}
		// Use the system look and feel
		try {
			String systemLookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(systemLookAndFeelClassName);
		} catch (Exception e) {
			throw new IllegalArgumentException("The provided code was not safe to execute.", e);
		}
	}
}
