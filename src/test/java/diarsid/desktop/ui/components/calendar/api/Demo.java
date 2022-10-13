package diarsid.desktop.ui.components.calendar.api;

import diarsid.support.javafx.PlatformStartup;
import diarsid.support.javafx.pseudoclasses.PseudoClassesBoundTo;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static diarsid.desktop.ui.components.calendar.api.Calendar.MonthView.newMonthView;
import static diarsid.desktop.ui.components.calendar.api.Calendar.State.Control.newCalendarStateControl;
import static diarsid.desktop.ui.components.calendar.api.Calendar.YearView.newYearView;
import static java.lang.String.format;
import static java.time.DayOfWeek.MONDAY;
import static java.time.LocalDate.now;
import static java.time.format.TextStyle.FULL;

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
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);

            stage.setAlwaysOnTop(true);
            stage.setMinWidth(200);
            stage.setMinHeight(100);
            stage.setResizable(true);

            PseudoClassesBoundTo<LocalDate> pseudoClassesToDates = new PseudoClassesBoundTo<>();
            Calendar.State.Control calendarStateControl = newCalendarStateControl(now());
            Calendar.MonthView monthView = newMonthView(calendarStateControl, MONDAY, pseudoClassesToDates, DATE_TO_STRING_2);
            Calendar.YearView yearView = newYearView(calendarStateControl, pseudoClassesToDates, DATE_TO_STRING);

            yearView.node().minWidthProperty().bind(width);
            yearView.node().maxWidthProperty().bind(width);

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

        for ( int i = 0; i < 3; i++ ) {
            Thread.sleep(1000);
            Platform.runLater(() -> {
                width.set(width.get() + 60);
                stageRef.get().sizeToScene();
            });

        }

        Thread.sleep(1000);


        for ( int i = 0; i < 3; i++ ) {
            Thread.sleep(1000);
            Platform.runLater(() -> {
                width.set(width.get() - 60);
                stageRef.get().sizeToScene();
            });
        }
    }

}
