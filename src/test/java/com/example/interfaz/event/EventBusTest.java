package com.example.interfaz.event;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {

    @Test
    void shouldReceivePublishedEvent() {
        EventBus bus = new EventBus();
        AtomicInteger count = new AtomicInteger(0);

        bus.subscribe(DownloadEvent.QueueEmpty.class, e -> count.incrementAndGet());
        bus.publish(new DownloadEvent.QueueEmpty());

        assertEquals(1, count.get());
    }

    @Test
    void shouldNotReceiveAfterUnsubscribeAndCleanup() {
        EventBus bus = new EventBus();
        AtomicInteger count = new AtomicInteger(0);
        Consumer<DownloadEvent.QueueEmpty> listener = e -> count.incrementAndGet();

        bus.subscribe(DownloadEvent.QueueEmpty.class, listener);
        assertEquals(1, bus.getListenerCount(DownloadEvent.QueueEmpty.class));

        bus.unsubscribe(DownloadEvent.QueueEmpty.class, listener);
        assertEquals(0, bus.getListenerCount(DownloadEvent.QueueEmpty.class));

        bus.publish(new DownloadEvent.QueueEmpty());
        assertEquals(0, count.get());
    }

    @Test
    void shouldSupportPolymorphicEvents() {
        EventBus bus = new EventBus();
        AtomicInteger baseCount = new AtomicInteger(0);

        bus.subscribe(DownloadEvent.class, e -> baseCount.incrementAndGet());
        bus.publish(new DownloadEvent.QueueEmpty());

        assertEquals(1, baseCount.get(), "Listener de la superclase DownloadEvent debe recibir subtipo QueueEmpty");
    }

    @Test
    void shouldNotRegisterSameListenerTwice() {
        EventBus bus = new EventBus();
        AtomicInteger count = new AtomicInteger(0);
        Consumer<DownloadEvent.QueueEmpty> listener = e -> count.incrementAndGet();

        bus.subscribe(DownloadEvent.QueueEmpty.class, listener);
        bus.subscribe(DownloadEvent.QueueEmpty.class, listener);

        assertEquals(1, bus.getListenerCount(DownloadEvent.QueueEmpty.class));

        bus.publish(new DownloadEvent.QueueEmpty());
        assertEquals(1, count.get());
    }

    @Test
    void shouldValidateNullParameters() {
        EventBus bus = new EventBus();

        assertThrows(NullPointerException.class, () -> bus.subscribe(null, e -> {}));
        assertThrows(NullPointerException.class, () -> bus.subscribe(DownloadEvent.class, null));
        assertThrows(NullPointerException.class, () -> bus.publish(null));
    }
}
