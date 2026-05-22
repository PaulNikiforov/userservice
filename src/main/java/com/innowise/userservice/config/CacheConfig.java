package com.innowise.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;
import com.innowise.userservice.model.dto.UserResponseDTO;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/** Redis cache configuration with typed JSON serialization per cache. */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        ObjectMapper cacheMapper = objectMapper.copy()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration("users",
                        base.serializeValuesWith(typedSerializer(cacheMapper, UserResponseDTO.class)))
                .withCacheConfiguration("paymentCards",
                        base.serializeValuesWith(typedSerializer(cacheMapper, PaymentCardResponseDTO.class)))
                .withCacheConfiguration("userCards",
                        base.serializeValuesWith(listSerializer(cacheMapper, PaymentCardResponseDTO.class)))
                .transactionAware()
                .build();
    }

    private <T> RedisSerializationContext.SerializationPair<T> typedSerializer(
            ObjectMapper mapper, Class<T> type) {
        return RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, type));
    }

    private <T> RedisSerializationContext.SerializationPair<List<T>> listSerializer(
            ObjectMapper mapper, Class<T> elementType) {
        var javaType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, javaType));
    }
}
