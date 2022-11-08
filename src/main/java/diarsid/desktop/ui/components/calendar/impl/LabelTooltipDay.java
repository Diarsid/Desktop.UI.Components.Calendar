package diarsid.desktop.ui.components.calendar.impl;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.function.Function;

import diarsid.desktop.ui.components.calendar.api.Day;

public class LabelTooltipDay extends LabeledWithTooltip<LocalDate> {

    public LabelTooltipDay(
            LocalDate date,
            Function<LocalDate, String> toLabelString,
            Function<LocalDate, Optional<Day.Info>> dayInfoByDate,
            Function<LocalDate, String> defaultTooltipText,
            Day.Info.ToString dayInfoToString) {
        super(
                date,
                toLabelString,
                (newDate) -> {
                    Optional<Day.Info> dayInfoLoaded = dayInfoByDate.apply(newDate);

                    String text;
                    if ( dayInfoLoaded.isPresent() ) {
                        var dayInfo = dayInfoLoaded.get();
                        if ( dayInfo.customToString().isPresent() ) {
                            text = dayInfo.customToString().get().apply(dayInfo);
                        }
                        else {
                            text = dayInfoToString.apply(dayInfo);
                        }
                    }
                    else {
                        text = defaultTooltipText.apply(newDate);
                    }

                    return text;
                });
    }
}
