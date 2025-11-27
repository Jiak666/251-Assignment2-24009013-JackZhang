package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

class MemAppenderTest {

    private MemAppender appender;
    private List<LoggingEvent> eventList;
    private Logger logger;

    @BeforeEach
    void setUp() {
        eventList = new ArrayList<>();
        appender = MemAppender.getInstance(eventList);
        appender.setLayout(new VelocityLayout("[TEST] $m"));
        appender.setMaxSize(3);

        logger = Logger.getLogger("TestLogger");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.ALL);
    }

    @AfterEach
    void tearDown() {
        if (appender != null) {
            appender.close();
        }
        MemAppender.resetInstance(); // 添加重置方法到MemAppender
    }

    @Test
    void testSingletonPattern() {
        MemAppender instance1 = MemAppender.getInstance(new ArrayList<>());
        MemAppender instance2 = MemAppender.getInstance(new ArrayList<>());
        assertSame(instance1, instance2, "Should return the same singleton instance");
    }

    @Test
    void testLogAppending() {
        logger.info("Test message 1");
        logger.warn("Test message 2");

        assertEquals(2, appender.getCurrentSize(), "Should have 2 log events");
        assertEquals(0, appender.getDiscardedLogCount(), "No logs should be discarded");
    }

    @Test
    void testMaxSizeEnforcement() {
        // Add more logs than maxSize
        for (int i = 0; i < 5; i++) {
            logger.info("Message " + i);
        }

        assertEquals(3, appender.getCurrentSize(), "Should only keep maxSize logs");
        assertEquals(2, appender.getDiscardedLogCount(), "Should have discarded 2 logs");
    }

    @Test
    void testGetCurrentLogs() {
        logger.info("First message");
        logger.error("Second message");

        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertEquals(2, logs.size(), "Should return all current logs");
        assertEquals("First message", logs.get(0).getRenderedMessage());
        assertEquals("Second message", logs.get(1).getRenderedMessage());

        // Test that returned list is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            logs.add(new LoggingEvent(null, logger, 0, Level.INFO, "Test", null));
        });
    }

    @Test
    void testGetEventStrings() {
        appender.setLayout(new VelocityLayout("$m"));
        logger.info("Test message");
        logger.warn("Warning message");

        List<String> eventStrings = appender.getEventStrings();
        assertEquals(2, eventStrings.size());
        assertTrue(eventStrings.get(0).contains("Test message"));
        assertTrue(eventStrings.get(1).contains("Warning message"));
    }

    @Test
    void testGetEventStringsWithoutLayout() {
        // Remove layout to test exception
        appender.setLayout(null);
        logger.info("Test message");

        assertThrows(IllegalStateException.class, () -> {
            appender.getEventStrings();
        });
    }

    @Test
    void testPrintLogs() {
        appender.setLayout(new VelocityLayout("PRINT: $m"));
        logger.info("Message to print");
        logger.error("Error to print");

        // This should print to console and clear logs
        appender.printLogs();

        assertEquals(0, appender.getCurrentSize(), "Logs should be cleared after printing");
    }

    @Test
    void testPrintLogsWithoutLayout() {
        appender.setLayout(null);
        logger.info("Test message");

        assertThrows(IllegalStateException.class, () -> {
            appender.printLogs();
        });
    }

    @Test
    void testDiscardedLogCount() {
        appender.setMaxSize(2);

        logger.info("Message 1");
        logger.info("Message 2");
        logger.info("Message 3"); // This should discard Message 1

        assertEquals(1, appender.getDiscardedLogCount());

        logger.info("Message 4"); // This should discard Message 2
        assertEquals(2, appender.getDiscardedLogCount());
    }

    @Test
    void testMaxSizeReduction() {
        // First add some logs
        for (int i = 0; i < 5; i++) {
            logger.info("Message " + i);
        }

        // Reduce maxSize - should discard excess logs
        appender.setMaxSize(2);

        assertEquals(2, appender.getCurrentSize());
        assertTrue(appender.getDiscardedLogCount() >= 3);
    }

    @Test
    void testDifferentLogLevels() {
        logger.debug("Debug message");
        logger.info("Info message");
        logger.warn("Warning message");
        logger.error("Error message");
        logger.fatal("Fatal message");

        assertEquals(5, appender.getCurrentSize());

        List<LoggingEvent> logs = appender.getCurrentLogs();
        assertEquals(Level.DEBUG, logs.get(0).getLevel());
        assertEquals(Level.INFO, logs.get(1).getLevel());
        assertEquals(Level.WARN, logs.get(2).getLevel());
        assertEquals(Level.ERROR, logs.get(3).getLevel());
        assertEquals(Level.FATAL, logs.get(4).getLevel());
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int logsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < logsPerThread; j++) {
                    logger.info("Thread " + threadId + " - Message " + j);
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify no data corruption and correct counts
        assertTrue(appender.getCurrentSize() <= appender.getMaxSize());
        assertTrue(appender.getDiscardedLogCount() >= 0);

        int totalLogs = threadCount * logsPerThread;
        int expectedDiscarded = Math.max(0, totalLogs - appender.getMaxSize());
        assertEquals(expectedDiscarded, appender.getDiscardedLogCount());
    }
}