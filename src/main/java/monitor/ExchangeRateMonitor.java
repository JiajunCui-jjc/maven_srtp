package monitor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Singleton monitor that polls CNY->GBP every 6 hours,
 * keeps a rolling history of the last 48 readings (12 days),
 * and logs console alerts when the rate moves significantly.
 */
public class ExchangeRateMonitor {

    // ---- configuration ------------------------------------------------
    /** How often to poll (hours). */
    public static final long INTERVAL_HOURS = 6;

    /** Alert if rate changes by more than this percentage between checks. */
    private static final double ALERT_THRESHOLD_PCT = 1.0;

    /** Maximum history entries kept in memory. */
    private static final int MAX_HISTORY = 48;
    // -------------------------------------------------------------------

    public static class RateEntry {
        public final double rate;
        public final Date timestamp;
        public final String alert;   // null = no alert

        RateEntry(double rate, Date timestamp, String alert) {
            this.rate = rate;
            this.timestamp = timestamp;
            this.alert = alert;
        }

        public String getFormattedTime() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
        }
    }

    // ---- singleton ----------------------------------------------------
    private static final ExchangeRateMonitor INSTANCE = new ExchangeRateMonitor();

    public static ExchangeRateMonitor getInstance() {
        return INSTANCE;
    }

    private ExchangeRateMonitor() {}
    // -------------------------------------------------------------------

    private final List<RateEntry> history =
            Collections.synchronizedList(new ArrayList<RateEntry>());

    private ScheduledExecutorService scheduler;
    private volatile long nextCheckEpoch = -1;

    /** Start the 6-hour scheduler (called once on app startup). */
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // First check immediately, then every INTERVAL_HOURS
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                checkAndRecord();
            }
        }, 0, INTERVAL_HOURS, TimeUnit.HOURS);

        System.out.println("[RateMonitor] Started – polling every "
                + INTERVAL_HOURS + " hours.");
    }

    /** Stop the scheduler (called on app shutdown). */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            System.out.println("[RateMonitor] Stopped.");
        }
    }

    /** Fetch rate, store in history, log alert if needed. */
    private void checkAndRecord() {
        nextCheckEpoch = System.currentTimeMillis()
                + TimeUnit.HOURS.toMillis(INTERVAL_HOURS);

        double rate = ExchangeRateService.fetchCnyToGbp();
        if (rate <= 0) {
            System.err.println("[RateMonitor] Fetch failed – skipping.");
            return;
        }

        String alert = buildAlert(rate);
        RateEntry entry = new RateEntry(rate, new Date(), alert);

        synchronized (history) {
            history.add(entry);
            if (history.size() > MAX_HISTORY) {
                history.remove(0);
            }
        }

        // Console reminder
        String timestamp = entry.getFormattedTime();
        System.out.println("[RateMonitor] " + timestamp
                + "  1 CNY = " + String.format("%.6f", rate) + " GBP");
        if (alert != null) {
            System.out.println("[RateMonitor] *** ALERT *** " + alert);
        }
    }

    /**
     * Compares the new rate to the previous one and returns an alert message
     * if the change exceeds ALERT_THRESHOLD_PCT, otherwise null.
     */
    private String buildAlert(double newRate) {
        if (history.isEmpty()) return null;
        double prev = history.get(history.size() - 1).rate;
        double changePct = (newRate - prev) / prev * 100.0;
        if (Math.abs(changePct) >= ALERT_THRESHOLD_PCT) {
            return String.format("Rate changed %.2f%% (%.6f -> %.6f)",
                    changePct, prev, newRate);
        }
        return null;
    }

    // ---- public accessors for the servlet/JSP -------------------------

    /** Latest rate entry, or null if no data yet. */
    public RateEntry getLatest() {
        synchronized (history) {
            if (history.isEmpty()) return null;
            return history.get(history.size() - 1);
        }
    }

    /** Snapshot of the full history (oldest first). */
    public List<RateEntry> getHistory() {
        synchronized (history) {
            return new ArrayList<RateEntry>(history);
        }
    }

    /**
     * Milliseconds until the next scheduled check,
     * or -1 if not started yet.
     */
    public long getMillisUntilNext() {
        if (nextCheckEpoch < 0) return -1;
        return Math.max(0, nextCheckEpoch - System.currentTimeMillis());
    }
}
