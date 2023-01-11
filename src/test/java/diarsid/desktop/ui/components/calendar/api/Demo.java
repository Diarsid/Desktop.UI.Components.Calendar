package diarsid.desktop.ui.components.calendar.api;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import diarsid.desktop.ui.components.calendar.api.defaultimpl.DayInfo;
import diarsid.desktop.ui.components.calendar.api.defaultimpl.DayInfoToString;
import diarsid.support.javafx.PlatformActions;
import diarsid.support.javafx.css.pseudoclasses.PseudoClassesBoundTo;

import static java.lang.String.format;
import static java.time.DayOfWeek.MONDAY;
import static java.time.LocalDate.now;
import static java.time.Month.DECEMBER;
import static java.time.Month.OCTOBER;
import static java.time.Month.SEPTEMBER;
import static java.time.format.TextStyle.FULL;

import static diarsid.desktop.ui.components.calendar.api.Calendar.MonthView.newMonthView;
import static diarsid.desktop.ui.components.calendar.api.Calendar.State.Control.newCalendarStateControl;
import static diarsid.desktop.ui.components.calendar.api.Calendar.YearView.newYearView;
import static diarsid.desktop.ui.components.calendar.api.Dates.firstDayOf;
import static diarsid.desktop.ui.components.calendar.api.Day.Info.Control.newDayInfoControl;

public class Demo {

    private static final Function<LocalDate, String> DATE_TO_STRING_YEAR = date -> {
        return format("%s %s, %s",
                date.getDayOfMonth(),
                date.getMonth().getDisplayName(FULL, Locale.getDefault()),
                date.getDayOfWeek().getDisplayName(FULL, Locale.getDefault()));
    };

    private static final Function<LocalDate, String> DATE_TO_STRING_MONTH = date -> {
        return "nothing";
    };

    public static void main(String[] args) throws Exception {
        PlatformActions.awaitStartup();
//        DoubleProperty width = new SimpleDoubleProperty(400);
//        DoubleProperty height = new SimpleDoubleProperty(400);
        AtomicReference<Stage> stageRef = new AtomicReference<>();

        Day.Info.ToString toString = DayInfoToString.DEFAULT;

        Calendar.State.Control calendarStateControl = newCalendarStateControl(now());

        Day.Info.Repository dayInfoRepository = new Day.Info.Repository() {

            private final Map<LocalDate, Day.Info> map;

            {
                this.map = new HashMap<>();

                var october25 = LocalDate.of(2022, OCTOBER, 25);
                this.map.put(october25, new DayInfo(october25, "Event", "aaaa", "bbbbbb", "cc"));

                var september1 = LocalDate.of(2022, SEPTEMBER, 1);
                this.map.put(september1, new DayInfo(september1, "to School!", "to learn"));

                var december24 = LocalDate.of(2022, DECEMBER, 24);
                this.map.put(december24, new DayInfo(december24, "Christmas!", "jingle bells", "jingle bells"));
            }

            @Override
            public Optional<Day.Info> findBy(LocalDate date) {
                return Optional.ofNullable(this.map.get(date));
            }

            @Override
            public Map<LocalDate, Day.Info> findAllBy(YearMonth month) {
                return this.map
                        .entrySet()
                        .stream()
                        .filter((entry) -> entry.getKey().getMonth() == month.getMonth())
                        .collect(Collectors.toMap(
                                entry -> entry.getKey(),
                                entry -> entry.getValue(),
                                (info1, info2) -> info1));
            }

            @Override
            public Map<LocalDate, Day.Info> findAllBy(Year year) {
                return this.map
                        .entrySet()
                        .stream()
                        .filter((entry) -> entry.getKey().getYear() == year.getValue())
                        .collect(Collectors.toMap(
                                entry -> entry.getKey(),
                                entry -> entry.getValue(),
                                (info1, info2) -> info1));
            }
        };

        Day.Info.Control dayInfoControl = newDayInfoControl(dayInfoRepository);

        LocalDate firstDayOfMonth = firstDayOf(calendarStateControl.yearMonth());
        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);

            stage.setAlwaysOnTop(true);
//            stage.setMinWidth(200);
//            stage.setMinHeight(100);
            stage.setResizable(true);



            PseudoClassesBoundTo<LocalDate> pseudoClassesToDates = new PseudoClassesBoundTo<>();


            Day.MouseCallback mouseCallback = (click, date, dayInfo) -> {
                System.out.println("click " + click.name() + " " + date);
            };

            Calendar.MonthView monthView = newMonthView(
                    calendarStateControl,
                    dayInfoControl,
                    toString,
                    mouseCallback,
                    MONDAY,
                    pseudoClassesToDates,
                    DATE_TO_STRING_MONTH);

            Calendar.YearView yearView = newYearView(
                    calendarStateControl,
                    dayInfoControl,
                    toString,
                    mouseCallback,
                    pseudoClassesToDates,
                    DATE_TO_STRING_YEAR);

//            yearView.node().minWidthProperty().bind(width);
//            monthView.node().minWidthProperty().bind(width);

//            monthView.node().minHeightProperty().bind(height);
//            monthView.node().prefHeightProperty().bind(height);

//            yearView.node().maxWidthProperty().bind(width);

            VBox window = new VBox();
            window.getChildren().addAll(monthView.node(), yearView.node());

            Scene scene = new Scene(window);
            scene.setFill(Color.TRANSPARENT);
            scene.getStylesheets().add("file:./src/test/resources/style.css");

            stage.setScene(scene);
            stage.sizeToScene();
            stage.show();

            stageRef.set(stage);

        });

        dayInfoControl.set(new DayInfo(firstDayOfMonth, "first day", List.of("first", "second")));
        dayInfoControl.set(new DayInfo(lastDayOfMonth, "last day", List.of("first", "second")));

        AtomicInteger n = new AtomicInteger(0);

        Function<Integer, Day.Info> toInfo = (i) -> {
            return DayInfo
                    .Builder
                    .ofDate(now())
                    .withHeader("i: " + i)
                    .withToString((info) -> info.header().get())
                    .build();
        };

//        while ( true ) {
//            Thread.sleep(1000);
//            Platform.runLater(() -> {
//                dayInfoControl.set(toInfo.apply(n.incrementAndGet()));
//            });
//
//        }

//        for ( int i = 0; i < 3; i++ ) {
//            Thread.sleep(1000);
//            Platform.runLater(() -> {
//                width.set(width.get() + 60);
//                stageRef.get().sizeToScene();
//                System.out.println("width +");
//            });
//
//        }
//
//        Thread.sleep(1000);
//
//
//        for ( int i = 0; i < 3; i++ ) {
//            Thread.sleep(1000);
//            Platform.runLater(() -> {
//                width.set(width.get() - 60);
//                stageRef.get().sizeToScene();
//                System.out.println("width -");
//            });
//        }
    }

}
