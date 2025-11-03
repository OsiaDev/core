package co.cetad.umas.core.infrastructure.redis.config;

import co.cetad.umas.core.domain.model.vo.TelemetryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    // Plain ObjectMapper for Redis (no default typing). Works with concrete serializers.
    @Bean
    @Qualifier("redisPlainObjectMapper")
    public ObjectMapper redisPlainObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Generic serializer for caches and generic values (no default typing)
    @Bean
    public GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer(@Qualifier("redisPlainObjectMapper") ObjectMapper mapper) {
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    // Typed serializer for TelemetryData values
    @Bean
    public Jackson2JsonRedisSerializer<TelemetryData> telemetryDataRedisSerializer(@Qualifier("redisPlainObjectMapper") ObjectMapper mapper) {
        // Prefer constructor accepting ObjectMapper if available to avoid deprecated setter
        try {
            return Jackson2JsonRedisSerializer.class
                    .getConstructor(ObjectMapper.class, Class.class)
                    .newInstance(mapper, TelemetryData.class);
        } catch (Exception reflectiveFallback) {
            // Fallback to non-arg constructor; acceptable if warning appears in older APIs
            Jackson2JsonRedisSerializer<TelemetryData> ser = new Jackson2JsonRedisSerializer<>(TelemetryData.class);
            // Avoid using deprecated setObjectMapper if possible; only use when necessary
            try {
                Jackson2JsonRedisSerializer.class.getMethod("setObjectMapper", ObjectMapper.class).invoke(ser, mapper);
            } catch (Exception ignored) {
                // If method not present, serializer will use its default mapper which may lack JavaTimeModule
            }
            return ser;
        }
    }

    // Generic RedisTemplate (if needed elsewhere)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

    // Telemetry-specific RedisTemplate using typed serializer
    @Bean
    @Qualifier("telemetryRedisTemplate")
    public RedisTemplate<String, TelemetryData> telemetryRedisTemplate(
            RedisConnectionFactory connectionFactory,
            Jackson2JsonRedisSerializer<TelemetryData> telemetryDataRedisSerializer) {
        RedisTemplate<String, TelemetryData> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(telemetryDataRedisSerializer);
        template.setHashValueSerializer(telemetryDataRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ZERO)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
