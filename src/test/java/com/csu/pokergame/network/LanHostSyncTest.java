package com.csu.pokergame.network;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;
import com.csu.pokergame.core.engine.PlayerId;
import com.csu.pokergame.paodekuai.PdkSnapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端验证局域网牌局同步（真实 TCP 回环 + 真实 {@link LanClient}）：
 * <ul>
 *   <li>主机本地出牌、客户端出牌、机器人出牌三条路径都会触发主机本地快照回调</li>
 *   <li>两名客户端都能持续收到 GAME_SNAPSHOT，且公开状态（各家剩牌数）与主机一致</li>
 *   <li>对局能打到终局，三端都收到 winner / GAME_ENDED</li>
 * </ul>
 */
class LanHostSyncTest {

    private LanHost host;
    private LanClient client2;
    private LanClient client3;

    @AfterEach
    void tearDown() {
        if (client3 != null) client3.shutdown();
        if (client2 != null) client2.shutdown();
        if (host != null) host.shutdown();
    }

    @Test
    void hostUiRefreshesForLocalAndRemoteCommandsAndClientsStayInSync() throws Exception {
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        Thread.sleep(100);

        AtomicInteger localCallbackCount = new AtomicInteger();
        host.setOnLocalSnapshot(s -> localCallbackCount.incrementAndGet());
        // 模拟 UI 接线：IO 线程收到命令后交回主机处理（生产中会先 Platform.runLater）
        host.setOnCommand(pc -> host.handleRemoteCommand(pc.seat(), pc.command()));

        CountDownLatch start2 = new CountDownLatch(1);
        CountDownLatch start3 = new CountDownLatch(1);
        AtomicInteger snapCount2 = new AtomicInteger();
        AtomicInteger snapCount3 = new AtomicInteger();
        final GameSnapshot[] lastSnap2 = new GameSnapshot[1];
        final GameSnapshot[] lastSnap3 = new GameSnapshot[1];
        final String[] ended2 = new String[1];
        final String[] ended3 = new String[1];

        client2 = new LanClient("127.0.0.1", host.port());
        client2.setOnStartGame((type, seat) -> {
            assertThat(seat).isEqualTo(PlayerId.SEAT_2);
            start2.countDown();
        });
        client2.setOnSnapshot(s -> {
            snapCount2.incrementAndGet();
            lastSnap2[0] = s;
        });
        client2.setOnEnded(reason -> ended2[0] = reason);

        client3 = new LanClient("127.0.0.1", host.port());
        client3.setOnStartGame((type, seat) -> {
            assertThat(seat).isEqualTo(PlayerId.SEAT_3);
            start3.countDown();
        });
        client3.setOnSnapshot(s -> {
            snapCount3.incrementAndGet();
            lastSnap3[0] = s;
        });
        client3.setOnEnded(reason -> ended3[0] = reason);

        client2.join(PlayerId.SEAT_2);
        client3.join(PlayerId.SEAT_3);

        // 等待两名客户端入座、房间满员
        waitUntil(() -> host.currentRoom().full(), 5000);

        host.startGame();
        assertThat(start2.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(start3.await(5, TimeUnit.SECONDS)).isTrue();

        // 驱动一整局：轮到谁就由谁提交主机判定的第一条合法命令
        int submitted = 0;
        while (host.localSnapshot().winner().isEmpty() && submitted < 300) {
            GameSnapshot snap = host.localSnapshot();
            PlayerId current = snap.currentPlayer();
            List<GameCommand> legal = host.legalCommands(current);
            assertThat(legal).as("轮到 %s 时必须存在合法命令", current).isNotEmpty();
            GameCommand cmd = legal.get(0);
            if (current == PlayerId.SEAT_1) {
                host.submitLocalCommand(cmd);
            } else if (current == PlayerId.SEAT_2) {
                client2.submitCommand(cmd);
            } else {
                client3.submitCommand(cmd);
            }
            submitted++;
            Thread.sleep(80); // 给 TCP 往返与客户端回调留出时间
        }

        PdkSnapshot hostFinal = (PdkSnapshot) host.localSnapshot();
        assertThat(hostFinal.winner()).as("对局应在限步内打完").isPresent();
        assertThat(localCallbackCount.get())
                .as("每条生效命令都应回调主机 UI（本地 + 远程路径）")
                .isEqualTo(submitted);

        // 客户端最终也应收到终局事件与多帧快照
        waitUntil(() -> ended2[0] != null && ended3[0] != null, 5000);
        assertThat(ended2[0]).isEqualTo("GAME_OVER");
        assertThat(ended3[0]).isEqualTo("GAME_OVER");
        assertThat(snapCount2.get()).isGreaterThan(1);
        assertThat(snapCount3.get()).isGreaterThan(1);

        // 公开状态一致性：各端看到的各家剩牌数必须完全相同
        assertThat(((PdkSnapshot) lastSnap2[0]).remainingCardCounts())
                .isEqualTo(hostFinal.remainingCardCounts());
        assertThat(((PdkSnapshot) lastSnap3[0]).remainingCardCounts())
                .isEqualTo(hostFinal.remainingCardCounts());
    }

    @Test
    void hostUiRefreshesForBotCommands() throws Exception {
        host = new LanHost(GameType.PAO_DE_KUAI, 0);
        Thread.sleep(100);

        AtomicInteger localCallbackCount = new AtomicInteger();
        host.setOnLocalSnapshot(s -> localCallbackCount.incrementAndGet());

        host.addBot(PlayerId.SEAT_2);
        host.addBot(PlayerId.SEAT_3);
        // 无 JavaFX 环境：机器人立即出牌，链式推进到主机回合
        host.setBotScheduler((task, delayMs) -> task.run());

        host.startGame();

        int guard = 0;
        while (host.localSnapshot().winner().isEmpty() && guard++ < 200) {
            GameSnapshot snap = host.localSnapshot();
            // 同步调度器返回后，当前玩家只可能是主机或已终局
            assertThat(snap.currentPlayer()).isEqualTo(PlayerId.SEAT_1);
            List<GameCommand> legal = host.legalCommands(PlayerId.SEAT_1);
            assertThat(legal).isNotEmpty();
            host.submitLocalCommand(legal.get(0));
        }

        assertThat(host.localSnapshot().winner()).as("机器人局应能打完").isPresent();
        assertThat(localCallbackCount.get())
                .as("机器人与主机的出牌都应触发主机 UI 回调")
                .isGreaterThan(1);
    }

    private static void waitUntil(BooleanCondition condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("条件等待超时");
    }

    @FunctionalInterface
    private interface BooleanCondition {
        boolean get();
    }
}
