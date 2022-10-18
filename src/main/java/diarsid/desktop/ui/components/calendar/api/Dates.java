package diarsid.desktop.ui.components.calendar.api;

import java.time.LocalDate;
import java.time.YearMonth;

public class Dates {

    public static LocalDate firstDayOf(YearMonth yearMonth) {
        return LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), 1);
    }
}
