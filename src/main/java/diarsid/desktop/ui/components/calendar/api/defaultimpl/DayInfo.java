package diarsid.desktop.ui.components.calendar.api.defaultimpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import diarsid.desktop.ui.components.calendar.api.Day;
import diarsid.support.objects.references.Possible;
import diarsid.support.objects.references.References;

import static java.util.Arrays.asList;

public class DayInfo implements Day.Info {

    public static class Builder {

        private final LocalDate day;
        private String header;
        private List<String> content;
        private ToString toString;

        private Builder(LocalDate date) {
            this.day = date;
        }

        public static Builder ofDate(LocalDate date) {
            return new Builder(date);
        }

        public Builder withHeader(String header) {
            this.header = header;
            return this;
        }

        public Builder withContent(List<String> content) {
            if ( this.content == null ) {
                this.content = content;
            }
            else {
                this.content.addAll(content);
            }

            return this;
        }

        public Builder withContent(String... content) {
            return this.withContent(asList(content));
        }

        public Builder withToString(ToString toString) {
            this.toString = toString;
            return this;
        }

        public Day.Info build() {
            return new DayInfo(this);
        }

    }

    private final LocalDate day;
    private final Possible<String> header;
    private final Possible<List<String>> content;
    private final Possible<ToString> toString;

    private DayInfo(Builder builder) {
        this.day = builder.day;
        this.header = References.simplePossibleWith(builder.header);
        this.content = References.simplePossibleWith(builder.content);
        this.toString = References.simplePossibleWith(builder.toString);
    }

    public DayInfo(LocalDate day, String header, List<String> content) {
        this.day = day;
        this.header = References.simplePossibleWith(header);
        this.content = References.simplePossibleWith(content);
        this.toString = References.simplePossibleButEmpty();
    }

    public DayInfo(LocalDate day, String header, String... content) {
        this.day = day;
        this.header = References.simplePossibleWith(header);
        this.content = References.simplePossibleWith(asList(content));
        this.toString = References.simplePossibleButEmpty();
    }

    public DayInfo(LocalDate day, List<String> content) {
        this.day = day;
        this.header = References.simplePossibleButEmpty();
        this.content = References.simplePossibleWith(content);
        this.toString = References.simplePossibleButEmpty();
    }

    public DayInfo(LocalDate day, String header) {
        this.day = day;
        this.header = References.simplePossibleWith(header);
        this.content = References.simplePossibleButEmpty();
        this.toString = References.simplePossibleButEmpty();
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
    public Possible<ToString> customToString() {
        return this.toString;
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
