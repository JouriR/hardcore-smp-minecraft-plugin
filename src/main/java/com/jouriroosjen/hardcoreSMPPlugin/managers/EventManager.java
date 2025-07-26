package com.jouriroosjen.hardcoreSMPPlugin.managers;

import com.jouriroosjen.hardcoreSMPPlugin.events.BaseEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages in-game live events.
 *
 * @author Jouri Roosjen
 * @version 1.0.0
 */
public class EventManager {
    private final JavaPlugin plugin;

    private BaseEvent currentEvent;

    /**
     * Construct a new {@code EventManager} instance.
     *
     * @param plugin The main plugin instance.
     */
    public EventManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the given event.
     *
     * @param event The event to start.
     * @return {@code true} if the event got started successfully, {@code false} otherwise.
     */
    public boolean startEvent(BaseEvent event) {
        if (currentEvent != null) return false;

        currentEvent = event;
        event.start();

        return true;
    }

    /**
     * Get the current event.
     *
     * @return The currently ongoing event.
     */
    public BaseEvent getCurrentEvent() {
        return currentEvent;
    }

    /**
     * Checks if there is an active event.
     *
     * @return {@code true} if there is an active event, {@code false} otherwise.
     */
    public boolean hasActiveEvent() {
        return currentEvent != null;
    }

    /**
     * Stop the currently active event.
     *
     * @return {@code true} if the event got stopped successfully, {@code false} otherwise.
     */
    public boolean stopEvent() {
        if (currentEvent == null) return false;

        currentEvent.end();
        currentEvent = null;

        return true;
    }
}
