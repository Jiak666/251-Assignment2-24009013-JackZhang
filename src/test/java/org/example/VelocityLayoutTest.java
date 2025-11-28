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

        LoggingEvent event = new LoggingEvent(
                "TestLogger",
                logger,
                currentTime,
                Level.DEBUG,
                "Debug message for testing",
                null
        );

        String result = layout.format(event);

        System.out.println("Formatted result: " + result);

        assertTrue(result.contains("TestLogger"), "Should contain logger name");
        assertTrue(result.contains("Debug message for testing"), "Should contain message");
        assertTrue(result.contains("DEBUG"), "Should contain log level");
        assertTrue(result.contains("main"), "Should contain thread name");

        String dateString = new Date(currentTime).toString();
        assertTrue(result.contains(dateString), "Should contain date: " + dateString);
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
                "org.example.CustomLogger",  // FQCN 参数
                logger,                      // Logger 实例
                System.currentTimeMillis(),
                Level.WARN,
                "Custom pattern test",
                null
        );

        String result = layout.format(event);

        // 添加调试信息
        System.out.println("=== DEBUG testCustomPattern ===");
        System.out.println("Custom pattern: " + customPattern);
        System.out.println("Formatted result: '" + result + "'");
        System.out.println("Starts with CUSTOM - : " + result.startsWith("CUSTOM -"));
        System.out.println("Contains {WARN} : " + result.contains("{WARN}"));
        System.out.println("Contains Custom pattern test : " + result.contains("Custom pattern test"));
        System.out.println("Contains for CustomLogger : " + result.contains("for CustomLogger"));
        System.out.println("=============================");

        // 根据实际情况调整断言 - 现在检查 "for TestLogger"
        assertTrue(result.startsWith("CUSTOM -"), "Should start with 'CUSTOM -'");
        assertTrue(result.contains("{WARN}"), "Should contain '{WARN}'");
        assertTrue(result.contains("Custom pattern test"), "Should contain message");
        assertTrue(result.contains("for TestLogger"), "Should contain 'for TestLogger'");
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
        assertEquals("INFO:", result.trim());
        assertTrue(result.contains("INFO:"));
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