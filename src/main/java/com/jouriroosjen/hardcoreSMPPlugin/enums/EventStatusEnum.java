package com.jouriroosjen.hardcoreSMPPlugin.enums;

public enum EventStatusEnum {
    NOT_STARTED(1),
    STARTING(2),
    IN_PROGRESS(3),
    ENDING(4),
    FINISHED(5);

    private final int id;

    /**
     * Constructs a new {@code EventStatusEnum} instance.
     *
     * @param id The ID of the event status.
     */
    EventStatusEnum(int id) {
        this.id = id;
    }

    /**
     * Get the corresponding ID of a value.
     *
     * @return The ID of this value.
     */
    public int getId() {
        return id;
    }
}
