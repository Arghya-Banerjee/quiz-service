package com.thinkforge.quiz_service.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LogTestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger("org.mongodb.driver.connection");

    @Override
    public void run(String... args) {
        logger.info("✅ This is a test log to check MongoDB appender.");
    }
}
