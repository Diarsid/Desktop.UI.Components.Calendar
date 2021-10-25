package diarsid.jfxmonthview;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import static java.lang.Double.MAX_VALUE;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static javafx.geometry.Pos.CENTER;
import static javafx.scene.layout.Priority.ALWAYS;

public class JFXMonthView {

    public static final Map<DayOfWeek, String> SHORT_NAMES_BY_DAYS;
    public static final PseudoClass PREVIOUS_MONTH = PseudoClass.getPseudoClass("prev-month");
    public static final PseudoClass CURRENT_MONTH = PseudoClass.getPseudoClass("current-month");
    public static final PseudoClass NEXT_MONTH = PseudoClass.getPseudoClass("next-month");

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

    final VBox view;
    final VBox daysOfWeek;
    final Map<DayOfWeek, Integer> positionByDays;
    final Label[][] grid;
    final Label monthYearLabel;
    final boolean useCssWidth;
    private int year;
    private Month month;

    public JFXMonthView(DayOfWeek firstDayOfWeek, int year, Month month, boolean useCssWidth) {
        this.month = month;
        this.year = year;
        this.useCssWidth = useCssWidth;

        this.view = new VBox();
        this.daysOfWeek = new VBox();
        this.grid = new Label[6][7];

        this.view.getStyleClass().add("month-view");

        Map<Integer, DayOfWeek> daysByPosition = new HashMap<>();
        positionByDays = new HashMap<>();

        Month prevMonth = month.minus(1);
        int yearOfPrevMonth;
        if ( prevMonth == DECEMBER ) {
            yearOfPrevMonth = year - 1;
        }
        else {
            yearOfPrevMonth = year;
        }
        LocalDate firstDay = LocalDate.of(year, month, 1);
        DayOfWeek firstDayName = firstDay.getDayOfWeek();
        DayOfWeek currentDay = firstDayOfWeek;

        for (int d = 1; d <= 7 ; d++) {
            daysByPosition.put(d, currentDay);
            positionByDays.put(currentDay, d);
            currentDay = currentDay.plus(1);
        }

        int firstDayIndexInFirstWeek = positionByDays.get(firstDayName);

        int prevMonthDaysRemnant = firstDayIndexInFirstWeek - 1;
        int prevMonthDaysIndex = LocalDate.of(yearOfPrevMonth, prevMonth, 1).lengthOfMonth() - prevMonthDaysRemnant + 1;
        int daysIndex = 1;
        int nextMonthDaysIndex = 1;
        int maxDays = firstDay.lengthOfMonth();

        ReadOnlyDoubleProperty gridWidth = null;
        ReadOnlyDoubleProperty gridHeight = null;
        ReadOnlyDoubleProperty gridSpacing = null;
        ReadOnlyObjectProperty<Font> font = null;
        HBox days = null;
        for (int w = 1; w <= 6; w++) {
            days = new HBox();
            days.getStyleClass().add("days");
            if ( gridSpacing == null ) {
                gridSpacing = days.spacingProperty();
            }
            Label day;
            for (int d = 1; d <= 7; d++) {
                day = new Label();
                day.getStyleClass().add("day");

                if (useCssWidth) {
                    if ( gridWidth == null ) {
                        gridWidth = day.minWidthProperty();
                        gridHeight = day.minHeightProperty();
                        font = day.fontProperty();
                    }

                    day.maxWidthProperty().bind(gridWidth);
                    day.maxHeightProperty().bind(gridHeight);
                }
                else {
                    if ( gridWidth == null && w == 3 ) {
                        gridWidth = day.widthProperty();
                        gridHeight = day.heightProperty();
                        font = day.fontProperty();
                    }

                    day.setMaxWidth(MAX_VALUE);
                    day.setMaxHeight(MAX_VALUE);
                    HBox.setHgrow(day, ALWAYS);
                }

                day.setAlignment(CENTER);
                days.getChildren().add(day);

                grid[w-1][d-1] = day;

                if ( w == 1 && prevMonthDaysRemnant > 0 ) {
                    prevMonthDaysRemnant--;
                    setPrevMonthDay(prevMonthDaysIndex, day);
                    prevMonthDaysIndex++;
                }
                else {
                    if ( daysIndex <= maxDays ) {
                        setCurrentMonthDay(daysIndex, day);
                        daysIndex++;
                    }
                    else {
                        setNextMonthDay(nextMonthDaysIndex, day);
                        nextMonthDaysIndex++;
                    }
                }

            }

            days.autosize();
            daysOfWeek.getChildren().add(days);
        }

        if ( !useCssWidth) {
            Label day;
            for (int w = 1; w <= 6; w++) {
                for (int d = 1; d <= 7; d++) {
                    day = grid[w-1][d-1];
                    day.minWidthProperty().bind(gridWidth);
                }
            }
        }

        HBox daysOfWeekNames = new HBox();
        daysOfWeekNames.getStyleClass().add("day-names");

        currentDay = firstDayOfWeek;
        for (int d = 1; d <= 7 ; d++) {
            Label dayName = new Label();
            dayName.setText(SHORT_NAMES_BY_DAYS.get(currentDay));

            dayName.fontProperty().bind(font);
            dayName.minHeightProperty().bind(gridHeight);
            dayName.maxHeightProperty().bind(gridHeight);
            dayName.minWidthProperty().bind(gridWidth);
            dayName.maxWidthProperty().bind(gridWidth);

            dayName.setAlignment(CENTER);

            dayName.getStyleClass().add("day-name");
            daysOfWeekNames.getChildren().add(dayName);

            currentDay = currentDay.plus(1);
        }

        daysOfWeek.spacingProperty().bind(gridSpacing);
        daysOfWeek.getStyleClass().add("days");

        ReadOnlyDoubleProperty viewWidth = days.widthProperty();

        if (useCssWidth) {
            daysOfWeek.minWidthProperty().bind(viewWidth);
            daysOfWeek.maxWidthProperty().bind(viewWidth);

            daysOfWeekNames.minWidthProperty().bind(viewWidth);
            daysOfWeekNames.maxWidthProperty().bind(viewWidth);

            view.minWidthProperty().bind(viewWidth);
            view.maxWidthProperty().bind(viewWidth);
        }

        daysOfWeekNames.minHeightProperty().bind(gridHeight);
        daysOfWeekNames.maxHeightProperty().bind(gridHeight);

        daysOfWeekNames.spacingProperty().bind(gridSpacing);
        view.spacingProperty().bind(gridSpacing);

        BorderPane dateInfo = new BorderPane();
        Label prevButton = new Label();
        Label nextButton = new Label();

        if (useCssWidth) {
            prevButton.minWidthProperty().bind(gridWidth);
            prevButton.maxWidthProperty().bind(gridWidth);
        }
        prevButton.minHeightProperty().bind(gridHeight);
        prevButton.maxHeightProperty().bind(gridHeight);

        prevButton.setOnMousePressed(event -> this.prev());
        prevButton.setText("<");
        prevButton.getStyleClass().add("prev-month");

        if (useCssWidth) {
            nextButton.minWidthProperty().bind(gridWidth);
            nextButton.maxWidthProperty().bind(gridWidth);
        }
        nextButton.minHeightProperty().bind(gridHeight);
        nextButton.maxHeightProperty().bind(gridHeight);

        nextButton.setText(">");
        nextButton.setOnMousePressed(event -> this.next());
        nextButton.getStyleClass().add("next-month");

        monthYearLabel = new Label();
        monthYearLabel.fontProperty().bind(font);
        monthYearLabel.setText(month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + year);
        monthYearLabel.getStyleClass().add("current-month");

        dateInfo.setLeft(prevButton);
        dateInfo.setRight(nextButton);
        dateInfo.setCenter(monthYearLabel);
        dateInfo.maxWidthProperty().bind(daysOfWeek.minWidthProperty());
        dateInfo.getStyleClass().add("months-menu");

        dateInfo.minHeightProperty().bind(gridHeight);
        dateInfo.maxHeightProperty().bind(gridHeight);

        view.getChildren().addAll(dateInfo, daysOfWeekNames, daysOfWeek);
    }

    private static void setNextMonthDay(int dayNumber, Label day) {
        day.setText(String.valueOf(dayNumber));
        day.pseudoClassStateChanged(PREVIOUS_MONTH, false);
        day.pseudoClassStateChanged(CURRENT_MONTH, false);
        day.pseudoClassStateChanged(NEXT_MONTH, true);
    }

    private static void setCurrentMonthDay(int dayNumber, Label day) {
        day.setText(String.valueOf(dayNumber));
        day.pseudoClassStateChanged(PREVIOUS_MONTH, false);
        day.pseudoClassStateChanged(CURRENT_MONTH, true);
        day.pseudoClassStateChanged(NEXT_MONTH, false);
    }

    private static void setPrevMonthDay(int dayNumber, Label day) {
        day.setText(String.valueOf(dayNumber));
        day.pseudoClassStateChanged(PREVIOUS_MONTH, true);
        day.pseudoClassStateChanged(CURRENT_MONTH, false);
        day.pseudoClassStateChanged(NEXT_MONTH, false);
    }

    void next() {
        month = month.plus(1);
        if ( month == JANUARY ) {
            year++;
        }
        fill();
    }

    void prev() {
        month = month.minus(1);
        if ( month == DECEMBER ) {
            year--;
        }
        fill();
    }

    private void fill() {
        Month prevMonth = month.minus(1);
        int yearOfPrevMonth;
        if ( prevMonth == DECEMBER ) {
            yearOfPrevMonth = year - 1;
        }
        else {
            yearOfPrevMonth = year;
        }
        LocalDate firstDay = LocalDate.of(year, month, 1);
        DayOfWeek firstDayName = firstDay.getDayOfWeek();

        int firstDayIndexInFirstWeek = positionByDays.get(firstDayName);

        int prevMonthDaysRemnant = firstDayIndexInFirstWeek - 1;
        int prevMonthDaysIndex = LocalDate.of(yearOfPrevMonth, prevMonth, 1).lengthOfMonth() - prevMonthDaysRemnant + 1;
        int daysIndex = 1;
        int nextMonthDaysIndex = 1;
        int maxDays = firstDay.lengthOfMonth();

        Label day;
        for (int w = 1; w <= 6; w++) {
            for (int d = 1; d <= 7; d++) {
                day = grid[w-1][d-1];

                if ( w == 1 && prevMonthDaysRemnant > 0 ) {
                    prevMonthDaysRemnant--;
                    setPrevMonthDay(prevMonthDaysIndex, day);
                    prevMonthDaysIndex++;
                }
                else {
                    if ( daysIndex <= maxDays ) {
                        setCurrentMonthDay(daysIndex, day);
                        daysIndex++;
                    }
                    else {
                        setNextMonthDay(nextMonthDaysIndex, day);
                        nextMonthDaysIndex++;
                    }
                }
            }
        }

        monthYearLabel.setText(month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + year);
    }
}
