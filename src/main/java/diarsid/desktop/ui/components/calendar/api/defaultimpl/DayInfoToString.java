package diarsid.desktop.ui.components.calendar.api.defaultimpl;

import java.time.format.DateTimeFormatter;
import java.util.List;

import diarsid.desktop.ui.components.calendar.api.Day;

public class DayInfoToString implements Day.Info.ToString {

    private static final String LINE_END = System.lineSeparator();
    public static final DayInfoToString DEFAULT = new DayInfoToString(
            DateTimeFormatter.ofPattern("d MMMM"),
            " - ",
            " - ");

    private final DateTimeFormatter format;
    private final String betweenDateAndHeader;
    private final String contentLineStart;

    public DayInfoToString(DateTimeFormatter format, String betweenDateAndHeader, String contentLineStart) {
        this.format = format;
        this.betweenDateAndHeader = betweenDateAndHeader;
        this.contentLineStart = contentLineStart;
    }

    @Override
    public String apply(Day.Info dayInfo) {
        StringBuilder builder = new StringBuilder();
        String date = this.format.format(dayInfo.date());
        builder.append(date);

        if ( dayInfo.header().isPresent() ) {
            builder.append(this.betweenDateAndHeader)
                    .append(dayInfo.header().get())
                    .append(LINE_END);
        }

        if ( dayInfo.content().isPresent() ) {
            List<String> content = dayInfo.content().get();
            int last = content.size() - 1;
            String line;
            for ( int i = 0; i < last; i++ ) {
                line = content.get(i);
                builder.append(this.contentLineStart).append(line).append(LINE_END);
            }
            builder.append(this.contentLineStart).append(content.get(last));

        }

        return builder.toString();
    }
}
