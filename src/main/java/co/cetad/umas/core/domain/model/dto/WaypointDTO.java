package co.cetad.umas.core.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Value Object que representa un waypoint (punto de ruta)
 * Contiene coordenadas de latitud y longitud
 */
public record WaypointDTO(
        @JsonProperty("latitude") double latitude,
        @JsonProperty("longitude") double longitude
) {

    public WaypointDTO {
        Objects.requireNonNull(latitude, "Latitude cannot be null");
        Objects.requireNonNull(longitude, "Longitude cannot be null");

        // Validaciones de rango
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    /**
     * Factory method para crear un waypoint desde un array de coordenadas [longitude, latitude]
     * GeoJSON usa el orden [longitude, latitude]
     */
    public static WaypointDTO fromGeoJsonCoordinates(double[] coordinates) {
        if (coordinates == null || coordinates.length < 2) {
            throw new IllegalArgumentException("Coordinates array must have at least 2 elements");
        }
        // GeoJSON: [longitude, latitude]
        // Waypoint: (latitude, longitude)
        return new WaypointDTO(coordinates[1], coordinates[0]);
    }

}