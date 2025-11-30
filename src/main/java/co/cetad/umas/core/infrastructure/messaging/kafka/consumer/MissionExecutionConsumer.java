package co.cetad.umas.core.infrastructure.messaging.kafka.consumer;

import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.domain.model.dto.MissionExecutionDTO;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer de Kafka para comandos de ejecución de misión
 * Recibe MissionExecutionCommand desde el servicio de operaciones
 * y orquesta la creación/selección de ruta y ejecución completa
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionExecutionConsumer {

    private final EventProcessor<MissionExecutionDTO, CommandResultDTO> missionExecutionService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @KafkaListener(
            topics = "${kafka.topics.mission:umas.drone.mission}",
            groupId = "${spring.kafka.consumer.group-id:ugcs-core-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMissionExecution(
            @Payload String message,
            Acknowledgment acknowledgment
    ) {
        try {
            var mission = objectMapper.readValue(message, MissionExecutionDTO.class);

            log.info("📥 Received mission execution command: mission={}, vehicle={}",
                    mission.missionId(),
                    mission.drones());

            log.debug("Mission details: {}", mission);

            missionExecutionService.process(mission)
                    .thenAccept(result -> {
                        log.info("✅ Mission execution completed - Mission: {}, Vehicle: {}, Status: {}, Message: {}",
                                mission.missionId(),
                                result.vehicleId(),
                                result.status(),
                                result.message());
                        acknowledgment.acknowledge();
                    })
                    .exceptionally(error -> {
                        log.error("❌ Failed to execute mission: {}",
                                mission.missionId(), error);
                        acknowledgment.acknowledge();  // Acknowledge para no reintentarlo indefinidamente
                        return null;
                    });

        } catch (Exception e) {
            log.error("❌ Failed to parse mission execution message: {}", message, e);
            acknowledgment.acknowledge();
        }
    }
}