package com.csu.pokergame.player;

/**
 * 金币充值服务（阶段 12，模拟充值版）。
 *
 * <p>职责：模拟充值。点击即成功，不接支付流程、不接支付 SDK。
 * 充值动作只做一件事——把金额交给 {@link CoinService#addGold(int, String)}：
 * 于是余额、{@code coin_log.json} 流水、{@code player.json} 落盘三件事
 * 全部由 {@link CoinService} 统一完成。
 * 本类不缓存余额，读当前金币仍走 {@link CoinService#getGold()}。
 */
public final class CoinRechargeService {

    /** 充值流水统一原因。 */
    public static final String REASON = "模拟充值";

    private static volatile CoinRechargeService instance;

    private final CoinService coins;

    /** 生产用法：单例，转发到 {@link CoinService} 单例。 */
    public static CoinRechargeService getInstance() {
        CoinRechargeService local = instance;
        if (local == null) {
            synchronized (CoinRechargeService.class) {
                local = instance;
                if (local == null) {
                    local = new CoinRechargeService(CoinService.getInstance());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 测试 / 多档位注入用构造器。 */
    public CoinRechargeService(CoinService coins) {
        this.coins = coins;
    }

    // ============================================================= 充值

    /**
     * 模拟充值：点击立即成功。
     *
     * <p>实际动作 = {@code CoinService.addGold(amount, "模拟充值")}，
     * 因此余额累加、流水追加（reason 为"模拟充值"）、player.json 落盘一次完成。
     *
     * @param amount 充值金币数，必须 &gt; 0
     * @return 是否成功
     */
    public boolean recharge(int amount) {
        if (amount <= 0) {
            return false;
        }
        return coins.addGold(amount, REASON);
    }

    /** 当前金币余额（转发 {@link CoinService#getGold()}，界面刷新用）。 */
    public int getGold() {
        return coins.getGold();
    }
}
