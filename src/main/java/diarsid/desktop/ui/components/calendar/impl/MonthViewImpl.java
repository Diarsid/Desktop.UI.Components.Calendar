package diarsid.desktop.ui.components.calendar.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import diarsid.desktop.ui.components.calendar.api.Calendar;
import diarsid.desktop.ui.components.calendar.api.Dates;
import diarsid.desktop.ui.components.calendar.api.Day;
import diarsid.support.javafx.css.pseudoclasses.PseudoClassAppliedTo;
import diarsid.support.javafx.css.pseudoclasses.PseudoClassesBoundTo;
import diarsid.support.javafx.mouse.ClickTypeDetector;
import diarsid.support.objects.references.Possible;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.util.Objects.nonNull;

import static javafx.geometry.Pos.CENTER;
import static javafx.scene.layout.Priority.ALWAYS;

import static diarsid.support.objects.references.References.simplePossibleButEmpty;

public class MonthViewImpl implements Calendar.MonthView, DayInfoState.ChangesListener {

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

    private static class DayInMonth extends LabelTooltipDay {

        private static final Function<LocalDate, String> DATE_TO_LABEL_STRING = date -> String.valueOf(date.getDayOfMonth());

        private final ClickTypeDetector clickTypeDetector;
        private final Day.MouseCallback clickCallback;
        private boolean isToday;

        private DayInMonth(
                YearMonth yearMonth,
                int day,
                Function<LocalDate, String> defaultTooltipText,
                Function<LocalDate, Optional<Day.Info>> dayInfoByDate,
                Day.Info.ToString dayInfoToString,
                Day.MouseCallback callback
                ) {
            super(
                    LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), day),
                    DATE_TO_LABEL_STRING,
                    dayInfoByDate,
                    defaultTooltipText,
                    dayInfoToString);
            this.clickCallback = callback;

            this.clickTypeDetector = ClickTypeDetector
                    .Builder
                    .createFor(this)
                    .withDoOnAll((clickType, event) -> {
                        LocalDate date = this.property().get();
                        Optional<Day.Info> dayInfo = dayInfoByDate.apply(date);
                        this.clickCallback.onClick(clickType, date, dayInfo);
                    })
                    .build();
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
    private final DayInMonth[][] daysStaticGrid;
    private final ByDatesHolder<DayInMonth> daysByDates;
    private final Possible<DayInMonth> currentToday;
    private final Label monthYearLabel;
    private final List<PseudoClassAppliedTo<DayInMonth>> appliedPseudoClasses;
    private final PseudoClassesBoundTo<LocalDate> pseudoClassesByDates;
    private final MidnightTimer midnightTimer;

    private final Calendar.State.Control calendarStateControl;
    private YearMonth currMonth;
    private YearMonth prevMonth;
    private YearMonth nextMonth;

    public MonthViewImpl(
            Calendar.State.Control calendarStateControl,
            DayInfoState dayInfoState,
            Day.Info.ToString dayInfoToString,
            Day.MouseCallback mouseCallback,
            DayOfWeek firstDayOfWeek,
            PseudoClassesBoundTo<LocalDate> pseudoClassesByDates,
            Function<LocalDate, String> defaultTooltipText) {
        this.calendarStateControl = calendarStateControl;
        this.currMonth = calendarStateControl.yearMonth();
        this.prevMonth = this.currMonth.minusMonths(1);
        this.nextMonth = this.currMonth.plusMonths(1);
        dayInfoState.add(this);

        this.view = new VBox();
        final ReadOnlyDoubleProperty spacing = this.view.spacingProperty();
        this.grid = new GridPane();
        VBox.setVgrow(this.grid, ALWAYS);

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

        double y = 100.0 / 7;
        RowConstraints c1 = new RowConstraints();
        c1.setPercentHeight(y);
        RowConstraints c2 = new RowConstraints();
        c2.setPercentHeight(y);
        RowConstraints c3 = new RowConstraints();
        c3.setPercentHeight(y);
        RowConstraints c4 = new RowConstraints();
        c4.setPercentHeight(y);
        RowConstraints c5 = new RowConstraints();
        c5.setPercentHeight(y);
        RowConstraints c6 = new RowConstraints();
        c6.setPercentHeight(y);
        RowConstraints c7 = new RowConstraints();
        c7.setPercentHeight(y);

        this.grid.getRowConstraints().addAll(c1, c2, c3, c4, c5, c6, c7);

        this.grid.vgapProperty().bind(spacing);
        this.grid.hgapProperty().bind(spacing);

        this.daysStaticGrid = new DayInMonth[6][7];
        this.daysByDates = new ByDatesHolder<>();
        this.currentToday = simplePossibleButEmpty();
        this.pseudoClassesByDates = pseudoClassesByDates;
        this.appliedPseudoClasses = new ArrayList<>();

        Map<Integer, DayOfWeek> daysByPosition = new HashMap<>();
        this.positionByDays = new HashMap<>();

        ReadOnlyDoubleProperty viewWidth = this.view.minWidthProperty();
        this.grid.minWidthProperty().bind(viewWidth);
        this.grid.maxWidthProperty().bind(viewWidth);

        final DoubleProperty gridCellWidth = new SimpleDoubleProperty();
        final DoubleProperty gridCellHeight = new SimpleDoubleProperty();
        final ObjectProperty<Font> font = new SimpleObjectProperty<>();

        this.view.getStyleClass().add("month-view");

        LocalDate firstDay = Dates.firstDayOf(this.currMonth);
        DayOfWeek firstDayName = firstDay.getDayOfWeek();
        DayOfWeek currentDay = firstDayOfWeek;

        for (int d = 1; d <= 7 ; d++) {
            daysByPosition.put(d, currentDay);
            positionByDays.put(currentDay, d);
            currentDay = currentDay.plus(1);
        }

        int firstDayIndexInFirstWeek = positionByDays.get(firstDayName);

        int prevMonthDaysRemnant = firstDayIndexInFirstWeek - 1;
        int prevMonthDaysIndex = Dates.firstDayOf(prevMonth).lengthOfMonth() - prevMonthDaysRemnant + 1;
        int currMonthDaysIndex = 1;
        int nextMonthDaysIndex = 1;
        int maxDays = firstDay.lengthOfMonth();
        LocalDate today = LocalDate.now();

        BlinkingDetector blinkingDetector = new BlinkingDetector(() -> {
            System.out.println("blinking detected");
        });

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
                    dayName.minWidthProperty().bind(gridCellWidth);
                    dayName.minWidthProperty().bind(gridCellWidth);

                    GridPane.setFillWidth(dayName, true);

                    dayName.getStyleClass().addAll("day-name", currentDay.name().toLowerCase());

                    this.grid.add(dayName, gridCol, gridRow);

                    currentDay = currentDay.plus(1);
                }
                continue;
            }

            DayInMonth day;
            for (int gridCol = 0; gridCol <= 6; gridCol++) {
                if ( gridRow == firstWeekRow && prevMonthDaysRemnant > 0 ) {
                    day = new DayInMonth(
                            this.prevMonth,
                            prevMonthDaysIndex,
                            defaultTooltipText,
                            dayInfoState::findDayInfoOf,
                            dayInfoToString,
                            mouseCallback);
                    day.prevMonthStyle();
                    day.setIsToday(today);

                    prevMonthDaysRemnant--;
                    prevMonthDaysIndex++;
                }
                else {
                    if ( currMonthDaysIndex <= maxDays ) {
                        day = new DayInMonth(
                                this.currMonth,
                                currMonthDaysIndex,
                                defaultTooltipText,
                                dayInfoState::findDayInfoOf,
                                dayInfoToString,
                                mouseCallback);
                        day.currentMonthStyle();
                        day.setIsToday(today);

                        currMonthDaysIndex++;
                    }
                    else {
                        day = new DayInMonth(
                                this.nextMonth,
                                nextMonthDaysIndex,
                                defaultTooltipText,
                                dayInfoState::findDayInfoOf,
                                dayInfoToString,
                                mouseCallback);
                        day.nextMonthStyle();
                        day.setIsToday(today);

                        nextMonthDaysIndex++;
                    }
                }

                if ( ! gridCellWidth.isBound() ) {
                    font.bind(day.fontProperty());
                    gridCellWidth.bind(day.widthProperty());
                    gridCellHeight.bind(day.heightProperty());

                    day.heightProperty().addListener((p, o, n) -> {
                        blinkingDetector.onChange((double) n);
                    });

                    gridCellWidth.addListener((p, o, n) -> {
                        System.out.println("grid cell width: " + n);
                    });
                    gridCellHeight.addListener((p, o, n) -> {
                    });
                }

                if ( day.isToday ) {
                    this.currentToday.resetTo(day);
                }

                daysByDates.put(day.date(), day);
                daysStaticGrid[gridRow-firstWeekRow][gridCol] = day;

                day.getStyleClass().add("day");
                day.setAlignment(CENTER);

                day.setMaxWidth(Double.MAX_VALUE);
                day.setMaxHeight(Double.MAX_VALUE);
                GridPane.setFillWidth(day, true);
                GridPane.setFillHeight(day, true);

                this.grid.add(day, gridCol, gridRow);
            }
        }

        this.applyGivenPseudoClasses();

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

        dayInfoState.load(this.prevMonth, this.currMonth, this.nextMonth);

        this.calendarStateControl.property().addListener((p, oldV, newV) -> {
            this.currMonth = YearMonth.of(newV.getYear(), newV.getMonth());
            this.prevMonth = this.currMonth.minusMonths(1);
            this.nextMonth = this.currMonth.plusMonths(1);

            dayInfoState.load(this.prevMonth, this.currMonth, this.nextMonth);

            this.fill();
        });
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
    public void onChange(LocalDate date) {
        DayInMonth day = this.daysByDates.findOrNull(date);
        if ( nonNull(day) ) {
            day.refresh();
        }
    }

    @Override
    public void onChange(YearMonth month) {
        this.daysByDates
                .findBy(month)
                .forEach(DayInMonth::refresh);
    }

    @Override
    public void onChange(Year year) {
        this.daysByDates
                .findBy(year)
                .forEach(DayInMonth::refresh);
    }

    @Override
    public Region node() {
        return this.view;
    }

    private void fill() {
        daysByDates.clear();
        this.revertGivenPseudoClasses();

        LocalDate firstDay = Dates.firstDayOf(currMonth);
        DayOfWeek firstDayName = firstDay.getDayOfWeek();

        int firstDayIndexInFirstWeek = positionByDays.get(firstDayName);

        int prevMonthDaysRemnant = firstDayIndexInFirstWeek - 1;
        int prevMonthDaysIndex = Dates.firstDayOf(prevMonth).lengthOfMonth() - prevMonthDaysRemnant + 1;
        int daysIndex = 1;
        int nextMonthDaysIndex = 1;
        int maxDays = firstDay.lengthOfMonth();
        LocalDate today = LocalDate.now();

        DayInMonth day;
        for (int w = 1; w <= 6; w++) {
            for (int d = 1; d <= 7; d++) {
                day = daysStaticGrid[w-1][d-1];

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

        this.monthYearLabel.setText(currMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currMonth.getYear());
    }

    private void revertGivenPseudoClasses() {
        appliedPseudoClasses.forEach(PseudoClassAppliedTo::revertNodePseudoClass);
        appliedPseudoClasses.clear();
    }

    private void applyGivenPseudoClasses() {
        pseudoClassesByDates.forEach((date, pseudoClasses) -> {
            DayInMonth dayByDate = daysByDates.findOrNull(date);
            if ( dayByDate != null ) {
                pseudoClasses.forEach((pseudoClass, active) -> {
                    dayByDate.pseudoClassStateChanged(pseudoClass, active);
                    appliedPseudoClasses.add(new PseudoClassAppliedTo<>(pseudoClass, active, dayByDate));
                });
            }
        });
    }

}
