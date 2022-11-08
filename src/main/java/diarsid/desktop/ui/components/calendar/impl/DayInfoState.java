package diarsid.desktop.ui.components.calendar.impl;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Optional;

import diarsid.desktop.ui.components.calendar.api.Day;

public interface DayInfoState {

    public static interface ChangesListener {

        void onChange(LocalDate date);

        void onChange(YearMonth month);

        void onChange(Year year);
    }

    void load(YearMonth prev, YearMonth current, YearMonth next);

    void load(Year year);

    default void load(int yearInt) {
        this.load(Year.of(yearInt));
    }

    Optional<Day.Info> findDayInfoOf(LocalDate date);

    void add(ChangesListener changesListener);
}
