package de.cech12.bucketlib.gametest;

import de.cech12.bucketlib.api.BucketLib;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hides log messages that the tests provoke on purpose, so that the game test logs only show real problems.
 * <p>
 * BucketLib logs an error with stack trace every time it checks a fluid whose {@code getBucket()} throws (the test fluid
 * {@link TestFluids#THROWING_BUCKET_FLUID}). The creative tab tests check it for every bucket, which would fill the log
 * with dozens of stack traces. The hidden messages are counted, so tests can still check that BucketLib logs them.
 */
public final class ExpectedLogs {

    private static final String INCOMPATIBLE_FLUID_MESSAGE = "BucketLib is not compatible with this fluid";
    private static final AtomicInteger INCOMPATIBLE_FLUID_ERRORS = new AtomicInteger();
    private static Configuration installedIn;

    private ExpectedLogs() {}

    /**
     * Installs the filter in the log4j configuration of the BucketLib logger. The loaders replace the configuration
     * after the mods are loaded, so this is called before every test and installs the filter again if needed.
     */
    public static synchronized void install() {
        LoggerContext context = ((Logger) BucketLib.LOG).getContext();
        Configuration configuration = context.getConfiguration();
        if (configuration == installedIn) {
            return;
        }
        installedIn = configuration;
        Filter filter = new AbstractFilter(Filter.Result.DENY, Filter.Result.NEUTRAL) {
            // configuration filters are checked before the event is created, so all variants are needed
            @Override
            public Result filter(LogEvent event) {
                return check(event.getMessage().getFormattedMessage());
            }

            @Override
            public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
                return msg == null ? onMismatch : check(msg.getFormattedMessage());
            }

            @Override
            public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
                return check(String.valueOf(msg));
            }

            @Override
            public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
                return check(msg);
            }

            private Result check(String message) {
                if (message != null && message.contains(INCOMPATIBLE_FLUID_MESSAGE) && message.contains(TestFluids.THROWING_BUCKET_FLUID.id().toLanguageKey("fluid"))) {
                    INCOMPATIBLE_FLUID_ERRORS.incrementAndGet();
                    return onMatch;
                }
                return onMismatch;
            }
        };
        // the filter must be the first one: NeoForge starts with a filter that accepts all warnings and errors
        Filter existing = configuration.getFilter();
        if (existing != null) {
            configuration.removeFilter(existing);
        }
        configuration.addFilter(filter);
        if (existing != null) {
            configuration.addFilter(existing);
        }
        context.updateLoggers();
    }

    /** @return number of hidden "BucketLib is not compatible with this fluid" errors of the throwing test fluid */
    public static int incompatibleFluidErrors() {
        return INCOMPATIBLE_FLUID_ERRORS.get();
    }

}
