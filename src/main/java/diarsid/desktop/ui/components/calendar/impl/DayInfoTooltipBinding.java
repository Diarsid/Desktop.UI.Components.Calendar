package diarsid.desktop.ui.components.calendar.impl;

import java.time.LocalDate;
import java.util.Map;

import diarsid.desktop.ui.components.calendar.api.Day;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Tooltip;

public class DayInfoTooltipBinding implements Day.Info.Control {

    private final Map<LocalDate, StringProperty> textsByDate;
    private final Map<LocalDate, Tooltip> tooltipsByDate;
    private final Day.Info.ToString dayInfoToString;

    public DayInfoTooltipBinding(
            Map<LocalDate, StringProperty> textsByDate,
            Map<LocalDate, Tooltip> tooltipsByDate,
            Day.Info.ToString dayInfoToString) {
        this.textsByDate = textsByDate;
        this.tooltipsByDate = tooltipsByDate;
        this.dayInfoToString = dayInfoToString;
    }

    @Override
    public void set(LocalDate date, String newText) {
        StringProperty text = textsByDate.get(date);

        if ( text == null ) {
            text = new SimpleStringProperty();
            textsByDate.put(date, text);
        }

        text.set(newText);

        Tooltip tooltip = tooltipsByDate.get(date);

        if ( tooltip == null ) {
            return;
        }

        tooltip.textProperty().bind(text);
    }

    @Override
    public void set(Day.Info dayInfo) {
        this.set(dayInfo.date(), this.dayInfoToString.apply(dayInfo));
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
        this.tooltipsByDate
                .values()
                .forEach(tooltip -> tooltip.textProperty().unbind());
    }
}
