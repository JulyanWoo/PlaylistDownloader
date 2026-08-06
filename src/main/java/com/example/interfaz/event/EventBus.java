package com.example.interfaz.event;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBus implements EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);

    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Consumer<Object>>> listeners;

    public EventBus() {
        this.listeners = new ConcurrentHashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        Objects.requireNonNull(eventType, "eventType no puede ser null");
        Objects.requireNonNull(listener, "listener no puede ser null");

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .addIfAbsent((Consumer<Object>) listener);
        LOGGER.debug("Listener suscrito para evento: {}", eventType.getSimpleName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        Objects.requireNonNull(eventType, "eventType no puede ser null");
        Objects.requireNonNull(listener, "listener no puede ser null");

        CopyOnWriteArrayList<Consumer<Object>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove((Consumer<Object>) listener);
            if (eventListeners.isEmpty()) {
                listeners.remove(eventType, eventListeners);
            }
            LOGGER.debug("Listener desuscrito para evento: {}", eventType.getSimpleName());
        }
    }

    @Override
    public void publish(Object event) {
        Objects.requireNonNull(event, "event no puede ser null");
        Class<?> actualClass = event.getClass();

        listeners.forEach((targetType, targetListeners) -> {
            if (targetType.isAssignableFrom(actualClass)) {
                for (Consumer<Object> listener : targetListeners) {
                    try {
                        listener.accept(event);
                    } catch (Exception e) {
                        LOGGER.error("Error procesando evento {} para listener de tipo {}", actualClass.getSimpleName(), targetType.getSimpleName(), e);
                    }
                }
            }
        });
    }
    @Override
    public void clear() {
        listeners.clear();
        LOGGER.info("EventBus limpiado");
    }

    @Override
    public int getListenerCount(Class<?> eventType) {
        if (eventType == null) return 0;
        CopyOnWriteArrayList<Consumer<Object>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
}
