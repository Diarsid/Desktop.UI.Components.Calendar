package diarsid.desktop.ui.components.calendar.impl;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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

import diarsid.desktop.ui.components.calendar.api.Calendar;
import diarsid.desktop.ui.components.calendar.api.Day;
import diarsid.support.javafx.css.pseudoclasses.PseudoClassesBoundTo;
import diarsid.support.javafx.mouse.ClickTypeDetector;

import static java.util.Objects.nonNull;

public class YearViewImpl implements Calendar.YearView, DayInfoState.ChangesListener {

    public static final PseudoClass IN_FUTURE = PseudoClass.getPseudoClass("in-future");
    public static final PseudoClass IN_PAST = PseudoClass.getPseudoClass("in-past");
    public static final PseudoClass TODAY = PseudoClass.getPseudoClass("today");
    public static final PseudoClass MONTH_FOCUSED = PseudoClass.getPseudoClass("month-focused");

    public static class DayInYear extends HBox {

        private final Label label;
        private final ObjectProperty<LocalDate> date;
        private final YearViewImpl view;
        private final ClickTypeDetector clickTypeDetector;

        public DayInYear(
                LocalDate date,
                YearViewImpl view) {
            this.label = new Label();
            this.date = new SimpleObjectProperty<>(date);
            this.view = view;

            super.getChildren().add(this.label);
            super.getStyleClass().add("day-in-month");

            this.label.getStyleClass().add("day");

            DoubleProperty width = this.label.minWidthProperty();
            this.label.maxWidthProperty().bind(width);
            this.label.minHeightProperty().bind(width);
            this.label.maxHeightProperty().bind(width);

            this.hoverProperty().addListener((p, oldValue, newValue) -> {
                boolean hover = newValue || super.hoverProperty().get();
                this.hoverChanged(hover);
            });

            super.hoverProperty().addListener((p, oldValue, newValue) -> {
                boolean hover = newValue || super.hoverProperty().get();
                this.hoverChanged(hover);
            });


            Tooltip tooltip = new Tooltip();
            this.label.setTooltip(tooltip);
            this.fillTooltipText(this.date.get());

            this.date.addListener((prop, oldV, newV) -> {
                this.fillTooltipText(newV);
            });

            this.clickTypeDetector = ClickTypeDetector.Builder.createFor(this)
                    .withDoOnAll((clickType, event) -> {
                        LocalDate currentDate = this.date.get();
                        this.view.mouseCallback.onClick(
                                clickType,
                                currentDate,
                                this.view.dayInfoState.findDayInfoOf(currentDate));
                    })
                    .build();
        }

        private void fillTooltipText(LocalDate date) {
            Optional<Day.Info> dayInfoLoaded = this.view.dayInfoState.findDayInfoOf(date);

            String text;
            if ( dayInfoLoaded.isPresent() ) {
                var dayInfo = dayInfoLoaded.get();
                if ( dayInfo.customToString().isPresent() ) {
                    text = dayInfo.customToString().get().apply(dayInfo);
                }
                else {
                    text = this.view.dayInfoToString.apply(dayInfo);
                }
            }
            else {
                text = this.view.defaultTooltipText.apply(date);
            }

            this.label.getTooltip().setText(text);
        }

        void refresh() {
            this.fillTooltipText(this.date.get());
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
            Month month = this.date.get().getMonth();
            this.view
                    .daysByMonths
                    .get(month)
                    .forEach(day -> day.monthFocused(value));
        }

        private void monthFocused(boolean value) {
            super.pseudoClassStateChanged(MONTH_FOCUSED, value);
        }
    }

    private final Calendar.State.Control calendarStateControl;
    private final DayInfoState dayInfoState;
    private final Day.Info.ToString dayInfoToString;
    private final Day.MouseCallback mouseCallback;
    private final PseudoClassesBoundTo<LocalDate> pseudoClassesByDates;
    private final FlowPane view;
    private final ByDatesHolder<DayInYear> daysByDates;
    private final Map<Month, List<DayInYear>> daysByMonths;
    private final Map<Integer, DayInYear> daysByYearIndex;
    private final Function<LocalDate, String> defaultTooltipText;
    private final MidnightTimer midnightTimer;

    public YearViewImpl(
            Calendar.State.Control calendarState,
            DayInfoState dayInfoState,
            Day.Info.ToString dayInfoToString,
            Day.MouseCallback mouseCallback,
            PseudoClassesBoundTo<LocalDate> pseudoClassesByDates,
            Function<LocalDate, String> defaultTooltipText) {
        this.calendarStateControl = calendarState;
        this.dayInfoState = dayInfoState;
        this.dayInfoToString = dayInfoToString;
        this.mouseCallback = mouseCallback;
        this.pseudoClassesByDates = pseudoClassesByDates;
        this.defaultTooltipText = defaultTooltipText;
        dayInfoState.add(this);

        this.view = new FlowPane();
        this.daysByDates = new ByDatesHolder<>();
        this.daysByMonths = new HashMap<>();
        this.daysByYearIndex = new HashMap<>();

        int year = calendarState.year();

        this.dayInfoState.load(year);

        List<Node> viewChildren = this.view.getChildren();

        for ( Month month : Month.values() ) {
            this.daysByMonths.put(month, new ArrayList<>());
        }

        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth;
        LocalDate dayDate;
        DayInYear day;
        int daysInMonth;
        int dayInYear = 1;
        for ( Month month : Month.values() ) {
            firstDayOfMonth = LocalDate.of(year, month, 1);
            daysInMonth = firstDayOfMonth.lengthOfMonth();

            for ( int dayInMonth = 1; dayInMonth <= daysInMonth; dayInMonth++) {
                dayDate = LocalDate.of(year, month, dayInMonth);
                day = new DayInYear(dayDate, this);

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

        this.calendarStateControl.property().addListener((p, oldDate, newDate) -> {
            int oldYear = oldDate.getYear();
            int newYear = newDate.getYear();

            if ( oldYear != newYear ) {
                this.fill();
                System.out.println("change year");
            }
        });

        String timerThreadName = this.getClass().getCanonicalName() + "." + MidnightTimer.class.getSimpleName();
        this.midnightTimer = new MidnightTimer(
                timerThreadName,
                this::fillInJavaFXThread);
    }

    @Override
    public void onChange(LocalDate date) {
        DayInYear day = this.daysByDates.findOrNull(date);
        if ( nonNull(day) ) {
            day.refresh();
        }
    }

    @Override
    public void onChange(YearMonth month) {
        this.daysByDates
                .findBy(month)
                .forEach(DayInYear::refresh);
    }

    @Override
    public void onChange(Year year) {
        this.daysByDates
                .findBy(year)
                .forEach(DayInYear::refresh);
    }

    @Override
    public Region node() {
        return this.view;
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

        List<Node> viewChildren = this.view.getChildren();
        viewChildren.clear();
        LocalDate today = LocalDate.now();

        this.view.maxWidthProperty();

        int year = calendarStateControl.year();
        this.dayInfoState.load(year);

        LocalDate firstDayOfMonth;
        LocalDate dayDate;
        DayInYear day;
        int daysInMonth;
        int dayInYear = 1;
        for ( Month month : Month.values() ) {
            firstDayOfMonth = LocalDate.of(year, month, 1);
            daysInMonth = firstDayOfMonth.lengthOfMonth();

            for ( int dayInMonth = 1; dayInMonth <= daysInMonth; dayInMonth++) {
                dayDate = LocalDate.of(year, month, dayInMonth);
                day = this.daysByYearIndex.get(dayInYear);

                if ( day == null ) {
                    day = new DayInYear(dayDate, this);
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

                day.date.set(dayDate);
                this.daysByDates.put(dayDate, day);
                this.daysByMonths.get(month).add(day);
                viewChildren.add(day);

                dayInYear++;
            }
        }

        System.out.println("Year::filled");
    }
}
