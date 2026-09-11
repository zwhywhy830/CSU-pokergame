package com.csu.pokergame.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 一条对局战绩（阶段 17）：{@code data/players/<username>_records.json} 里的一条记录。
 *
 * <p>落盘形态（数组元素）：
 * <pre>{@code
 * {
 *   "id" : "20260910230101",
 *   "gameType" : "跑得快",
 *   "win" : true,
 *   "goldChange" : 100,
 *   "expChange" : 50,
 *   "time" : 1789052910767,
 *   "opponentInfo" : "AI"
 * }
 * }</pre>
 *
 * <p>字段含义：
 * <ul>
 *   <li>{@code id}：记录标识，由发生时间生成（{@code yyyyMMddHHmmss}），同秒多局自动补 {@code -2} 序号；</li>
 *   <li>{@code gameType}：玩法名（跑得快 / 骗子酒馆 …）；</li>
 *   <li>{@code win}：本局是否获胜（true 胜利 / false 失败）；</li>
 *   <li>{@code goldChange}：本局金币变动，<b>正数为收入、负数为支出</b>；
 *       注意这是"结算奖励"而不是"净变化"——入场费单独记在 {@code coin_log.json}；</li>
 *   <li>{@code expChange}：本局获得的经验；</li>
 *   <li>{@code time}：对局结算时间（epoch 毫秒）；</li>
 *   <li>{@code opponentInfo}：对手信息（本地人机为 {@code AI}）。</li>
 * </ul>
 *
 * <p>本类只做数据承载，不含任何业务判断：写入由 {@link GameRecordService} 负责、读取同样由它负责。
 * 写成普通 Bean（而非 record）是为了让 Jackson 在无参构造 + getter/setter 的标准映射下
 * 直接读写 JSON，同时让"展示用"的派生方法（{@link #getDisplayGoldChange()} 等）用
 * {@link JsonIgnore} 明确排除在落盘字段之外，保证文件里只有上面 7 个字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id", "gameType", "win", "goldChange", "expChange", "time", "opponentInfo"})
public class GameRecord {

    /** {@code time} 的完整展示格式。 */
    private static final DateTimeFormatter FULL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** 只到天的展示格式（跨年记录用）。 */
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 只到分钟的展示格式（今年内记录用）。 */
    private static final DateTimeFormatter MONTH_DAY_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    /** 时分展示格式（今天 / 昨天用）。 */
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    /** id 生成格式。 */
    private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 记录标识。 */
    private String id;
    /** 玩法名（跑得快 / 骗子酒馆 …）。 */
    private String gameType;
    /** 是否获胜。 */
    private boolean win;
    /** 本局金币变动（正数收入、负数支出）。 */
    private int goldChange;
    /** 本局获得的经验。 */
    private int expChange;
    /** 对局结算时间（epoch 毫秒）。 */
    private long time;
    /** 对手信息（本地人机为 AI）。 */
    private String opponentInfo;

    /** Jackson 反序列化用的无参构造。 */
    public GameRecord() {
    }

    public GameRecord(String id, String gameType, boolean win, int goldChange, int expChange,
                      long time, String opponentInfo) {
        this.id = id;
        this.gameType = gameType;
        this.win = win;
        this.goldChange = goldChange;
        this.expChange = expChange;
        this.time = time;
        this.opponentInfo = opponentInfo;
    }

    /**
     * 以"当前时间"创建一条记录：{@code id} 与 {@code time} 都由这一刻生成。
     *
     * @param gameType     玩法名
     * @param win          是否获胜
     * @param goldChange   本局金币变动
     * @param expChange    本局经验
     * @param opponentInfo 对手信息
     */
    public static GameRecord of(String gameType, boolean win, int goldChange, int expChange,
                               String opponentInfo) {
        long now = System.currentTimeMillis();
        return new GameRecord(makeId(now), gameType, win, goldChange, expChange, now, opponentInfo);
    }

    /** 由时间戳生成 14 位 id，例如 {@code 20260910230101}。 */
    public static String makeId(long epochMillis) {
        return ID_FORMAT.format(toLocalDateTime(epochMillis));
    }

    // ============================================================= getter / setter

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public boolean isWin() {
        return win;
    }

    public void setWin(boolean win) {
        this.win = win;
    }

    public int getGoldChange() {
        return goldChange;
    }

    public void setGoldChange(int goldChange) {
        this.goldChange = goldChange;
    }

    public int getExpChange() {
        return expChange;
    }

    public void setExpChange(int expChange) {
        this.expChange = expChange;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = Math.max(0L, time);
    }

    public String getOpponentInfo() {
        return opponentInfo;
    }

    public void setOpponentInfo(String opponentInfo) {
        this.opponentInfo = opponentInfo;
    }

    // ============================================================= 展示辅助（只读，不参与落盘）

    /** 结果文案：{@code 胜利} / {@code 失败}。 */
    @JsonIgnore
    public String getResultText() {
        return win ? "胜利" : "失败";
    }

    /** 带符号的金币变动文案：{@code +100金币} / {@code -50金币}。 */
    @JsonIgnore
    public String getDisplayGoldChange() {
        return (goldChange >= 0 ? "+" : "") + goldChange + "金币";
    }

    /** 带符号的经验变动文案：{@code +50经验} / {@code +0经验}。 */
    @JsonIgnore
    public String getDisplayExpChange() {
        return (expChange >= 0 ? "+" : "") + expChange + "经验";
    }

    /** 对局日期：{@code 2026-09-10}。 */
    @JsonIgnore
    public String getDisplayDate() {
        return DATE_ONLY.format(toLocalDateTime(time));
    }

    /**
     * 对局时间的口语化展示：
     * <ul>
     *   <li>今天 → {@code 今天 23:01}</li>
     *   <li>昨天 → {@code 昨天 23:01}</li>
     *   <li>今年内 → {@code 09-10 23:01}</li>
     *   <li>更早 → {@code 2025-12-31 23:01}</li>
     * </ul>
     */
    @JsonIgnore
    public String getDisplayTime() {
        if (time <= 0) {
            return "";
        }
        LocalDateTime moment = toLocalDateTime(time);
        LocalDate day = moment.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) {
            return "今天 " + CLOCK.format(moment);
        }
        if (day.equals(today.minusDays(1))) {
            return "昨天 " + CLOCK.format(moment);
        }
        if (day.getYear() == today.getYear()) {
            return MONTH_DAY_TIME.format(moment);
        }
        return FULL_TIME.format(moment);
    }

    @JsonIgnore
    private static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(Math.max(0L, epochMillis)),
                ZoneId.systemDefault());
    }

    // ============================================================= 工具

    /** 深拷贝：对外返回记录时不把内存对象暴露出去，避免被就地改写。 */
    public GameRecord copy() {
        return new GameRecord(id, gameType, win, goldChange, expChange, time, opponentInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GameRecord other)) {
            return false;
        }
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return gameType + " " + getResultText() + " " + getDisplayGoldChange() + " @ " + getDisplayDate();
    }
}
