package com.strataguard.api.config;

import com.strataguard.core.config.EstateContext;
import com.strataguard.core.config.TenantContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("(5) ThreadLocal cleanup — concurrent request isolation")
class EstateContextThreadSafetyTest {

    private static final int THREAD_COUNT = 20;
    private static final int ITERATIONS_PER_THREAD = 50;

    @Test
    @DisplayName("concurrent threads never see another thread's EstateContext")
    void noContextLeakageBetweenThreads() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicInteger failures = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadIndex = t;
            final UUID threadEstateId = UUID.randomUUID();
            final UUID threadTenantId = UUID.randomUUID();
            final String threadUserId = "user-" + threadIndex;
            final String threadRole = (threadIndex % 2 == 0) ? "ESTATE_ADMIN" : "SECURITY_GUARD";
            final Set<String> threadPerms = Set.of("perm." + threadIndex + ".a", "perm." + threadIndex + ".b");

            executor.submit(() -> {
                try {
                    // Synchronize start
                    barrier.await(5, TimeUnit.SECONDS);

                    for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                        // Simulate filter: set context
                        TenantContext.setTenantId(threadTenantId);
                        EstateContext.setEstateId(threadEstateId);
                        EstateContext.setUserId(threadUserId);
                        EstateContext.setRole(threadRole);
                        EstateContext.setPermissions(threadPerms);

                        // Simulate request processing — brief delay
                        Thread.yield();

                        // Verify MY context is still MY context (no leakage)
                        if (!threadEstateId.equals(EstateContext.getEstateId())) {
                            failures.incrementAndGet();
                        }
                        if (!threadUserId.equals(EstateContext.getUserId())) {
                            failures.incrementAndGet();
                        }
                        if (!threadRole.equals(EstateContext.getRole())) {
                            failures.incrementAndGet();
                        }
                        if (!threadPerms.equals(EstateContext.getPermissions())) {
                            failures.incrementAndGet();
                        }
                        if (!threadTenantId.equals(TenantContext.getTenantId())) {
                            failures.incrementAndGet();
                        }

                        // Simulate filter cleanup (finally block)
                        EstateContext.clear();
                        TenantContext.clear();

                        // After cleanup, everything must be null/empty
                        if (EstateContext.getEstateId() != null) {
                            failures.incrementAndGet();
                        }
                        if (EstateContext.getUserId() != null) {
                            failures.incrementAndGet();
                        }
                        if (!EstateContext.getPermissions().isEmpty()) {
                            failures.incrementAndGet();
                        }
                        if (TenantContext.getTenantId() != null) {
                            failures.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    EstateContext.clear();
                    TenantContext.clear();
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("All threads should complete within timeout").isTrue();
        assertThat(failures.get()).as("No context leakage detected across %d threads x %d iterations",
            THREAD_COUNT, ITERATIONS_PER_THREAD).isZero();
    }

    @Test
    @DisplayName("cleared ThreadLocal does not resurrect stale data on reuse")
    void noStaleDataOnThreadReuse() throws Exception {
        // Use a single-thread executor to guarantee thread reuse
        ExecutorService executor = Executors.newSingleThreadExecutor();

        UUID estate1 = UUID.randomUUID();
        UUID estate2 = UUID.randomUUID();

        // Task 1: set context, then clear
        Future<Void> task1 = executor.submit(() -> {
            EstateContext.setEstateId(estate1);
            EstateContext.setPermissions(Set.of("estate.read", "resident.create"));
            EstateContext.clear();
            return null;
        });
        task1.get(5, TimeUnit.SECONDS);

        // Task 2: same thread — must NOT see estate1 data
        Future<UUID> task2 = executor.submit(() -> {
            UUID leaked = EstateContext.getEstateId();
            Set<String> leakedPerms = EstateContext.getPermissions();

            // Set fresh context
            EstateContext.setEstateId(estate2);
            EstateContext.setPermissions(Set.of("gate.entry"));

            assertThat(leaked).as("Should not see previous request's estate ID").isNull();
            assertThat(leakedPerms).as("Should not see previous request's permissions").isEmpty();
            assertThat(EstateContext.getEstateId()).isEqualTo(estate2);

            EstateContext.clear();
            return leaked;
        });

        UUID leakedId = task2.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(leakedId).isNull();
    }

    @Test
    @DisplayName("hasPermission is thread-isolated")
    void hasPermissionThreadIsolated() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger failures = new AtomicInteger(0);

        // Thread A: has "estate.delete"
        Future<?> threadA = executor.submit(() -> {
            try {
                EstateContext.setPermissions(Set.of("estate.delete"));
                barrier.await(5, TimeUnit.SECONDS);
                Thread.sleep(10); // let thread B check

                if (!EstateContext.hasPermission("estate.delete")) {
                    failures.incrementAndGet(); // should still be true for me
                }
            } catch (Exception e) {
                failures.incrementAndGet();
            } finally {
                EstateContext.clear();
            }
        });

        // Thread B: does NOT have "estate.delete"
        Future<?> threadB = executor.submit(() -> {
            try {
                EstateContext.setPermissions(Set.of("gate.entry"));
                barrier.await(5, TimeUnit.SECONDS);

                if (EstateContext.hasPermission("estate.delete")) {
                    failures.incrementAndGet(); // should be false — thread B doesn't have it
                }
            } catch (Exception e) {
                failures.incrementAndGet();
            } finally {
                EstateContext.clear();
            }
        });

        threadA.get(10, TimeUnit.SECONDS);
        threadB.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(failures.get()).isZero();
    }
}
