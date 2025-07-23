package com.jouriroosjen.hardcoreSMPPlugin.events;

import com.jouriroosjen.hardcoreSMPPlugin.enums.EventStatusEnum;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for in-game live events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public abstract class BaseEvent {
    protected final JavaPlugin plugin;
    protected final String eventName;
    protected final Long duration;

    protected long startTime;

    protected EventStatusEnum status = EventStatusEnum.NOT_STARTED;

    protected final Set<UUID> participants = ConcurrentHashMap.newKeySet();

    /**
     * Constructs a new {@code BaseEvent} instance with duration.
     *
     * @param plugin    The main plugin instance.
     * @param eventName The name of the event.
     * @param duration  The duration of the event.
     */
    protected BaseEvent(JavaPlugin plugin, String eventName, Long duration) {
        this.plugin = plugin;
        this.eventName = eventName;
        this.duration = duration;
    }

    /**
     * Constructs a new {@code BaseEvent} instance with duration without duration.
     *
     * @param plugin    The main plugin instance.
     * @param eventName The name of the event.
     */
    protected BaseEvent(JavaPlugin plugin, String eventName) {
        this.plugin = plugin;
        this.eventName = eventName;
        this.duration = null;
    }

    /**
     * Start the event.
     */
    public final void start() {
        if (status != EventStatusEnum.NOT_STARTED)
            throw new IllegalStateException("Event has already started!");

        status = EventStatusEnum.STARTING;
        onEventStarting();

        status = EventStatusEnum.IN_PROGRESS;
        startTime = System.currentTimeMillis();
        onEventStart();

        if (duration != null)
            plugin.getServer().getScheduler().runTaskLater(plugin, this::end, duration / 50);
    }

    /**
     * End the event.
     */
    public final void end() {
        if (status != EventStatusEnum.IN_PROGRESS)
            throw new IllegalStateException("Event has already ended!");

        status = EventStatusEnum.ENDING;
        onEventEnding();

        status = EventStatusEnum.FINISHED;
        onEventFinished();

        cleanup();
    }

    /**
     * Add a participant to the event.
     *
     * @param uuid The UUID of the participant to add.
     * @return {@code true} if participant was added successfully, {@code false} otherwise.
     */
    public boolean addParticipant(UUID uuid) {
        if (status != EventStatusEnum.STARTING) return false;

        if (!participants.add(uuid)) return false;

        onPlayerJoin(uuid);

        return true;
    }

    /**
     * Remove a participant from the event.
     *
     * @param uuid The UUID of the participant to remove.
     * @return {@code true} if the Participant was removed successfully, {@code false} otherwise.
     */
    public boolean removeParticipant(UUID uuid) {
        if (!participants.remove(uuid)) return false;

        onPlayerLeave(uuid);

        return true;
    }

    /**
     * Logic to run when starting up an event.
     */
    protected abstract void onEventStarting();

    /**
     * Logic to run when an event starts.
     */
    protected abstract void onEventStart();

    /**
     * Logic to run when an event is ending.
     */
    protected abstract void onEventEnding();

    /**
     * Logic to run when an event is finished.
     */
    protected abstract void onEventFinished();

    /**
     * Logic to run when a participant is added to the event.
     *
     * @param uuid The UUID of the added participant.
     */
    protected abstract void onPlayerJoin(UUID uuid);

    /**
     * Logic to run when a participant is removed from the event.
     *
     * @param uuid The UUID of the removed participant.
     */
    protected abstract void onPlayerLeave(UUID uuid);

    /**
     * Cleanup task for when an event has been concluded.
     */
    protected void cleanup() {
        participants.clear();
    }

    /**
     * Get the event name.
     *
     * @return The event name.
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Get the event status.
     *
     * @return The event status.
     */
    public EventStatusEnum getStatus() {
        return status;
    }

    /**
     * Get all participants in the event.
     *
     * @return A set of all currently active participants UUID's.
     */
    public Set<UUID> getParticipants() {
        return Set.copyOf(participants);
    }

    /**
     * Get the current participant count.
     *
     * @return The amount of participants.
     */
    public int getParticipantCount() {
        return participants.size();
    }

    /**
     * Checks if the event has a set duration or not.
     *
     * @return {@code true} if the event has a set duration, {@code false} otherwise.
     */
    public boolean hasDuration() {
        return duration != null;
    }

    /**
     * Get the remaining event time.
     *
     * @return The remaining time in milliseconds.
     */
    public long getRemainingTime() {
        if (status != EventStatusEnum.IN_PROGRESS) return 0;
        return Math.max(0, duration - (System.currentTimeMillis() - startTime));
    }
}
