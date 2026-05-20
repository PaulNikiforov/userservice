package com.innowise.userservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration for the UserService.
 *
 * <p>Configures cache regions with specific TTLs:
 * <ul>
 *   <li>users — user by ID, 15 min TTL</li>
 *   <li>paymentCards — card by ID, 10 min TTL</li>
 *   <li>userCards — cards list by user ID, 5 min TTL</li>
 * </ul>
 *
 * <p>All caches use JSON serialization with type-restricted deserialization
 * and disable null value caching.
 *
 * <p>{@code transactionAware()} defers all cache writes until after the transaction commits.
 * This means within a single {@code @Transactional} method, a second {@code @Cacheable} call
 * will still hit the database (cache is not yet populated).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.innowise.userservice")
                .allowIfSubType("java.util")
                .allowIfSubType("java.time")
                .allowIfSubType("java.lang")
                .build();

        ObjectMapper cacheMapper = objectMapper.copy();
        cacheMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        cacheMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(cacheMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("users", defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration("paymentCards", defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("userCards", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .transactionAware()
                .build();
    }
}
