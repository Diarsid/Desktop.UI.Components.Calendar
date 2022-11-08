package diarsid.desktop.ui.components.calendar.impl;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

public class ByDatesHolder<T> {

    private final Map<LocalDate, T> tByDate;
    private final Map<Year, List<T>> tsByYears;
    private final Map<YearMonth, List<T>> tsByMonths;

    public ByDatesHolder() {
        this.tByDate = new HashMap<>();
        this.tsByYears = new HashMap<>();
        this.tsByMonths = new HashMap<>();
    }

    T findOrNull(LocalDate date) {
        return this.tByDate.get(date);
    }

    List<T> findBy(Year year) {
        var ts = this.tsByYears.get(year);
        if ( isNull(ts) ) {
            ts = emptyList();
        }
        return ts;
    }

    List<T> findBy(YearMonth month) {
        var ts = this.tsByMonths.get(month);
        if ( isNull(ts) ) {
            ts = emptyList();
        }
        return ts;
    }

    void put(LocalDate date, T t) {
        this.tByDate.put(date, t);

        YearMonth month = YearMonth.from(date);
        List<T> tsByMonth = this.tsByMonths.get(month);
        if ( isNull(tsByMonth) ) {
            tsByMonth = new ArrayList<>();
            this.tsByMonths.put(month, tsByMonth);
        }
        tsByMonth.add(t);

        Year year = Year.from(month);
        List<T> tsByYear = this.tsByYears.get(year);
        if ( isNull(tsByYear) ) {
            tsByYear = new ArrayList<>();
            this.tsByYears.put(year, tsByYear);
        }
        tsByYear.add(t);
    }

    void clear() {
        this.tByDate.clear();
        this.tsByMonths.clear();
        this.tsByYears.clear();
    }
}
