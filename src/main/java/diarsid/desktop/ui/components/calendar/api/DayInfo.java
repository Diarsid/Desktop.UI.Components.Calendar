package diarsid.desktop.ui.components.calendar.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import diarsid.support.objects.references.Possible;
import diarsid.support.objects.references.References;

public class DayInfo implements Day.Info {

    private final LocalDate day;
    private final Possible<String> header;
    private final Possible<List<String>> content;

    public DayInfo(LocalDate day, String header, List<String> content) {
        this.day = day;
        this.header = References.simplePossibleWith(header);
        this.content = References.simplePossibleWith(content);
    }

    public DayInfo(LocalDate day, List<String> content) {
        this.day = day;
        this.header = References.simplePossibleButEmpty();
        this.content = References.simplePossibleWith(content);
    }

    public DayInfo(LocalDate day, String header) {
        this.day = day;
        this.header = References.simplePossibleButEmpty();
        this.content = References.simplePossibleButEmpty();
    }

    @Override
    public LocalDate date() {
        return this.day;
    }

    @Override
    public Possible<String> header() {
        return this.header;
    }

    @Override
    public Possible<List<String>> content() {
        return this.content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DayInfo)) return false;
        DayInfo dayInfo = (DayInfo) o;
        return day.equals(dayInfo.day) &&
                header.equals(dayInfo.header) &&
                content.equals(dayInfo.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, header, content);
    }
}
