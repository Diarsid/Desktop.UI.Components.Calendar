package diarsid.desktop.ui.components.calendar.impl;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import diarsid.desktop.ui.components.calendar.api.Calendar;
import diarsid.desktop.ui.components.calendar.api.TooltipsByDate;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import diarsid.support.javafx.pseudoclasses.PseudoClassesBoundTo;

public class YearViewImpl implements Calendar.YearView {

    public static final PseudoClass IN_FUTURE = PseudoClass.getPseudoClass("in-future");
    public static final PseudoClass IN_PAST = PseudoClass.getPseudoClass("in-past");
    public static final PseudoClass TODAY = PseudoClass.getPseudoClass("today");
    public static final PseudoClass MONTH_FOCUSED = PseudoClass.getPseudoClass("month-focused");

    public static class Day extends HBox {

        private final YearViewImpl view;
        private final Label label;
        private final ObjectProperty<LocalDate> date;

        public Day(LocalDate date, YearViewImpl view) {
            this.date = new SimpleObjectProperty<>(date);
            this.view = view;
            this.label = new Label();

            super.getChildren().add(this.label);
            super.getStyleClass().add("day-in-month");

            this.label.getStyleClass().add("day");

            DoubleProperty width = this.label.minWidthProperty();
            this.label.maxWidthProperty().bind(width);
            this.label.minHeightProperty().bind(width);
            this.label.maxHeightProperty().bind(width);

            this.hoverProperty().addListener((p, oldValue, newValue) -> {
                boolean hover = newValue || this.label.hoverProperty().get();
                this.hoverChanged(hover);
            });

            this.label.hoverProperty().addListener((p, oldValue, newValue) -> {
                boolean hover = newValue || super.hoverProperty().get();
                this.hoverChanged(hover);
            });

            Tooltip tooltip = new Tooltip();
            tooltip.setText(this.view.defaultTooltipText.apply(this.date.get()));
            this.label.setTooltip(tooltip);

            this.date.addListener((p, oldValue, newValue) -> {
                tooltip.setText(this.view.defaultTooltipText.apply(newValue));
            });
        }

        public void dateTo(LocalDate date) {
            this.date.set(date);
        }

        public void todayStyle() {
            this.label.pseudoClassStateChanged(TODAY, true);
            this.label.pseudoClassStateChanged(IN_PAST, false);
            this.label.pseudoClassStateChanged(IN_FUTURE, false);
        }

        public void pastStyle() {
            this.label.pseudoClassStateChanged(TODAY, false);
            this.label.pseudoClassStateChanged(IN_PAST, true);
            this.label.pseudoClassStateChanged(IN_FUTURE, false);
        }

        public void futureStyle() {
            this.label.pseudoClassStateChanged(TODAY, false);
            this.label.pseudoClassStateChanged(IN_PAST, false);
            this.label.pseudoClassStateChanged(IN_FUTURE, true);
        }

        private void hoverChanged(boolean value) {
            this.view.daysByMonths.get(this.date.get().getMonth()).forEach(day -> day.monthFocused(value));
        }

        private void monthFocused(boolean value) {
            super.pseudoClassStateChanged(MONTH_FOCUSED, value);
        }
    }

    private final Calendar.State.Control calendarStateControl;
    private final PseudoClassesBoundTo<LocalDate> pseudoClassesByDates;
    private final FlowPane view;
    private final Map<LocalDate, Day> daysByDates;
    private final Map<Month, List<Day>> daysByMonths;
    private final Map<Integer, Day> daysByYearIndex;
    private final TooltipsByDateImpl tooltipsByDate;
    private final Function<LocalDate, String> defaultTooltipText;
    private final MidnightTimer midnightTimer;

    public YearViewImpl(
            Calendar.State.Control calendarState,
            PseudoClassesBoundTo<LocalDate> pseudoClassesByDates,
            Function<LocalDate, String> defaultTooltipText) {
        this.calendarStateControl = calendarState;
        this.pseudoClassesByDates = pseudoClassesByDates;
        this.defaultTooltipText = defaultTooltipText;

        this.view = new FlowPane();
        this.daysByDates = new HashMap<>();
        this.daysByMonths = new HashMap<>();
        this.daysByYearIndex = new HashMap<>();
        this.tooltipsByDate = new TooltipsByDateImpl(new HashMap<>(), new HashMap<>());

        int year = calendarState.year();
        List<Node> viewChildren = this.view.getChildren();

        for ( Month month : Month.values() ) {
            this.daysByMonths.put(month, new ArrayList<>());
        }

        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth;
        LocalDate dayDate;
        Day day;
        int daysInMonth;
        int dayInYear = 1;
        for ( Month month : Month.values() ) {
            firstDayOfMonth = LocalDate.of(year, month, 1);
            daysInMonth = firstDayOfMonth.lengthOfMonth();

            for ( int dayInMonth = 1; dayInMonth <= daysInMonth; dayInMonth++) {
                dayDate = LocalDate.of(year, month, dayInMonth);
                day = new Day(dayDate, this);

                viewChildren.add(day);

                this.daysByDates.put(dayDate, day);
                this.daysByMonths.get(month).add(day);
                this.daysByYearIndex.put(dayInYear, day);

                if ( today.isEqual(dayDate) ) {
                    day.todayStyle();
                }
                else if ( today.isBefore(dayDate) ) {
                    day.futureStyle();
                }
                else {
                    day.pastStyle();
                }

                dayInYear++;
            }
        }

        this.view.getStyleClass().add("year-view");

        this.calendarStateControl.property().addListener((p, oldValue, newValue) -> {
            int oldYear = oldValue.getYear();
            int newYear = newValue.getYear();

            if ( oldYear != newYear ) {
                this.fill();
                System.out.println("change year");
            }
        });

        this.bindTooltipsToDates();

        String timerThreadName = this.getClass().getCanonicalName() + "." + MidnightTimer.class.getSimpleName();
        this.midnightTimer = new MidnightTimer(
                timerThreadName,
                this::fillInJavaFXThread);
    }

    private void bindTooltipsToDates() {
        this.daysByDates.forEach((key, value) -> this.tooltipsByDate.bind(key, value.label.getTooltip()));
    }

    @Override
    public Calendar.State.Control control() {
        return this.calendarStateControl;
    }

    @Override
    public Region node() {
        return this.view;
    }

    @Override
    public TooltipsByDate tooltipsByDate() {
        return this.tooltipsByDate;
    }

    private void fillInJavaFXThread() {
        if ( Platform.isFxApplicationThread() ) {
            this.fill();
        }
        else {
            Platform.runLater(this::fill);
        }
    }

    private void fill() {
        System.out.println("Year::fill");
        this.daysByDates.clear();
        this.daysByMonths.forEach((month, days) -> days.clear());
        this.tooltipsByDate.unbindAll();
        List<Node> viewChildren = this.view.getChildren();
        viewChildren.clear();
        LocalDate today = LocalDate.now();

        this.view.maxWidthProperty();

        int year = calendarStateControl.year();

        LocalDate firstDayOfMonth;
        LocalDate dayDate;
        Day day;
        int daysInMonth;
        int dayInYear = 1;
        for ( Month month : Month.values() ) {
            firstDayOfMonth = LocalDate.of(year, month, 1);
            daysInMonth = firstDayOfMonth.lengthOfMonth();

            for ( int dayInMonth = 1; dayInMonth <= daysInMonth; dayInMonth++) {
                dayDate = LocalDate.of(year, month, dayInMonth);
                day = this.daysByYearIndex.get(dayInYear);

                if ( day == null ) {
                    day = new Day(dayDate, this);
                    this.daysByYearIndex.put(dayInYear, day);
                }

                if ( today.isEqual(dayDate) ) {
                    day.todayStyle();
                }
                else if ( today.isBefore(dayDate) ) {
                    day.futureStyle();
                }
                else {
                    day.pastStyle();
                }

                day.dateTo(dayDate);
                this.daysByDates.put(dayDate, day);
                this.daysByMonths.get(month).add(day);
                viewChildren.add(day);

                dayInYear++;
            }
        }

        this.bindTooltipsToDates();
        System.out.println("Year::filled");
    }
}
