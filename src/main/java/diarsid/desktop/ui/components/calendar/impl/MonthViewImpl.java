package diarsid.desktop.ui.components.calendar.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import diarsid.desktop.ui.components.calendar.api.Calendar;
import diarsid.desktop.ui.components.calendar.api.TooltipsByDate;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import diarsid.support.javafx.Labeled;
import diarsid.support.javafx.pseudoclasses.PseudoClassAppliedTo;
import diarsid.support.javafx.pseudoclasses.PseudoClassesBoundTo;
import diarsid.support.objects.references.Possible;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static javafx.geometry.Pos.CENTER;

import static diarsid.support.javafx.PropertiesUtil.bindMapping;
import static diarsid.support.objects.references.References.simplePossibleButEmpty;

public class MonthViewImpl implements Calendar.MonthView {

    public static final Map<DayOfWeek, String> SHORT_NAMES_BY_DAYS;
    public static final PseudoClass PREVIOUS_MONTH = PseudoClass.getPseudoClass("prev-month");
    public static final PseudoClass CURRENT_MONTH = PseudoClass.getPseudoClass("current-month");
    public static final PseudoClass NOT_CURRENT_MONTH = PseudoClass.getPseudoClass("not-current-month");
    public static final PseudoClass NEXT_MONTH = PseudoClass.getPseudoClass("next-month");
    public static final PseudoClass TODAY = PseudoClass.getPseudoClass("today");

    static {
        SHORT_NAMES_BY_DAYS = new HashMap<>();
        SHORT_NAMES_BY_DAYS.put(MONDAY, "Mo");
        SHORT_NAMES_BY_DAYS.put(TUESDAY, "Tu");
        SHORT_NAMES_BY_DAYS.put(WEDNESDAY, "We");
        SHORT_NAMES_BY_DAYS.put(THURSDAY, "Th");
        SHORT_NAMES_BY_DAYS.put(FRIDAY, "Fr");
        SHORT_NAMES_BY_DAYS.put(SATURDAY, "Sa");
        SHORT_NAMES_BY_DAYS.put(SUNDAY, "Su");
    }

    public static class Day extends Labeled<LocalDate> {

        private static final Function<LocalDate, String> DATE_TO_DAY = date -> String.valueOf(date.getDayOfMonth());

        private Function<LocalDate, String> defaultTooltipText;
        private boolean isToday;

        public Day(LocalDate date, Function<LocalDate, String> defaultTooltipText) {
            super(date, DATE_TO_DAY);
            this.defaultTooltipText = defaultTooltipText;

            super.setTooltip(new Tooltip(this.defaultTooltipText.apply(date)));

            super.property().addListener((prop, oldValue, newValue) -> {
                super.getTooltip().setText(this.defaultTooltipText.apply(newValue));
            });
        }

        public Day(YearMonth yearMonth, int day, Function<LocalDate, String> defaultTooltipText) {
            super(LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), day), DATE_TO_DAY);
            this.defaultTooltipText = defaultTooltipText;

            super.setTooltip(new Tooltip(this.defaultTooltipText.apply(this.date())));

            super.property().addListener((prop, oldValue, newValue) -> {
                super.getTooltip().setText(this.defaultTooltipText.apply(newValue));
            });
        }

        public LocalDate date() {
            return super.property().get();
        }

        public void dateTo(YearMonth yearMonth, int day) {
            super.property().set(LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), day));
        }

        private void nextMonthStyle() {
            super.pseudoClassStateChanged(PREVIOUS_MONTH, false);
            super.pseudoClassStateChanged(CURRENT_MONTH, false);
            super.pseudoClassStateChanged(NOT_CURRENT_MONTH, true);
            super.pseudoClassStateChanged(NEXT_MONTH, true);
        }

        private void currentMonthStyle() {
            super.pseudoClassStateChanged(PREVIOUS_MONTH, false);
            super.pseudoClassStateChanged(CURRENT_MONTH, true);
            super.pseudoClassStateChanged(NOT_CURRENT_MONTH, false);
            super.pseudoClassStateChanged(NEXT_MONTH, false);
        }

        private void prevMonthStyle() {
            super.pseudoClassStateChanged(PREVIOUS_MONTH, true);
            super.pseudoClassStateChanged(CURRENT_MONTH, false);
            super.pseudoClassStateChanged(NOT_CURRENT_MONTH, true);
            super.pseudoClassStateChanged(NEXT_MONTH, false);
        }

        private void setIsToday(LocalDate today) {
            this.isToday = today.equals(super.property().get());
            super.pseudoClassStateChanged(TODAY, this.isToday);
        }

        public boolean isToday() {
            return this.isToday;
        }
    }

    public final VBox view;
    private final GridPane grid;
    private final Map<DayOfWeek, Integer> positionByDays;
    private final Day[][] days;
    private final Map<LocalDate, Day> daysByDates;
    private final Possible<Day> currentToday;
    private final Label monthYearLabel;
    private final List<PseudoClassAppliedTo<Day>> appliedPseudoClasses;
    private final PseudoClassesBoundTo<LocalDate> pseudoClassesByDates;
    private final TooltipsByDateImpl tooltipsByDate;
    private final Function<LocalDate, String> defaultTooltipText;
    private final MidnightTimer midnightTimer;

    private final Calendar.State.Control calendarStateControl;
    private YearMonth currMonth;
    private YearMonth prevMonth;
    private YearMonth nextMonth;

    public MonthViewImpl(
            Calendar.State.Control calendarStateControl,
            DayOfWeek firstDayOfWeek,
            PseudoClassesBoundTo<LocalDate> pseudoClassesByDates,
            Function<LocalDate, String> defaultTooltipText) {
        this.calendarStateControl = calendarStateControl;
        this.currMonth = calendarStateControl.yearMonth();
        this.prevMonth = this.currMonth.minusMonths(1);
        this.nextMonth = this.currMonth.plusMonths(1);

        this.calendarStateControl.property().addListener((p, oldV, newV) -> {
            this.currMonth = YearMonth.of(newV.getYear(), newV.getMonth());
            this.prevMonth = this.currMonth.minusMonths(1);
            this.nextMonth = this.currMonth.plusMonths(1);
            this.fill();
        });

        this.view = new VBox();
        final ReadOnlyDoubleProperty spacing = this.view.spacingProperty();
        this.grid = new GridPane();

        double x = 100.0 / 7;
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(x);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(x);
        ColumnConstraints column3 = new ColumnConstraints();
        column3.setPercentWidth(x);
        ColumnConstraints column4 = new ColumnConstraints();
        column4.setPercentWidth(x);
        ColumnConstraints column5 = new ColumnConstraints();
        column5.setPercentWidth(x);
        ColumnConstraints column6 = new ColumnConstraints();
        column6.setPercentWidth(x);
        ColumnConstraints column7 = new ColumnConstraints();
        column7.setPercentWidth(x);

        this.grid.getColumnConstraints().addAll(column1, column2, column3, column4, column5, column6, column7);

        this.grid.vgapProperty().bind(spacing);
        this.grid.hgapProperty().bind(spacing);

        this.days = new Day[6][7];
        this.daysByDates = new HashMap<>();
        this.currentToday = simplePossibleButEmpty();
        this.pseudoClassesByDates = pseudoClassesByDates;
        this.tooltipsByDate = new TooltipsByDateImpl(new HashMap<>(), new HashMap<>());
        this.defaultTooltipText = defaultTooltipText;
        this.appliedPseudoClasses = new ArrayList<>();

        Map<Integer, DayOfWeek> daysByPosition = new HashMap<>();
        this.positionByDays = new HashMap<>();

        ReadOnlyDoubleProperty viewWidth = this.view.minWidthProperty();
        final DoubleProperty gridCellWidth = new SimpleDoubleProperty();
        final DoubleProperty gridCellHeight = new SimpleDoubleProperty();
        final ObjectProperty<Font> font = new SimpleObjectProperty<>();

        this.view.getStyleClass().add("month-view");

        LocalDate firstDay = firstDayOf(this.currMonth);
        DayOfWeek firstDayName = firstDay.getDayOfWeek();
        DayOfWeek currentDay = firstDayOfWeek;

        for (int d = 1; d <= 7 ; d++) {
            daysByPosition.put(d, currentDay);
            positionByDays.put(currentDay, d);
            currentDay = currentDay.plus(1);
        }

        int firstDayIndexInFirstWeek = positionByDays.get(firstDayName);

        int prevMonthDaysRemnant = firstDayIndexInFirstWeek - 1;
        int prevMonthDaysIndex = firstDayOf(prevMonth).lengthOfMonth() - prevMonthDaysRemnant + 1;
        int currMonthDaysIndex = 1;
        int nextMonthDaysIndex = 1;
        int maxDays = firstDay.lengthOfMonth();
        LocalDate today = LocalDate.now();

        BlinkingDetector blinkingDetector = new BlinkingDetector(() -> System.out.println("blinking detected"));

        final int firstWeekRow = 1;
        for (int gridRow = 0; gridRow <= 6; gridRow++) {
            if ( gridRow == 0 ) {
                for (int gridCol = 0; gridCol <= 6 ; gridCol++) {
                    Label dayName = new Label();
                    dayName.setText(SHORT_NAMES_BY_DAYS.get(currentDay));

                    dayName.fontProperty().bind(font);
                    dayName.setMaxWidth(Double.MAX_VALUE);

                    dayName.setAlignment(CENTER);
                    dayName.minHeightProperty().bind(gridCellHeight);
                    dayName.maxHeightProperty().bind(gridCellHeight);

                    GridPane.setFillWidth(dayName, true);

                    dayName.getStyleClass().addAll("day-name", currentDay.name().toLowerCase());

                    this.grid.add(dayName, gridCol, gridRow);

                    currentDay = currentDay.plus(1);
                }
                continue;
            }

            Day day;
            for (int gridCol = 0; gridCol <= 6; gridCol++) {
                if ( gridRow == firstWeekRow && prevMonthDaysRemnant > 0 ) {
                    day = new Day(this.prevMonth, prevMonthDaysIndex, this.defaultTooltipText);
                    day.prevMonthStyle();
                    day.setIsToday(today);

                    prevMonthDaysRemnant--;
                    prevMonthDaysIndex++;
                }
                else {
                    if ( currMonthDaysIndex <= maxDays ) {
                        day = new Day(this.currMonth, currMonthDaysIndex, this.defaultTooltipText);
                        day.currentMonthStyle();
                        day.setIsToday(today);

                        currMonthDaysIndex++;
                    }
                    else {
                        day = new Day(nextMonth, nextMonthDaysIndex, this.defaultTooltipText);
                        day.nextMonthStyle();
                        day.setIsToday(today);

                        nextMonthDaysIndex++;
                    }
                }

                if ( ! gridCellWidth.isBound() ) {
                    font.bind(day.fontProperty());
                    gridCellWidth.bind(day.widthProperty());

                    day.heightProperty().addListener((p, o, n) -> {
                        blinkingDetector.onChange((double) n);
                    });

                    gridCellHeight.bind(day.heightProperty());

                    gridCellWidth.addListener((p, o, n) -> {
                    });
                    gridCellHeight.addListener((p, o, n) -> {
                    });
                }

                if ( day.isToday ) {
                    this.currentToday.resetTo(day);
                }

                daysByDates.put(day.date(), day);
                days[gridRow-firstWeekRow][gridCol] = day;

                day.getStyleClass().add("day");
                day.setAlignment(CENTER);

                day.setMaxWidth(Double.MAX_VALUE);
                GridPane.setFillWidth(day, true);

                this.grid.add(day, gridCol, gridRow);
            }
        }

        this.applyGivenPseudoClasses();
        this.bindTooltipsToDates();


        BorderPane dateInfo = new BorderPane();
        Label prevButton = new Label();
        Label nextButton = new Label();

        prevButton.minWidthProperty().bind(gridCellWidth);
        prevButton.maxWidthProperty().bind(gridCellWidth);
        prevButton.minHeightProperty().bind(gridCellHeight);
        prevButton.maxHeightProperty().bind(gridCellHeight);

        prevButton.setOnMousePressed(event -> this.calendarStateControl.toPrevMonth());
        prevButton.setText("<");
        prevButton.fontProperty().bind(font);
        prevButton.setAlignment(CENTER);
        prevButton.getStyleClass().add("prev-month");

        nextButton.minWidthProperty().bind(gridCellWidth);
        nextButton.maxWidthProperty().bind(gridCellWidth);
        nextButton.minHeightProperty().bind(gridCellHeight);
        nextButton.maxHeightProperty().bind(gridCellHeight);

        nextButton.setOnMousePressed(event -> this.calendarStateControl.toNextMonth());
        nextButton.setText(">");
        nextButton.fontProperty().bind(font);
        nextButton.setAlignment(CENTER);
        nextButton.getStyleClass().add("next-month");

        monthYearLabel = new Label();
        monthYearLabel.fontProperty().bind(font);
        monthYearLabel.setText(this.currMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + this.currMonth.getYear());
        monthYearLabel.getStyleClass().add("current-month");

        dateInfo.setLeft(prevButton);
        dateInfo.setRight(nextButton);
        dateInfo.setCenter(monthYearLabel);
        dateInfo.getStyleClass().add("months-menu");


        dateInfo.minHeightProperty().bind(gridCellHeight);
        dateInfo.maxHeightProperty().bind(gridCellHeight);

        view.getChildren().addAll(dateInfo, grid);

        String timerThreadName = this.getClass().getCanonicalName() + "." + MidnightTimer.class.getSimpleName();
        this.midnightTimer = new MidnightTimer(
                timerThreadName,
                this::fillInJavaFXThread);
    }

    private void fillInJavaFXThread() {
        if ( Platform.isFxApplicationThread() ) {
            this.fill();
        }
        else {
            Platform.runLater(this::fill);
        }
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

    private void bindTooltipsToDates() {
        this.daysByDates.forEach((key, value) -> this.tooltipsByDate.bind(key, value.getTooltip()));
    }

    private static LocalDate firstDayOf(YearMonth yearMonth) {
        return LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), 1);
    }

    private void fill() {
        daysByDates.clear();
        tooltipsByDate.unbindAll();
        this.revertGivenPseudoClasses();

        LocalDate firstDay = firstDayOf(currMonth);
        DayOfWeek firstDayName = firstDay.getDayOfWeek();

        int firstDayIndexInFirstWeek = positionByDays.get(firstDayName);

        int prevMonthDaysRemnant = firstDayIndexInFirstWeek - 1;
        int prevMonthDaysIndex = firstDayOf(prevMonth).lengthOfMonth() - prevMonthDaysRemnant + 1;
        int daysIndex = 1;
        int nextMonthDaysIndex = 1;
        int maxDays = firstDay.lengthOfMonth();
        LocalDate today = LocalDate.now();

        Day day;
        for (int w = 1; w <= 6; w++) {
            for (int d = 1; d <= 7; d++) {
                day = days[w-1][d-1];

                if ( w == 1 && prevMonthDaysRemnant > 0 ) {
                    day.dateTo(prevMonth, prevMonthDaysIndex);
                    day.prevMonthStyle();
                    day.setIsToday(today);

                    prevMonthDaysRemnant--;
                    prevMonthDaysIndex++;
                }
                else {
                    if ( daysIndex <= maxDays ) {
                        day.dateTo(currMonth, daysIndex);
                        day.currentMonthStyle();
                        day.setIsToday(today);

                        daysIndex++;
                    }
                    else {
                        day.dateTo(nextMonth, nextMonthDaysIndex);
                        day.nextMonthStyle();
                        day.setIsToday(today);

                        nextMonthDaysIndex++;
                    }
                }

                if ( day.isToday ) {
                    this.currentToday.resetTo(day);
                }

                daysByDates.put(day.date(), day);
            }
        }

        this.applyGivenPseudoClasses();
        this.bindTooltipsToDates();

        this.monthYearLabel.setText(currMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currMonth.getYear());
    }

    private void revertGivenPseudoClasses() {
        appliedPseudoClasses.forEach(PseudoClassAppliedTo::revertNodePseudoClass);
        appliedPseudoClasses.clear();
    }

    private void applyGivenPseudoClasses() {
        pseudoClassesByDates.forEach((date, pseudoClasses) -> {
            Day dayByDate = daysByDates.get(date);
            if ( dayByDate != null ) {
                pseudoClasses.forEach((pseudoClass, active) -> {
                    dayByDate.pseudoClassStateChanged(pseudoClass, active);
                    appliedPseudoClasses.add(new PseudoClassAppliedTo<>(pseudoClass, active, dayByDate));
                });
            }
        });
    }

}
