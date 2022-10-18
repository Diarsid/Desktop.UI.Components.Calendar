package diarsid.desktop.ui.components.calendar.api;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import diarsid.desktop.ui.components.calendar.impl.DayInfoTooltipBinding;
import diarsid.support.objects.references.Possible;

public interface Day {

    interface Info {

        LocalDate date();

        Possible<String> header();

        Possible<List<String>> content();

        interface ToString extends Function<Day.Info, String> {

        }

        interface Repository {

            Optional<Day.Info> findBy(LocalDate date);

            Map<LocalDate, Day.Info> findAllBy(YearMonth month);

            Map<LocalDate, Day.Info> findAllBy(Year year);

            /*
             * logic to be overridden if updates made by Day.Info.Control.set() need to be persisted in some
             * underlying storage.
             * This method is not mandatory to be implemented if there is no need to save such changes.
             * */
            default boolean update(Day.Info dayInfo) {
                return true;
            }
        }

        interface Control {

            static Day.Info.Control newDayInfoControl(Day.Info.ToString toString) {
                return new DayInfoTooltipBinding(new HashMap<>(), new HashMap<>(), toString);
            }

            void set(LocalDate date, String text);

            void set(Day.Info dayInfo);

        }
    }

    interface MouseCallback {

        default void onSingleClick(LocalDate date, Optional<Day.Info> dayInfo) {
            // to be overridden
        }

        default void onMultiClick(LocalDate date, Optional<Day.Info> dayInfo) {
            // to be overridden
        }
    }
}
