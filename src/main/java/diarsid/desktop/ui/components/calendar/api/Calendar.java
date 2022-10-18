package diarsid.desktop.ui.components.calendar.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.layout.Region;

import diarsid.desktop.ui.components.calendar.impl.CalendarStateControl;
import diarsid.desktop.ui.components.calendar.impl.MonthViewImpl;
import diarsid.desktop.ui.components.calendar.impl.YearViewImpl;
import diarsid.support.javafx.components.Visible;
import diarsid.support.javafx.css.pseudoclasses.PseudoClassesBoundTo;

public interface Calendar {

    interface MonthView extends Calendar, Visible {

        static MonthView newMonthView(
                Calendar.State.Control calendarStateControl,
                Day.Info.Control dayInfoControl,
                Day.Info.Repository dayInfoRepository,
                Day.Info.ToString dayInfoToString,
                Day.MouseCallback mouseCallback,
                DayOfWeek firstDayOfWeek,
                PseudoClassesBoundTo<LocalDate> pseudoClassesByDates,
                Function<LocalDate, String> defaultTooltipText) {
            return new MonthViewImpl(
                    calendarStateControl,
                    dayInfoControl,
                    dayInfoRepository,
                    mouseCallback,
                    firstDayOfWeek,
                    pseudoClassesByDates,
                    defaultTooltipText);
        }

        @Override
        Region node();
    }

    interface YearView extends Calendar, Visible {

        static YearView newYearView(
                Calendar.State.Control calendarStateControl,
                Day.Info.Control dayInfoControl,
                Day.Info.Repository dayInfoRepository,
                Day.Info.ToString dayInfoToString,
                Day.MouseCallback mouseCallback,
                PseudoClassesBoundTo<LocalDate> pseudoClassesByDates,
                Function<LocalDate, String> defaultTooltipText) {
            return new YearViewImpl(
                    calendarStateControl,
                    dayInfoControl,
                    dayInfoRepository,
                    mouseCallback,
                    pseudoClassesByDates,
                    defaultTooltipText);
        }

        @Override
        Region node();

    }

    interface State {

        ReadOnlyObjectProperty<LocalDate> property();

        default YearMonth yearMonth() {
            var date = this.property().get();
            return YearMonth.of(date.getYear(), date.getMonth());
        }

        default int year() {
            return this.property().get().getYear();
        }

        default Month month() {
            return this.property().get().getMonth();
        }

        default int dayOfMonth() {
            return this.property().get().getDayOfMonth();
        }

        default DayOfWeek dayOfWeek() {
            return this.property().get().getDayOfWeek();
        }

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
