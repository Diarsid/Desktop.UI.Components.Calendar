package diarsid.desktop.ui.components.calendar.impl;

import diarsid.desktop.ui.components.calendar.api.Calendar;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

public class CalendarStateControl implements Calendar.State.Control {

    private final ObjectProperty<LocalDate> date;

    public CalendarStateControl(int year, Month month, int day) {
        this.date = new SimpleObjectProperty<>(LocalDate.of(year, month, day));
    }

    public CalendarStateControl(LocalDate date) {
        this.date = new SimpleObjectProperty<>(date);
    }

    @Override
    public ReadOnlyObjectProperty<LocalDate> property() {
        return date;
    }

    @Override
    public int year() {
        return date.get().getYear();
    }

    @Override
    public Month month() {
        return date.get().getMonth();
    }

    @Override
    public int dayOfMonth() {
        return date.get().getDayOfMonth();
    }

    @Override
    public DayOfWeek dayOfWeek() {
        return date.get().getDayOfWeek();
    }

    @Override
    public void toNextMonth() {
        LocalDate current = date.get();
        date.set(current.plusMonths(1));
    }

    @Override
    public void toPrevMonth() {
        LocalDate current = date.get();
        date.set(current.minusMonths(1));
    }

    @Override
    public void toNextYear() {
        LocalDate current = date.get();
        date.set(current.plusYears(1));
    }

    @Override
    public void toPrevYear() {
        LocalDate current = date.get();
        date.set(current.minusYears(1));
    }

    @Override
    public void toYear(int year) {
        LocalDate current = date.get();
        date.set(current.withYear(year));
    }

    @Override
    public void toMonth(Month month) {
        LocalDate current = date.get();
        date.set(current.withMonth(month.getValue()));
    }

    @Override
    public void toYearAndMonth(int year, Month month) {
        LocalDate current = date.get();
        date.set(current.withYear(year).withMonth(month.getValue()));
    }

    @Override
    public void toNextDay() {
        LocalDate current = date.get();
        LocalDate next = current.plusDays(1);
        date.set(next);
    }

    @Override
    public void toPrevDay() {
        LocalDate current = date.get();
        LocalDate next = current.minusDays(1);
        date.set(next);
    }

    @Override
    public void toDayOfMonth(int dayOfMonth) {
        LocalDate current = date.get();
        LocalDate next = current.withDayOfMonth(dayOfMonth);
        date.set(next);
    }

    @Override
    public void toYearAndMonthAndDay(int year, Month month, int day) {
        date.set(LocalDate.of(year, month, day));
    }

    @Override
    public void toMonthAndDay(Month month, int day) {
        LocalDate current = date.get();
        LocalDate next = current.withMonth(month.getValue()).withDayOfMonth(day);
        date.set(next);
    }
}
