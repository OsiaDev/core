package co.cetad.umas.core.infrastructure.redis.adapter;

import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.ports.out.DroneCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisDroneCacheAdapter implements DroneCache {

    @Qualifier("telemetryRedisTemplate")
    private final RedisTemplate<String, TelemetryData> telemetryRedisTemplate;
    // Fallback generic template for legacy values
    private final RedisTemplate<String, Object> redisTemplate;
    // Mapper without default typing for Map->TelemetryData conversion
    @Qualifier("redisPlainObjectMapper")
    private final ObjectMapper redisPlainObjectMapper;

    @Value("${redis.keys.drone-prefix:umas:drone}")
    private String droneKeyPrefix;

    private String telemetryKey(String droneId) {
        return "%s:%s:telemetry".formatted(droneKeyPrefix, droneId);
    }

    @Override
    public Optional<TelemetryData> getTelemetry(String droneId) {
        String key = telemetryKey(droneId);
        try {
            TelemetryData v = telemetryRedisTemplate.opsForValue().get(key);
            if (v != null) return Optional.of(v);
        } catch (Exception e) {
            // If typed deserialization fails (e.g., legacy JSON), try to read with generic serializer
            log.debug("Typed telemetry read failed for {}. Will attempt legacy read.", key, e);
        }
        // Legacy fallback path: try generic read and convert
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw instanceof TelemetryData td) return Optional.of(td);
            if (raw instanceof Map) {
                try {
                    TelemetryData converted = redisPlainObjectMapper.convertValue(raw, TelemetryData.class);
                    if (converted != null) {
                        // Write back normalized typed value to prevent future fallbacks
                        telemetryRedisTemplate.opsForValue().set(key, converted);
                        log.info("Upgraded legacy telemetry at key {} to typed format.", key);
                        return Optional.of(converted);
                    }
                } catch (IllegalArgumentException ignore) {
                    // conversion failed
                }
            }
        } catch (Exception ex) {
            log.warn("Legacy telemetry read/convert failed for {}", key, ex);
        }
        return Optional.empty();
    }

    @Override
    public void setTelemetry(String droneId, TelemetryData telemetry) {
        try {
            telemetryRedisTemplate.opsForValue().set(telemetryKey(droneId), telemetry);
        } catch (Exception e) {
            log.warn("Redis setTelemetry failed for {}", droneId, e);
        }
    }
}
