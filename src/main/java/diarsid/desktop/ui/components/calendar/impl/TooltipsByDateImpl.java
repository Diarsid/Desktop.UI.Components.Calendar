package diarsid.desktop.ui.components.calendar.impl;

import java.time.LocalDate;
import java.util.Map;

import diarsid.desktop.ui.components.calendar.api.TooltipsByDate;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Tooltip;

class TooltipsByDateImpl implements TooltipsByDate {

    private final Map<LocalDate, StringProperty> textsByDate;
    private final Map<LocalDate, Tooltip> tooltipsByDate;

    public TooltipsByDateImpl(Map<LocalDate, StringProperty> textsByDate, Map<LocalDate, Tooltip> tooltipsByDate) {
        this.textsByDate = textsByDate;
        this.tooltipsByDate = tooltipsByDate;
    }

    @Override
    public void add(LocalDate date, String newText) {
        StringProperty text = textsByDate.get(date);

        if ( text == null ) {
            text = new SimpleStringProperty();
        }

        text.set(newText);

        Tooltip tooltip = tooltipsByDate.get(date);

        if ( tooltip == null ) {
            return;
        }

        tooltip.textProperty().bind(text);
    }

    void bind(LocalDate date, Tooltip tooltip) {
        tooltipsByDate.put(date, tooltip);

        StringProperty text = textsByDate.get(date);

        if ( text == null ) {
            return;
        }

        tooltip.textProperty().bind(text);
    }

    void unbindAll() {
        this.tooltipsByDate.values().forEach(tooltip -> tooltip.textProperty().unbind());
    }
}
