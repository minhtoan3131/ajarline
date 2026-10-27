package vn.io.ajarline.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A controllable Clock for tests.
 *
 * <p>It lets tests set the current time manually.
 * Clocks created with a different zone share the same current instant.</p>
 */
public final class MutableClock extends Clock {

    private final AtomicReference<Instant> current;
    private final ZoneId zone;

    private MutableClock(AtomicReference<Instant> current, ZoneId zone) {
        this.current = Objects.requireNonNull(current, "current");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    public MutableClock(Instant initial, ZoneId zone) {
        this(new AtomicReference<>(Objects.requireNonNull(initial, "initial")), zone);
    }

    public void set(Instant value) {
        current.set(Objects.requireNonNull(value, "value"));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(current, newZone);
    }

    @Override
    public Instant instant() {
        return current.get();
    }
}
