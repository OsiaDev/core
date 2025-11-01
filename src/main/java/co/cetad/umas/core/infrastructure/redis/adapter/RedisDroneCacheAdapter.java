package co.cetad.umas.core.infrastructure.redis.adapter;

import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.ports.out.DroneCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisDroneCacheAdapter implements DroneCache {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${redis.keys.drone-prefix:umas:drone}")
    private String droneKeyPrefix;

    private String telemetryKey(String droneId) {
        return "%s:%s:telemetry".formatted(droneKeyPrefix, droneId);
    }

    @Override
    public Optional<TelemetryData> getTelemetry(String droneId) {
        try {
            Object v = redisTemplate.opsForValue().get(telemetryKey(droneId));
            if (v instanceof TelemetryData td) {
                return Optional.of(td);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Redis getTelemetry failed for {}", droneId, e);
            return Optional.empty();
        }
    }

    @Override
    public void setTelemetry(String droneId, TelemetryData telemetry) {
        try {
            redisTemplate.opsForValue().set(telemetryKey(droneId), telemetry);
        } catch (Exception e) {
            log.warn("Redis setTelemetry failed for {}", droneId, e);
        }
    }
}
