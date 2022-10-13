package diarsid.desktop.ui.components.calendar.impl;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import diarsid.support.concurrency.threads.IncrementThreadsNaming;
import diarsid.support.concurrency.threads.NamedThreadFactory;
import diarsid.support.concurrency.threads.ThreadsNaming;

import static java.time.Duration.between;
import static java.time.LocalDateTime.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static diarsid.support.concurrency.threads.ThreadsUtil.shutdownAndWait;

public class MidnightTimer {

    private final ScheduledExecutorService async;
    private final Runnable action;
    private final ScheduledFuture<?> schedule;

    public MidnightTimer(String threadName, Runnable action) {
        ThreadsNaming naming = new IncrementThreadsNaming(threadName + ".%s");
        NamedThreadFactory ntf = new NamedThreadFactory(naming, Executors.defaultThreadFactory());
        this.async = Executors.newSingleThreadScheduledExecutor(ntf);
        this.action = action;

        LocalDateTime now = now();
        LocalDateTime startOfNextDay = now.plusDays(1).withHour(0).withMinute(0).withSecond(1).withNano(0);
        long initialDelay = (between(now, startOfNextDay).getSeconds() * 1000) - 100;
        long period = 24 * 60 * 60 * 1000;
        this.schedule = this.async.scheduleAtFixedRate(this.action, initialDelay, period, MILLISECONDS);
    }

    public void stop() {
        this.schedule.cancel(true);
        shutdownAndWait(this.async);
    }

}
