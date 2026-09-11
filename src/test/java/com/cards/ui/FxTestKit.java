package com.cards.ui;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JavaFX 测试工具：在不启动主窗口的前提下初始化 JavaFX runtime，
 * 并把测试动作切到 JavaFX Application Thread 上同步执行。
 *
 * <p>所有需要构造 JavaFX 节点 / 播放时间轴的单元测试都通过
 * {@link #runAndWait(Runnable)} 包裹断言动作；动画结束信号用
 * {@link #waitForLatch(CountDownLatch, long)} 在测试线程上等待。
 */
public final class FxTestKit {

    /** toolkit 初始化与单步动作的默认超时。 */
    public static final long DEFAULT_TIMEOUT_SECONDS = 15;

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private FxTestKit() {
    }

    /** 初始化 JavaFX toolkit（幂等，整个 JVM 只做一次）。 */
    public static void initToolkit() {
        if (INITIALIZED.get()) {
            return;
        }
        synchronized (FxTestKit.class) {
            if (INITIALIZED.get()) {
                return;
            }
            try {
                CountDownLatch latch = new CountDownLatch(1);
                // Platform.startup 只能调用一次；toolkit 已启动时抛 IllegalStateException，视为成功
                Platform.startup(latch::countDown);
                await(latch);
            } catch (IllegalStateException alreadyStarted) {
                // JavaFX runtime 已由其他测试类启动，直接复用
            }
            INITIALIZED.set(true);
        }
    }

    /**
     * 在 JavaFX Application Thread 上执行动作并等待结束；
     * 动作抛出的异常会在测试线程上原样重抛。
     */
    public static void runAndWait(Runnable action) {
        initToolkit();
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        Throwable[] error = new Throwable[1];
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                done.countDown();
            }
        });
        await(done);
        if (error[0] instanceof RuntimeException re) {
            throw re;
        }
        if (error[0] instanceof Error err) {
            throw err;
        }
        if (error[0] != null) {
            throw new AssertionError("FX 线程动作失败", error[0]);
        }
    }

    /** 在测试线程上等待动画 / 回调信号（默认 15 秒），超时即失败。 */
    public static void waitForLatch(CountDownLatch latch) {
        waitForLatch(latch, DEFAULT_TIMEOUT_SECONDS);
    }

    /** 在测试线程上等待动画 / 回调信号，超时即失败。 */
    public static void waitForLatch(CountDownLatch latch, long timeoutSeconds) {
        try {
            if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new AssertionError("等待 JavaFX 回调超时（" + timeoutSeconds + "s）");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("等待 JavaFX 回调被中断", e);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError("JavaFX toolkit 初始化超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("JavaFX toolkit 初始化被中断", e);
        }
    }
}
