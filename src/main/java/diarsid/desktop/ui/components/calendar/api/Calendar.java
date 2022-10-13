package diarsid.desktop.ui.components.calendar.api;

import diarsid.desktop.ui.components.calendar.impl.CalendarStateControl;
import diarsid.desktop.ui.components.calendar.impl.MonthViewImpl;
import diarsid.desktop.ui.components.calendar.impl.YearViewImpl;
import diarsid.support.javafx.Visible;
import diarsid.support.javafx.pseudoclasses.PseudoClassesBoundTo;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.function.Function;

public interface Calendar {

    Calendar.State.Control control();

    interface MonthView extends Calendar, Visible {

        static MonthView newMonthView(
                Calendar.State.Control calendarStateControl,
                DayOfWeek firstDayOfWeek,
                PseudoClassesBoundTo<LocalDate> pseudoClassesByDates,
                Function<LocalDate, String> defaultTooltipText) {
            return new MonthViewImpl(calendarStateControl, firstDayOfWeek, pseudoClassesByDates, defaultTooltipText);
        }

        @Override
        Region node();

        TooltipsByDate tooltipsByDate();
    }

    interface YearView extends Calendar, Visible {

        static YearView newYearView(
                Calendar.State.Control calendarStateControl,
                PseudoClassesBoundTo<LocalDate> pseudoClassesByDates,
                Function<LocalDate, String> defaultTooltipText) {
            return new YearViewImpl(calendarStateControl, pseudoClassesByDates, defaultTooltipText);
        }

        @Override
        Region node();

        TooltipsByDate tooltipsByDate();

    }

    interface State {

        ReadOnlyObjectProperty<LocalDate> property();

        int year();

        Month month();

        int dayOfMonth();

        default YearMonth yearMonth() {
            var date = this.property().get();
            return YearMonth.of(date.getYear(), date.getMonth());
        }

        DayOfWeek dayOfWeek();

        interface Control extends State {

            static Control newCalendarStateControl(LocalDate date) {
                return new CalendarStateControl(date);
            }

            void toNextMonth();

            void toPrevMonth();

            void toNextYear();

            void toPrevYear();

            void toNextDay();

            void toPrevDay();

            void toYear(int year);

            void toMonth(Month month);

            void toDayOfMonth(int dayOfMonth);

            void toYearAndMonth(int year, Month month);

            void toYearAndMonthAndDay(int year, Month month, int day);

            void toMonthAndDay(Month month, int day);
        }
    }
}
