package vn.io.ajarline.publication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import vn.io.ajarline.content.contract.ArticleContentInput;
import vn.io.ajarline.content.contract.ArticleCreator;
import vn.io.ajarline.content.contract.ArticleReader;
import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.ContentPublicationAccess;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.publication.contract.ContentPublisher;
import vn.io.ajarline.publication.contract.PublicationResult;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class PublicationConcurrencyIT {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Autowired private ArticleCreator creator;
    @Autowired private ArticleReader reader;
    @Autowired private ContentPublisher publisher;
    @Autowired private JdbcTemplate jdbc;

    @MockitoSpyBean
    private ContentPublicationAccess publicationAccess;

    @BeforeEach
    void clearDatabase() {
        jdbc.update("DELETE FROM content_item");
    }

    @Test
    void publish_ShouldAllowOneWriter_WhenTwoWritersUseSameRevision()
            throws Exception {
        ArticleView draft = creator.create(new ArticleContentInput(
                "Concurrent publication",
                "concurrent-publication",
                "Summary",
                "# Body"));
        CyclicBarrier beforeLock = new CyclicBarrier(2);

        ContentPublicationAccess spy =
                AopTestUtils.getUltimateTargetObject(publicationAccess);

        doAnswer(invocation -> {
            await(beforeLock);
            return invocation.callRealMethod();
        }).when(spy).lock(draft.id(), 1);

        List<ActorOutcome<PublicationResult>> outcomes = runTogether(List.of(
                () -> publisher.publish(draft.id(), 1),
                () -> publisher.publish(draft.id(), 1)));

        assertThat(outcomes.stream().filter(ActorOutcome::succeeded))
                .hasSize(1);
        assertThat(outcomes.stream()
                .map(ActorOutcome::failure)
                .filter(ContentOperationException.class::isInstance)
                .map(ContentOperationException.class::cast)
                .map(ContentOperationException::code))
                .containsExactly(ContentFailureCode.REVISION_CONFLICT);

        ArticleView stored = reader.get(draft.id());
        assertThat(stored.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(stored.revision()).isEqualTo(2);
        assertThat(stored.publishedAt()).isNotNull();
    }

    private static void await(CyclicBarrier barrier) throws Exception {
        barrier.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    private static <T> List<ActorOutcome<T>> runTogether(
            List<Callable<T>> actions
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(actions.size());
        CountDownLatch ready = new CountDownLatch(actions.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ActorOutcome<T>>> futures = new ArrayList<>();

        try {
            for (Callable<T> action : actions) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(
                            TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                        throw new IllegalStateException(
                                "Timed out while waiting for start gate.");
                    }
                    return ActorOutcome.capture(action);
                }));
            }
            assertThat(ready.await(
                    TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
            start.countDown();

            List<ActorOutcome<T>> outcomes = new ArrayList<>();
            for (Future<ActorOutcome<T>> future : futures) {
                outcomes.add(future.get(
                        TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
            }
            return outcomes;
        } finally {
            futures.stream()
                    .filter(future -> !future.isDone())
                    .forEach(future -> future.cancel(true));
            executor.shutdownNow();
            assertThat(executor.awaitTermination(
                    TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
        }
    }

    private record ActorOutcome<T>(T result, Throwable failure) {

        static <T> ActorOutcome<T> capture(Callable<T> action) {
            try {
                return new ActorOutcome<>(action.call(), null);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return new ActorOutcome<>(null, exception);
            } catch (Throwable failure) {
                return new ActorOutcome<>(null, failure);
            }
        }

        boolean succeeded() {
            return failure == null;
        }
    }
}
