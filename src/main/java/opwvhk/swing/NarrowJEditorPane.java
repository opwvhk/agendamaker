package opwvhk.swing;

import java.awt.*;
import javax.swing.*;

/// Variant of JEditorPane that reports its minimum size as preferred size. Useful if the default preferred size takes
/// too much space.
public class NarrowJEditorPane extends JEditorPane {
	@Override
	public Dimension getPreferredSize() {
		if (!isPreferredSizeSet()) {
			return getMinimumSize();
		}
		return super.getPreferredSize();
	}
}
