package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.io.File;

class StressTest {

    private static final int LOG_COUNT = 10000;
    private static final String STRESS_MESSAGE = "Stress test message for performance comparison ";

    @BeforeEach
    void cleanup() {
        // Clean up any previous test files
        File file = new File("stress_test.log");
        if (file.exists()) {
            file.delete();
        }
    }

    @AfterEach
    void tearDown() {
        MemAppender.resetInstance();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000, 10000})
    void testMemAppenderPerformanceWithArrayList(int maxSize) {
        // 修复：明确指定泛型类型
        List<LoggingEvent> eventList = new ArrayList<LoggingEvent>();
        MemAppender appender = MemAppender.getInstance(eventList);
        appender.setLayout(new VelocityLayout("[$p] $m"));
        appender.setMaxSize(maxSize);

        Logger logger = Logger.getLogger("ArrayListStressTest");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        // Performance measurement
        long startTime = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest GC before measurement
        long startMemory = runtime.totalMemory() - runtime.freeMemory();

        // Generate logs
        for (int i = 0; i < LOG_COUNT; i++) {
            logger.info(STRESS_MESSAGE + i);
        }

        long endTime = System.currentTimeMillis();
        long endMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = endMemory - startMemory;

        System.out.printf("ArrayList - MaxSize: %6d, Time: %5dms, Memory: %8d bytes, Discarded: %6d, FinalSize: %6d%n",
                maxSize, endTime - startTime, memoryUsed, appender.getDiscardedLogCount(), appender.getCurrentSize());

        appender.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000, 10000})
    void testMemAppenderPerformanceWithLinkedList(int maxSize) {
        // 修复：明确指定泛型类型
        List<LoggingEvent> eventList = new LinkedList<LoggingEvent>();
        MemAppender appender = MemAppender.getInstance(eventList);
        appender.setLayout(new VelocityLayout("[$p] $m"));
        appender.setMaxSize(maxSize);

        Logger logger = Logger.getLogger("LinkedListStressTest");
        logger.removeAllAppenders();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        // Performance measurement
        long startTime = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long startMemory = runtime.totalMemory() - runtime.freeMemory();

        // Generate logs
        for (int i = 0; i < LOG_COUNT; i++) {
            logger.info(STRESS_MESSAGE + i);
        }

        long endTime = System.currentTimeMillis();
        long endMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = endMemory - startMemory;

        System.out.printf("LinkedList - MaxSize: %6d, Time: %5dms, Memory: %8d bytes, Discarded: %6d, FinalSize: %6d%n",
                maxSize, endTime - startTime, memoryUsed, appender.getDiscardedLogCount(), appender.getCurrentSize());

        appender.close();
    }

    @Test
    void testConsoleAppenderPerformance() {
        Logger logger = Logger.getLogger("ConsoleAppenderTest");
        logger.removeAllAppenders();

        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setLayout(new PatternLayout("%-5p %c{1} - %m%n"));
        consoleAppender.activateOptions();
        logger.addAppender(consoleAppender);
        logger.setLevel(Level.INFO);

        long startTime = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long startMemory = runtime.totalMemory() - runtime.freeMemory();

        for (int i = 0; i < LOG_COUNT; i++) {
            logger.info(STRESS_MESSAGE + i);
        }

        long endTime = System.currentTimeMillis();
        long endMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = endMemory - startMemory;

        System.out.printf("ConsoleAppender - Time: %5dms, Memory: %8d bytes%n",
                endTime - startTime, memoryUsed);
    }

    @Test
    void testFileAppenderPerformance() throws Exception {
        Logger logger = Logger.getLogger("FileAppenderTest");
        logger.removeAllAppenders();

        FileAppender fileAppender = new FileAppender();
        fileAppender.setFile("stress_test.log");
        fileAppender.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1} - %m%n"));
        fileAppender.setAppend(false);
        fileAppender.activateOptions();
        logger.addAppender(fileAppender);
        logger.setLevel(Level.INFO);

        long startTime = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long startMemory = runtime.totalMemory() - runtime.freeMemory();

        for (int i = 0; i < LOG_COUNT; i++) {
            logger.info(STRESS_MESSAGE + i);
        }

        long endTime = System.currentTimeMillis();
        long endMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = endMemory - startMemory;

        System.out.printf("FileAppender - Time: %5dms, Memory: %8d bytes%n",
                endTime - startTime, memoryUsed);

        fileAppender.close();
    }

    @ParameterizedTest
    @CsvSource({
            "100, '[$p] $c: $m', '%-5p %c{1} - %m%n'",
            "1000, '$d $p $t - $m$n', '%d %-5p [%t] - %m%n'",
            "10000, 'SIMPLE: $m', '%m%n'"
    })
    void testLayoutPerformanceComparison(int maxSize, String velocityPattern, String patternLayoutPattern) {
        // Test VelocityLayout performance
        List<LoggingEvent> eventList = new ArrayList<LoggingEvent>();
        MemAppender velocityAppender = MemAppender.getInstance(eventList);
        VelocityLayout velocityLayout = new VelocityLayout(velocityPattern);
        velocityAppender.setLayout(velocityLayout);
        velocityAppender.setMaxSize(maxSize);

        Logger velocityLogger = Logger.getLogger("VelocityLayoutTest");
        velocityLogger.removeAllAppenders();
        velocityLogger.addAppender(velocityAppender);

        long velocityStart = System.currentTimeMillis();
        for (int i = 0; i < LOG_COUNT; i++) {
            velocityLogger.info(STRESS_MESSAGE + i);
        }
        long velocityTime = System.currentTimeMillis() - velocityStart;

        // Test PatternLayout performance
        List<LoggingEvent> eventList2 = new ArrayList<LoggingEvent>();
        MemAppender patternAppender = MemAppender.getInstance(eventList2);
        PatternLayout patternLayout = new PatternLayout(patternLayoutPattern);
        patternAppender.setLayout(patternLayout);
        patternAppender.setMaxSize(maxSize);

        Logger patternLogger = Logger.getLogger("PatternLayoutTest");
        patternLogger.removeAllAppenders();
        patternLogger.addAppender(patternAppender);

        long patternStart = System.currentTimeMillis();
        for (int i = 0; i < LOG_COUNT; i++) {
            patternLogger.info(STRESS_MESSAGE + i);
        }
        long patternTime = System.currentTimeMillis() - patternStart;

        System.out.printf("Layout Comparison - MaxSize: %d, Velocity: %dms, Pattern: %dms, Difference: %dms%n",
                maxSize, velocityTime, patternTime, velocityTime - patternTime);

        velocityAppender.close();
        patternAppender.close();
    }

    @Test
    void testMemoryUsageUnderLoad() {
        // Test memory behavior with different configurations
        int[] testSizes = {10, 100, 1000};

        for (int size : testSizes) {
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();

            List<LoggingEvent> eventList = new ArrayList<LoggingEvent>();
            MemAppender appender = MemAppender.getInstance(eventList);
            appender.setLayout(new VelocityLayout("$m"));
            appender.setMaxSize(size);

            Logger logger = Logger.getLogger("MemoryTest-" + size);
            logger.addAppender(appender);

            // Generate logs
            for (int i = 0; i < LOG_COUNT; i++) {
                logger.info("Memory test message " + i + " with some additional content to increase size");
            }

            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;

            System.out.printf("Memory Test - MaxSize: %d, Memory Increase: %d bytes, Discarded: %d%n",
                    size, memoryIncrease, appender.getDiscardedLogCount());

            appender.close();
        }
    }
}