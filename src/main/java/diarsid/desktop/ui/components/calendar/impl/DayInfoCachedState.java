package diarsid.desktop.ui.components.calendar.impl;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import diarsid.desktop.ui.components.calendar.api.Day;

import static java.util.Collections.synchronizedList;
import static java.util.Objects.nonNull;

public class DayInfoCachedState implements Day.Info.Control, DayInfoState {

    private static final Logger log = LoggerFactory.getLogger(DayInfoCachedState.class);

    private final Day.Info.Repository repository;
    private final Map<LocalDate, Day.Info> infoByDate;
    protected List<ChangesListener> changesListener;

    public DayInfoCachedState(Day.Info.Repository repository) {
        this.repository = repository;
        this.infoByDate = new ConcurrentHashMap<>();
        this.changesListener = synchronizedList(new ArrayList<>());
    }

    @Override
    public void load(YearMonth prev, YearMonth current, YearMonth next) {
        infoByDate.putAll(this.repository.findAllBy(prev));
        infoByDate.putAll(this.repository.findAllBy(current));
        infoByDate.putAll(this.repository.findAllBy(next));

        this.changed(prev, current, next);
    }

    @Override
    public void load(Year year) {
        this.infoByDate.putAll(this.repository.findAllBy(year));
        this.changed(year);
    }

    @Override
    public Optional<Day.Info> findDayInfoOf(LocalDate date) {
        Day.Info dayInfo = this.infoByDate.get(date);
        return Optional.ofNullable(dayInfo);
    }

    @Override
    public void add(ChangesListener changesListener) {
        this.changesListener.add(changesListener);
    }

    @Override
    public void set(Day.Info dayInfo) {
        LocalDate date = dayInfo.date();
        Day.Info dayInfoOld = this.infoByDate.get(date);
        this.infoByDate.put(date, dayInfo);

        boolean notChanged = false;
        if ( nonNull(dayInfoOld) ) {
            if (dayInfoOld.equals(dayInfo)) {
                notChanged = true;
            }
        }

        if ( ! notChanged ) {
            this.changed(date);
        }
    }

    @Override
    public void refresh(Year year) {
        this.load(year);
    }

    @Override
    public void refresh(YearMonth month) {
        this.load(
                month.minusMonths(1),
                month,
                month.plusMonths(1));
    }

    @Override
    public void refresh(LocalDate date) {
        Optional<Day.Info> dayInfo = this.repository.findBy(date);

        if ( dayInfo.isEmpty() ) {
            return;
        }

        this.infoByDate.put(date, dayInfo.get());
        this.changed(date);
    }

    private void changed(YearMonth prev, YearMonth current, YearMonth next) {
        for ( var listener : this.changesListener ) {
            try {
                listener.onChange(prev);
                listener.onChange(current);
                listener.onChange(next);
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void changed(Year year) {
        for ( var listener : this.changesListener ) {
            try {
                listener.onChange(year);
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void changed(LocalDate date) {
        for ( var listener : this.changesListener ) {
            try {
                listener.onChange(date);
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
