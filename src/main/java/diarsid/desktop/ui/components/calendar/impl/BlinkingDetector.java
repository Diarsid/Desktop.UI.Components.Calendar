package diarsid.desktop.ui.components.calendar.impl;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;

public class BlinkingDetector {

    static class Change {

        final long millis;
        final double value;

        Change(long millis, double value) {
            this.millis = millis;
            this.value = value;
        }
    }

    private final List<Change> changes;
    private final Runnable onBlinkingDetected;

    public BlinkingDetector(Runnable onBlinkingDetected) {
        this.changes = new ArrayList<>();
        this.onBlinkingDetected = onBlinkingDetected;
    }

    public void onChange(double value) {
        Change change = new Change(currentTimeMillis(), value);
        this.changes.add(change);

        if ( this.changes.size() >= 4 ) {
            int last = this.changes.size() - 1;
            Change c4 = this.changes.get(last);
            Change c3 = this.changes.get(last - 1);
            Change c2 = this.changes.get(last - 2);
            Change c1 = this.changes.get(last - 3);
            long millisDiff = c4.millis - c1.millis;
            System.out.println(millisDiff);

            boolean blinking =
                    c4.value == c2.value &&
                    c3.value == c1.value &&
                    millisDiff < 10;

            if ( blinking ) {
                this.onBlinkingDetected.run();
            }
            else {
                this.changes.clear();
                this.changes.add(change);
            }
        }
    }
}
