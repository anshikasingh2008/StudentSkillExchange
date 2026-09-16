package com.skillexchange.thread;

import com.skillexchange.service.SkillExchangeManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the Multithreading portion of the syllabus: several
 * threads race to accept the SAME pending exchange request at the
 * same instant. Because SkillExchangeManager.acceptRequest() is
 * synchronized on a shared lock, exactly one thread wins and the
 * rest safely back off instead of double-booking the same slot.
 *
 * Run this as its own scenario from the console menu ("Run
 * concurrency demo") after creating at least one pending request.
 */
public class ConcurrentMatchDemo {

    private final SkillExchangeManager manager;

    public ConcurrentMatchDemo(SkillExchangeManager manager) {
        this.manager = manager;
    }

    /**
     * Spins up {@code threadCount} threads that all try to accept the
     * same request concurrently. Prints which thread(s) won.
     */
    public void run(int requestId, int threadCount) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);   // release all threads at once
        CountDownLatch doneGate = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 1; i <= threadCount; i++) {
            final int threadNum = i;
            pool.submit(() -> {
                try {
                    startGate.await(); // all threads wait here, then fire together
                    boolean accepted = manager.acceptRequest(requestId);
                    if (accepted) {
                        successCount.incrementAndGet();
                        System.out.println("  [Thread-" + threadNum + "] SUCCESS - accepted request #" + requestId);
                    } else {
                        System.out.println("  [Thread-" + threadNum + "] blocked - request already handled");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.out.println("  [Thread-" + threadNum + "] error - " + e.getMessage());
                } finally {
                    doneGate.countDown();
                }
            });
        }

        System.out.println("Launching " + threadCount + " threads to accept request #" + requestId + " simultaneously...");
        startGate.countDown(); // release the gate - all threads race now
        doneGate.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.println("Result: " + successCount.get() + " thread(s) succeeded (expected: 1 if request was PENDING).");
    }

    /**
     * Same race as {@link #run}, but returns the log as a list of
     * strings instead of printing to stdout - used by the web API so
     * the browser can display the thread-by-thread outcome.
     */
    public java.util.List<String> runForApi(int requestId, int threadCount) throws InterruptedException {
        java.util.List<String> log = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        log.add("Launching " + threadCount + " threads to accept request #" + requestId + " simultaneously...");

        for (int i = 1; i <= threadCount; i++) {
            final int threadNum = i;
            pool.submit(() -> {
                try {
                    startGate.await();
                    boolean accepted = manager.acceptRequest(requestId);
                    if (accepted) {
                        successCount.incrementAndGet();
                        log.add("[Thread-" + threadNum + "] SUCCESS - accepted request #" + requestId);
                    } else {
                        log.add("[Thread-" + threadNum + "] blocked - request already handled");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.add("[Thread-" + threadNum + "] error - " + e.getMessage());
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        log.add("Result: " + successCount.get() + " thread(s) succeeded (expected: 1 if request was PENDING).");
        return log;
    }
}
