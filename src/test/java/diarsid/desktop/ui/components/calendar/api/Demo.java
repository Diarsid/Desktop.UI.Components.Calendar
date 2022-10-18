package diarsid.desktop.ui.components.calendar.api;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import diarsid.support.javafx.PlatformStartup;
import diarsid.support.javafx.css.pseudoclasses.PseudoClassesBoundTo;

import static java.lang.String.format;
import static java.time.DayOfWeek.MONDAY;
import static java.time.LocalDate.now;
import static java.time.format.TextStyle.FULL;

import static diarsid.desktop.ui.components.calendar.api.Calendar.MonthView.newMonthView;
import static diarsid.desktop.ui.components.calendar.api.Calendar.State.Control.newCalendarStateControl;
import static diarsid.desktop.ui.components.calendar.api.Calendar.YearView.newYearView;
import static diarsid.desktop.ui.components.calendar.api.Dates.firstDayOf;
import static diarsid.desktop.ui.components.calendar.api.Day.Info.Control.newDayInfoControl;

public class Demo {

    private static final Function<LocalDate, String> DATE_TO_STRING = date -> {
        return format("%s %s", date.getDayOfMonth(), date.getMonth().getDisplayName(FULL, Locale.getDefault()));
    };

    private static final Function<LocalDate, String> DATE_TO_STRING_2 = date -> {
        return "nothing";
    };

    public static void main(String[] args) throws Exception {
        PlatformStartup.await();
        DoubleProperty width = new SimpleDoubleProperty(400);
        DoubleProperty height = new SimpleDoubleProperty(400);
        AtomicReference<Stage> stageRef = new AtomicReference<>();

        String lineEnd = System.lineSeparator();

        Day.Info.ToString toString = (dayInfo) -> {
            StringBuilder builder = new StringBuilder();
            String date = dayInfo.date().toString();
            builder
                    .append(date)
                    .append(" ")
                    .append(dayInfo.header().or(""))
                    .append(lineEnd);

            if ( dayInfo.content().isPresent() ) {
                for ( var contentLine : dayInfo.content().get() ) {
                    builder.append(" - ").append(contentLine).append(lineEnd);
                }
            }

            return builder.toString();
        };

        Calendar.State.Control calendarStateControl = newCalendarStateControl(now());
        Day.Info.Control dayInfoControl = newDayInfoControl(toString);

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
            Day.Info.Repository dayInfoRepository = new Day.Info.Repository() {
                @Override
                public Optional<Day.Info> findBy(LocalDate date) {
                    return Optional.empty();
                }

                @Override
                public Map<LocalDate, Day.Info> findAllBy(YearMonth month) {
                    return null;
                }

                @Override
                public Map<LocalDate, Day.Info> findAllBy(Year year) {
                    return null;
                }
            };

            Day.MouseCallback mouseCallback = new Day.MouseCallback() {
                @Override
                public void onSingleClick(LocalDate date, Optional<Day.Info> dayInfo) {
                    System.out.println("single click on " + date + ", info:" + dayInfo.map(toString).orElse(null));
                }

                @Override
                public void onMultiClick(LocalDate date, Optional<Day.Info> dayInfo) {
                    System.out.println("multi click on " + date + ", info:" + dayInfo.map(toString).orElse(null));
                }
            };

            Calendar.MonthView monthView = newMonthView(calendarStateControl, dayInfoControl, dayInfoRepository, toString, mouseCallback, MONDAY, pseudoClassesToDates, DATE_TO_STRING_2);
            Calendar.YearView yearView = newYearView(calendarStateControl, dayInfoControl, dayInfoRepository, toString, mouseCallback, pseudoClassesToDates, DATE_TO_STRING);

//            yearView.node().minWidthProperty().bind(width);
//            yearView.node().maxWidthProperty().bind(width);

//            monthView.node().minHeightProperty().bind(height);
//            monthView.node().prefHeightProperty().bind(height);
//            monthView.node().minWidthProperty().bind(width);
//            monthView.node().maxWidthProperty().bind(width);

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

//        for ( int i = 0; i < 3; i++ ) {
//            Thread.sleep(1000);
//            Platform.runLater(() -> {
//                width.set(width.get() + 60);
//                stageRef.get().sizeToScene();
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
//            });
//        }
    }

}
