package vn.io.ajarline.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Waits until PostgreSQL reports that a backend is waiting for a lock.
 *
 * <p>This replaces a sleep. A competing actor has reached the conflict point
 * only when the database says it is blocked.</p>
 */
public final class PostgreSqlLockWait {

    private PostgreSqlLockWait() {
    }

    public static int backendPid(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class);
    }

    /**
     * @throws IllegalStateException when the backend does not wait for a lock within five seconds
     */
    public static void await(JdbcTemplate jdbcTemplate, int backendPid) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline) {
            Boolean waitingForLock = jdbcTemplate.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1
                        FROM pg_stat_activity
                        WHERE pid = ?
                          AND wait_event_type = 'Lock'
                    )
                    """, Boolean.class, backendPid);

            if (Boolean.TRUE.equals(waitingForLock)) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }

        throw new IllegalStateException("The backend did not wait for a database lock.");
    }
}
