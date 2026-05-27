package com.example.interfaz.event;

import java.util.function.Consumer;

public interface EventPublisher {

    <T> void subscribe(Class<T> eventType, Consumer<T> listener);

    <T> void unsubscribe(Class<T> eventType, Consumer<T> listener);

    void publish(Object event);

    void clear();

    int getListenerCount(Class<?> eventType);
}
