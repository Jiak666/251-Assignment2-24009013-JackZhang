package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

class VelocityLayoutTest {

    private VelocityLayout layout;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("TestLogger");
    }

    @Test
    void testBasicFormatting() {
        layout = new VelocityLayout("[$p] $c: $m");

        // 正确的 LoggingEvent 构造函数
        LoggingEvent event = new LoggingEvent(
                "TestLogger",
                logger,
                System.currentTimeMillis(),
                Level.INFO,
                "Test message content",
                null  // Throwable 参数
        );

        String result = layout.format(event);
        assertTrue(result.contains("[INFO]"));
        assertTrue(result.contains("TestLogger"));
        assertTrue(result.contains("Test message content"));
    }

    @Test
    void testAllVariables() {
        layout = new VelocityLayout("Logger: $c | Date: $d | Message: $m | Level: $p | Thread: $t");

        long currentTime = System.currentTimeMillis();

        // 正确的构造函数 - 线程名通过其他方式设置
        LoggingEvent event = new LoggingEvent(
                "com.example.TestClass",
                logger,
                currentTime,
                Level.DEBUG,
                "Debug message for testing",
                null  // Throwable 参数
        );

        String result = layout.format(event);
        assertTrue(result.contains("com.example.TestClass"));
        assertTrue(result.contains("Debug message for testing"));
        assertTrue(result.contains("DEBUG"));
        // Thread name will be the actual thread name, not "Thread-1"

        // Check date formatting (should contain the timestamp)
        assertTrue(result.contains(new Date(currentTime).toString()));
    }

    @Test
    void testLineSeparator() {
        layout = new VelocityLayout("$m$n");

        LoggingEvent event = new LoggingEvent(
                "TestLogger",
                logger,
                System.currentTimeMillis(),
                Level.INFO,
                "Message with newline",
                null
        );

        String result = layout.format(event);
        assertTrue(result.endsWith(System.lineSeparator()));
        assertTrue(result.contains("Message with newline"));
    }

    @Test
    void testCustomPattern() {
        String customPattern = "CUSTOM - $d {$p} $m for $c";
        layout = new VelocityLayout(customPattern);

        LoggingEvent event = new LoggingEvent(
                "CustomLogger",
                logger,
                System.currentTimeMillis(),
                Level.WARN,
                "Custom pattern test",
                null
        );

        String result = layout.format(event);
        assertTrue(result.startsWith("CUSTOM -"));
        assertTrue(result.contains("{WARN}"));
        assertTrue(result.contains("Custom pattern test"));
        assertTrue(result.contains("for CustomLogger"));
    }

    @Test
    void testPatternChange() {
        layout = new VelocityLayout("Initial: $m");

        LoggingEvent event1 = new LoggingEvent(
                "TestLogger",
                logger,
                System.currentTimeMillis(),
                Level.INFO,
                "First message",
                null
        );

        String result1 = layout.format(event1);
        assertTrue(result1.contains("Initial: First message"));

        // Change pattern
        layout.setPattern("Changed: $p - $m");

        LoggingEvent event2 = new LoggingEvent(
                "TestLogger",
                logger,
                System.currentTimeMillis(),
                Level.ERROR,
                "Second message",
                null
        );

        String result2 = layout.format(event2);
        assertTrue(result2.contains("Changed: ERROR - Second message"));
    }

    @Test
    void testEmptyMessage() {
        layout = new VelocityLayout("$p: $m");

        LoggingEvent event = new LoggingEvent(
                "TestLogger",
                logger,
                System.currentTimeMillis(),
                Level.INFO,
                "",  // Empty message
                null
        );

        String result = layout.format(event);
        assertEquals("INFO: ", result.trim());
    }

    @Test
    void testNullPatternThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new VelocityLayout(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            layout = new VelocityLayout();
            layout.setPattern(null);
        });
    }

    @Test
    void testSpecialCharactersInMessage() {
        layout = new VelocityLayout("$m");

        String specialMessage = "Message with \n newline and \t tab and \\ backslash";
        LoggingEvent event = new LoggingEvent(
                "TestLogger",
                logger,
                System.currentTimeMillis(),
                Level.INFO,
                specialMessage,
                null
        );

        String result = layout.format(event);
        assertEquals(specialMessage, result.trim());
    }

    @Test
    void testIntegrationWithMemAppender() {
        // Test that VelocityLayout works with MemAppender
        List<LoggingEvent> eventList = new ArrayList<LoggingEvent>();
        MemAppender appender = MemAppender.getInstance(eventList);
        layout = new VelocityLayout("INTEGRATION: $p - $m");
        appender.setLayout(layout);

        Logger testLogger = Logger.getLogger("IntegrationTest");
        testLogger.addAppender(appender);
        testLogger.info("Integration test message");

        List<String> eventStrings = appender.getEventStrings();
        assertEquals(1, eventStrings.size());
        assertTrue(eventStrings.get(0).contains("INTEGRATION: INFO - Integration test message"));

        appender.close();
    }

    // 测试线程名变量
    @Test
    void testThreadVariable() {
        layout = new VelocityLayout("Thread: $t");

        LoggingEvent event = new LoggingEvent(
                "ThreadTestLogger",
                logger,
                System.currentTimeMillis(),
                Level.INFO,
                "Thread test message",
                null
        );

        String result = layout.format(event);
        // 线程名会是实际的线程名，比如 "main" 或测试线程名
        assertTrue(result.startsWith("Thread: "));
    }
}