package org.example;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Custom Log4j Appender that stores log events in memory
 * Implements singleton pattern and supports dependency injection for the event list
 */
public class MemAppender extends AppenderSkeleton {

    private static MemAppender instance;
    private final List<LoggingEvent> events;
    private long discardedLogCount = 0;
    private int maxSize = 1000; // Default maximum size

    // Private constructor for singleton pattern
    private MemAppender(List<LoggingEvent> eventsList) {
        if (eventsList == null) {
            throw new IllegalArgumentException("Event list cannot be null");
        }
        this.events = eventsList;
    }

    /**
     * Get singleton instance with dependency injection for the event list
     */
    public static synchronized MemAppender getInstance(List<LoggingEvent> eventsList) {
        if (instance == null) {
            instance = new MemAppender(eventsList);
        }
        return instance;
    }

    /**
     * Get singleton instance with default ArrayList
     */
    public static synchronized MemAppender getInstance() {
        if (instance == null) {
            instance = new MemAppender(new ArrayList<>());
        }
        return instance;
    }

    /**
     * Reset the singleton instance (mainly for testing)
     */
    public static synchronized void resetInstance() {
        instance = null;
    }

    @Override
    protected void append(LoggingEvent event) {
        if (event == null) return;

        synchronized (events) {
            // Remove oldest event if max size is reached
            if (events.size() >= maxSize) {
                events.remove(0);
                discardedLogCount++;
            }
            events.add(event);
        }
    }

    /**
     * Returns an unmodifiable list of current LoggingEvents
     */
    public List<LoggingEvent> getCurrentLogs() {
        synchronized (events) {
            return Collections.unmodifiableList(new ArrayList<>(events));
        }
    }

    /**
     * Returns an unmodifiable list of formatted log strings using the layout
     */
    public List<String> getEventStrings() {
        if (layout == null) {
            throw new IllegalStateException("Layout is not set. Cannot format events without a layout.");
        }

        synchronized (events) {
            List<String> formattedEvents = new ArrayList<>(events.size());
            for (LoggingEvent event : events) {
                formattedEvents.add(layout.format(event));
            }
            return Collections.unmodifiableList(formattedEvents);
        }
    }

    /**
     * Prints all current logs to console using the layout and clears the memory
     */
    public void printLogs() {
        if (layout == null) {
            throw new IllegalStateException("Layout is not set. Cannot print events without a layout.");
        }

        synchronized (events) {
            for (LoggingEvent event : events) {
                System.out.println(layout.format(event));
            }
            events.clear();
        }
    }

    /**
     * Returns the number of logs that have been discarded due to maxSize limits
     */
    public long getDiscardedLogCount() {
        return discardedLogCount;
    }

    /**
     * Sets the maximum number of log events to store in memory
     */
    public void setMaxSize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be a positive integer");
        }

        synchronized (events) {
            this.maxSize = maxSize;
            // Remove excess events if current size exceeds new maxSize
            while (events.size() > maxSize) {
                events.remove(0);
                discardedLogCount++;
            }
        }
    }

    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Returns the current number of log events stored in memory
     */
    public int getCurrentSize() {
        synchronized (events) {
            return events.size();
        }
    }

    @Override
    public void close() {
        synchronized (events) {
            events.clear();
            discardedLogCount = 0;
        }
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    /**
     * Additional method to get estimated memory usage (for JMX monitoring)
     */
    public long getEstimatedMemoryUsage() {
        synchronized (events) {
            long estimatedSize = 0;
            for (LoggingEvent event : events) {
                String message = event.getRenderedMessage();
                if (message != null) {
                    estimatedSize += message.length() * 2L; // Rough estimate
                }
            }
            return estimatedSize;
        }
    }
}