package com.innowise.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /**
     * Application-wide clock, always UTC.
     *
     * <p>Inject {@link Clock} instead of calling {@link java.time.Instant#now()} directly
     * so that tests can provide a fixed clock and assert on exact timestamps.
     *
     * @return UTC system clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
