package com.example.interfaz.event;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class EventBus implements EventPublisher {

    private static final Logger LOGGER = Logger.getLogger(EventBus.class.getName());
    private static EventBus instance;

    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Consumer<Object>>> listeners;

    private EventBus() {
        this.listeners = new ConcurrentHashMap<>();
    }

    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((Consumer<Object>) listener);
        LOGGER.fine("Listener suscrito para evento: " + eventType.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        CopyOnWriteArrayList<Consumer<Object>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove((Consumer<Object>) listener);
            LOGGER.fine("Listener desuscrito para evento: " + eventType.getSimpleName());
        }
    }

    public void publish(Object event) {
        Class<?> eventType = event.getClass();
        CopyOnWriteArrayList<Consumer<Object>> eventListeners = listeners.get(eventType);

        if (eventListeners != null && !eventListeners.isEmpty()) {
            LOGGER.fine("Publicando evento: " + eventType.getSimpleName() + " a " + eventListeners.size() + " listeners");

            for (Consumer<Object> listener : eventListeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    LOGGER.warning("Error procesando evento " + eventType.getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }

    public void clear() {
        listeners.clear();
        LOGGER.info("EventBus limpiado");
    }

    public int getListenerCount(Class<?> eventType) {
        CopyOnWriteArrayList<Consumer<Object>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
}
