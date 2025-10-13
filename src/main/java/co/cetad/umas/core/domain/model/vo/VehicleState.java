package co.cetad.umas.core.domain.model.vo;

public enum VehicleState {
    IDLE,
    ARMED,
    FLYING,
    LANDING,
    ERROR;

    public boolean isOperational() {
        return this == ARMED || this == FLYING;
    }

    public boolean canExecuteCommand() {
        return this != ERROR;
    }

}