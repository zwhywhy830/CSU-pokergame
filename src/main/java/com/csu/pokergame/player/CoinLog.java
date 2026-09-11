package com.csu.pokergame.player;

/**
 * 一条金币流水（阶段 12-2）。
 *
 * <p>与 {@code resources/player/coin_log.json} 里的一条记录一一对应：
 * <pre>{@code
 * {
 *   "time" : "2026-09-10 22:02:15",
 *   "change" : 1000,
 *   "reason" : "模拟充值"
 * }
 * }</pre>
 *
 * <p>字段含义：
 * <ul>
 *   <li>{@code time}：发生时间，格式 {@code yyyy-MM-dd HH:mm:ss}；</li>
 *   <li>{@code change}：变动值，<b>正数为收入、负数为支出</b>（不会为 0）；</li>
 *   <li>{@code reason}：变动原因（如"模拟充值""跑得快入场""跑得快胜利奖励"）。</li>
 * </ul>
 *
 * <p>本类只做数据承载，不含任何业务判断；写入由 {@link CoinService} 负责，读取由
 * {@link CoinLogService} 负责。写成普通 Bean（而非 record）是为了让 Jackson 在
 * 无参构造 + getter/setter 的标准映射下直接读写 {@code coin_log.json}。
 */
public class CoinLog {

    private String time;
    private int change;
    private String reason;

    /** Jackson 反序列化用。 */
    public CoinLog() {
    }

    public CoinLog(String time, int change, String reason) {
        this.time = time;
        this.change = change;
        this.reason = reason;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getChange() {
        return change;
    }

    public void setChange(int change) {
        this.change = change;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // ============================================================= 展示辅助（只读，不参与序列化语义）

    /** 是否为收入（正数）。 */
    public boolean isIncome() {
        return change > 0;
    }

    /** 是否为支出（负数）。 */
    public boolean isExpense() {
        return change < 0;
    }

    /** 带符号的变动文本：收入 {@code +1000}、支出 {@code -50}。 */
    public String getDisplayChange() {
        return (change > 0 ? "+" : "") + change;
    }

    @Override
    public String toString() {
        return getDisplayChange() + " " + reason + " @ " + time;
    }
}
