package com.example.interfaz.event;

import java.util.function.Consumer;

/**
 * Interfaz para publicación de eventos que define el contrato
 * para diferentes implementaciones de sistemas de eventos.
 */
public interface EventPublisher {
    
    /**
     * Suscribe un listener a un tipo de evento
     * @param eventType tipo de evento
     * @param listener función que maneja el evento
     * @param <T> tipo del evento
     */
    <T> void subscribe(Class<T> eventType, Consumer<T> listener);
    
    /**
     * Desuscribe un listener de un tipo de evento
     * @param eventType tipo de evento
     * @param listener función que maneja el evento
     * @param <T> tipo del evento
     */
    <T> void unsubscribe(Class<T> eventType, Consumer<T> listener);
    
    /**
     * Publica un evento a todos los listeners suscritos
     * @param event evento a publicar
     */
    void publish(Object event);
    
    /**
     * Limpia todos los listeners (útil para testing)
     */
    void clear();
    
    /**
     * Obtiene el número de listeners para un tipo de evento
     * @param eventType tipo de evento
     * @return número de listeners
     */
    int getListenerCount(Class<?> eventType);
}