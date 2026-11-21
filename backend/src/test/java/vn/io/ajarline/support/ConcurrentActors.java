package vn.io.ajarline.support;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Helpers for tests that run several actors against one database.
 */
public final class ConcurrentActors {

    private ConcurrentActors() {
    }

    /**
     * Holds the result or the failure of one actor.
     */
    public record Outcome(Object result, Throwable failure) {

        public boolean succeeded() {
            return failure == null;
        }
    }

    /**
     * Runs the work in its own transaction. The transaction stays open until the work returns.
     */
    public static Outcome inTransaction(PlatformTransactionManager transactionManager, Supplier<Object> work) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setTimeout(10);
        try {
            return new Outcome(transaction.execute(status -> work.get()), null);
        } catch (Throwable failure) {
            return new Outcome(null, failure);
        }
    }

    public static void awaitLatch(CountDownLatch latch, long timeoutSeconds, String message) {
        try {
            if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException(message);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrency test was interrupted.", exception);
        }
    }

    /**
     * Waits for a signal from an actor. When the actor already ended, its outcome explains why.
     */
    public static void awaitSignal(
            CountDownLatch latch, Future<Outcome> actor, String message) throws Exception {
        if (latch.await(5, TimeUnit.SECONDS)) {
            return;
        }
        if (actor.isDone()) {
            throw new IllegalStateException(message + " Actor outcome: " + actor.get());
        }
        throw new IllegalStateException(message);
    }

    public static void stop(ExecutorService executor, Future<?>... futures) {
        for (Future<?> future : futures) {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }

        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent actors did not stop before the timeout.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while stopping concurrent actors.", exception);
        }
    }
}
