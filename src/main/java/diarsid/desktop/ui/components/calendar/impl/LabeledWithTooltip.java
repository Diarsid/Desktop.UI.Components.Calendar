package diarsid.desktop.ui.components.calendar.impl;

import java.util.function.Function;

import javafx.scene.control.Tooltip;

import diarsid.support.javafx.components.Labeled;

public class LabeledWithTooltip<T> extends Labeled<T> {

    protected final Function<T, String> toTooltipString;

    public LabeledWithTooltip(
            T t,
            Function<T, String> toLabelString,
            Function<T, String> toTooltipString) {
        super(t, toLabelString);
        this.toTooltipString = toTooltipString;

        Tooltip tooltip = new Tooltip();
        super.setTooltip(tooltip);
        this.fillTooltipText(super.property().get());

        super.property().addListener((prop, oldV, newV) -> {
            this.fillTooltipText(newV);
        });
    }

    private void fillTooltipText(T t) {
        String text = this.toTooltipString.apply(t);
        super.getTooltip().setText(text);
    }

    void refresh() {
        this.fillTooltipText(super.property().get());
    }
}
