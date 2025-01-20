package opwvhk.swing;

import java.awt.*;
import javax.swing.*;

import static java.util.Objects.requireNonNull;

public class FormBuilder {
	private final JPanel panel;
	private final GridBagConstraints defaultComponentConstraints;
	private final GridBagConstraints labelConstraints;
	private final int rowGap;
	private final int columnGap;
	private final int componentGap;
	private int columns;

	public FormBuilder() {
		this(null, null, -1, -1, -1);
	}

	public FormBuilder(GridBagConstraints labelConstraints,
	                   GridBagConstraints defaultComponentConstraints,
	                   int rowGap, int columnGap, int componentGap) {
		panel = new JPanel(new GridBagLayout());

		if (defaultComponentConstraints == null) {
			defaultComponentConstraints = new GridBagConstraints();
			defaultComponentConstraints.fill = GridBagConstraints.BOTH;
			defaultComponentConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
		}
		this.defaultComponentConstraints = defaultComponentConstraints;

		if (labelConstraints == null) {
			labelConstraints = new GridBagConstraints();
			labelConstraints.fill = GridBagConstraints.BOTH;
			labelConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
		}
		this.labelConstraints = labelConstraints;

		Font labelFont = UIManager.getFont("Label.font");
		int defaultGap = Math.round(labelFont.getSize2D() / 2.0f + 0.5f); // Round up
		int defaultLabelGap = defaultGap / 2;
		this.rowGap = Math.max(rowGap, 0);
		this.columnGap = columnGap < 0 ? defaultGap : columnGap;
		this.componentGap = componentGap < 0 ? defaultLabelGap : componentGap;

		columns = 0;
	}

	public JPanel build() {
		return panel;
	}

	/**
	 * Add a component at the specified grid location using default constraints.
	 *
	 * @param column    the grid column to place the component at
	 * @param row       the grid row the grid row to place the component at
	 * @param width     the width (column span) of the component
	 * @param component the component to place
	 * @return this builder
	 */
	public FormBuilder add(int column, int row, int width, JComponent component) {
		return addInternal(true, 2 * column, row, 2 * width, component, defaultComponentConstraints);
	}

	private FormBuilder addInternal(boolean useComponentGap, int gridColumn, int row, int gridWidth,
	                                JComponent component,
	                                GridBagConstraints constraints) {
		GridBagConstraints c = (GridBagConstraints) constraints.clone();
		c.gridx = gridColumn;
		c.gridy = row;
		c.gridwidth = gridWidth;
		c.gridheight = 1;
		c.insets.left += gridColumn == 0 ? 0 : useComponentGap ? columnGap : componentGap;
		c.insets.top += row == 0 ? 0 : rowGap;
		panel.add(requireNonNull(component), c);

		columns = Math.max(columns, gridColumn + gridWidth);

		return this;
	}

	public FormBuilder add(int column, int row, JLabel label, JComponent component) {
		return add(column, row, 1, label, component, defaultComponentConstraints);
	}

	public FormBuilder add(int column, int row, int width, JLabel label, JComponent component) {
		return add(column, row, width, label, component, defaultComponentConstraints);
	}

	public FormBuilder add(int column, int row, JLabel label, JComponent component,
	                       GridBagConstraints componentConstraints) {
		return add(column, row, 1, label, component, componentConstraints);
	}

	public FormBuilder add(int column, int row, int width, JLabel label, JComponent component,
	                       GridBagConstraints componentConstraints) {
		if (label != null) {
			addInternal(true, 2 * column, row, 1, label, labelConstraints);
		}
		return addInternal(false, 2 * column + 1, row, 2 * width - 1, component, componentConstraints);
	}
}
