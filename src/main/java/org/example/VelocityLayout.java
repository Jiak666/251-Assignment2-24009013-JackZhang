package org.example;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import java.io.StringWriter;
import java.util.Date;

/**
 * Custom Log4j Layout using Apache Velocity template engine
 * Supports variables: $c, $d, $m, $p, $t, $n
 */
public class VelocityLayout extends Layout {

    private String pattern;

    /**
     * Default constructor with a basic pattern
     */
    public VelocityLayout() {
        this("[$p] $c $d: $m$n");
    }

    /**
     * Constructor with custom pattern
     */
    public VelocityLayout(String pattern) {
        setPattern(pattern);
        initializeVelocity();
    }

    /**
     * Initialize Velocity template engine
     */
    private void initializeVelocity() {
        try {
            // Configure Velocity for simple string template usage
            Velocity.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
            Velocity.init();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Velocity template engine", e);
        }
    }

    @Override
    public String format(LoggingEvent event) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return event.getRenderedMessage();
        }

        VelocityContext context = createVelocityContext(event);
        StringWriter writer = new StringWriter();

        try {
            Velocity.evaluate(context, writer, "Log4jVelocityLayout", pattern);
            return writer.toString();
        } catch (Exception e) {
            // Fallback formatting if Velocity fails
            return createFallbackFormat(event);
        }
    }

    /**
     * Creates Velocity context with all supported variables
     */
    private VelocityContext createVelocityContext(LoggingEvent event) {
        VelocityContext context = new VelocityContext();

        // $c - logger name
        context.put("c", event.getLoggerName() != null ? event.getLoggerName() : "");

        // $d - date using default toString representation
        context.put("d", new Date(event.timeStamp).toString());

        // $m - message
        context.put("m", event.getRenderedMessage() != null ? event.getRenderedMessage() : "");

        // $p - log level (priority)
        context.put("p", event.getLevel() != null ? event.getLevel().toString() : "");

        // $t - thread name
        context.put("t", event.getThreadName() != null ? event.getThreadName() : "");

        // $n - line separator
        context.put("n", LINE_SEP);

        return context;
    }

    /**
     * Fallback formatting if Velocity evaluation fails
     */
    private String createFallbackFormat(LoggingEvent event) {
        StringBuilder fallback = new StringBuilder();

        if (event.getLevel() != null) {
            fallback.append("[").append(event.getLevel()).append("] ");
        }

        if (event.getLoggerName() != null) {
            fallback.append(event.getLoggerName()).append(" ");
        }

        fallback.append(new Date(event.timeStamp)).append(": ");

        if (event.getRenderedMessage() != null) {
            fallback.append(event.getRenderedMessage());
        }

        fallback.append(LINE_SEP);

        return fallback.toString();
    }

    /**
     * Sets the pattern template for formatting
     */
    public void setPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Pattern cannot be null or empty");
        }
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public boolean ignoresThrowable() {
        return true; // We're not handling throwables in this layout
    }

    @Override
    public void activateOptions() {
        // No additional options to activate
    }

    /**
     * Utility method to demonstrate pattern usage
     */
    public static String getSupportedVariables() {
        return "Supported variables:\n" +
                "  $c - Logger name\n" +
                "  $d - Date (default toString format)\n" +
                "  $m - Message\n" +
                "  $p - Log level/priority\n" +
                "  $t - Thread name\n" +
                "  $n - Line separator";
    }

    /**
     * Example patterns for common use cases
     */
    public static class ExamplePatterns {
        public static final String SIMPLE = "[$p] $m$n";
        public static final String DETAILED = "$d [$p] $c - $m$n";
        public static final String THREAD_AWARE = "$d [$p] $t - $c: $m$n";
        public static final String COMPACT = "$p: $m$n";
    }
}