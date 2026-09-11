package com.csu.pokergame.player;

import com.csu.pokergame.network.JsonCodec;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 金币流水查询服务（阶段 12-2）。
 *
 * <p>职责只有一个：<b>读</b>。把 {@code resources/player/coin_log.json} 解析成 {@link CoinLog} 列表，
 * 供个人中心「金币流水」区域展示。本类<b>不写</b>流水、也不改余额——
 * 写入永远是 {@link CoinService} 的事（这样才能保证"流水 = 余额变动"永不错位）。
 *
 * <p>读取顺序（任一失败都自动降级，不影响界面打开）：
 * <ol>
 *   <li>{@link CoinService#getLogFile()} 指向的流水文件（与 {@code player.json} 同目录，通常是
 *       {@code src/main/resources/player/coin_log.json}）；</li>
 *   <li>classpath 资源 {@code /player/coin_log.json}（首次运行、文件尚未生成时）；</li>
 *   <li>{@link CoinService} 内存中的流水快照（文件被外部占用 / 损坏时的兜底）。</li>
 * </ol>
 *
 * <p>每次查询都重新读盘，因此牌局结算、充值等任何地方新增的流水，下一次界面刷新即可见。
 */
public final class CoinLogService {

    /** classpath 上的默认流水位置。 */
    private static final String CLASSPATH_LOCATION = "/player/coin_log.json";

    /** 界面默认展示条数（最近 10 条）。 */
    public static final int DEFAULT_LIMIT = 10;

    private static volatile CoinLogService instance;

    private final CoinService coins;

    /** 生产用法：单例，与 {@link CoinService} 共用同一个流水文件。 */
    public static CoinLogService getInstance() {
        CoinLogService local = instance;
        if (local == null) {
            synchronized (CoinLogService.class) {
                local = instance;
                if (local == null) {
                    local = new CoinLogService(CoinService.getInstance());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 测试 / 多档位注入用构造器。 */
    public CoinLogService(CoinService coins) {
        this.coins = coins;
    }

    // ============================================================= 查询

    /**
     * 最近 {@code count} 条流水，<b>最新的在前</b>（界面按时间倒序展示，刚发生的排最上面）。
     *
     * @param count 需要的条数，{@code <= 0} 返回空列表
     * @return 不可变列表；流水不足 {@code count} 条时按实际数量返回
     */
    public List<CoinLog> getRecentLogs(int count) {
        if (count <= 0) {
            return List.of();
        }
        List<CoinLog> all = getAllLogs();
        int from = Math.max(0, all.size() - count);
        List<CoinLog> recent = new ArrayList<>(all.subList(from, all.size()));
        Collections.reverse(recent);
        return Collections.unmodifiableList(recent);
    }

    /** 默认条数的最近流水（{@link #DEFAULT_LIMIT} 条）。 */
    public List<CoinLog> getRecentLogs() {
        return getRecentLogs(DEFAULT_LIMIT);
    }

    /** 全量流水，按发生顺序（最早在前，与 {@code coin_log.json} 文件顺序一致）。 */
    public List<CoinLog> getAllLogs() {
        List<CoinLog> fromFile = loadFromFile();
        if (fromFile != null) {
            return Collections.unmodifiableList(fromFile);
        }
        return fromMemory();
    }

    /** 流水总条数（余额校验 / 调试用）。 */
    public int size() {
        return getAllLogs().size();
    }

    // ============================================================= 读盘

    /** 优先读流水文件，其次读 classpath；都拿不到返回 null 交由调用方兜底。 */
    private List<CoinLog> loadFromFile() {
        Path file = coins == null ? null : coins.getLogFile();
        if (file != null && Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                List<CoinLog> loaded = parse(in);
                if (loaded != null) {
                    return loaded;
                }
            } catch (Exception e) {
                warn("读取流水失败，改用内存快照: " + file + " -> " + e.getMessage());
            }
        }
        try (InputStream in = CoinLogService.class.getResourceAsStream(CLASSPATH_LOCATION)) {
            if (in != null) {
                List<CoinLog> loaded = parse(in);
                if (loaded != null) {
                    return loaded;
                }
            }
        } catch (Exception e) {
            warn("读取 classpath 流水失败: " + CLASSPATH_LOCATION + " -> " + e.getMessage());
        }
        return null;
    }

    /** 兜底：把 CoinService 内存里的流水（record）转成本类的流水对象。 */
    private List<CoinLog> fromMemory() {
        if (coins == null) {
            return List.of();
        }
        List<CoinLog> converted = new ArrayList<>();
        for (CoinService.CoinLogEntry entry : coins.getLog()) {
            converted.add(new CoinLog(entry.time(), entry.change(), entry.reason()));
        }
        return Collections.unmodifiableList(converted);
    }

    private static List<CoinLog> parse(InputStream in) throws Exception {
        return JsonCodec.mapper().readValue(in, JsonCodec.mapper()
                .getTypeFactory().constructCollectionType(List.class, CoinLog.class));
    }

    private static void warn(String message) {
        System.err.println("[CoinLogService] " + message);
    }
}
