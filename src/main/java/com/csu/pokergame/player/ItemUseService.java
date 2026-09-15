package com.csu.pokergame.player;

/**
 * 道具使用服务（阶段 15）：背包道具"使用 / 开启"的唯一入口。
 *
 * <p>本阶段让阶段 13 / 14 里的道具具备实际使用能力，流程严格按顺序执行：
 * <pre>{@code
 * useItem(itemId)
 *   ├─ 1. 查背包数量：数量 <= 0 → 返回 false（不改动任何数据）
 *   ├─ 2. 执行道具效果
 *   ├─ 3. InventoryService.removeItem(itemId, 1) 消耗一件
 *   └─ 4. PlayerManager.save() 落盘
 * }</pre>
 *
 * <p>三类道具效果：
 * <ul>
 *   <li>{@link InventoryService#LUCKY_CARD}：{@code luckyCount +1}（仅记录状态，供未来奖励倍率使用）；</li>
 *   <li>{@link InventoryService#DOUBLE_EXP_CARD}：{@code doubleExpCount +1}（仅记录状态，未来经验翻倍）；</li>
 *   <li>{@link InventoryService#GOLD_BOX}：{@link CoinService#addGold(int, String)} 立即 +1000 金币，
 *       余额、{@code coin_log.json} 流水、{@code player.json} 全部由 CoinService 统一完成。</li>
 * </ul>
 *
 * <p>金币的唯一写入口仍是 {@link CoinService}，背包的唯一写入口仍是 {@link InventoryService}，
 * 本类只负责"校验数量 → 触发效果 → 扣道具 → 落盘"这一步串联，不自己改余额 / 背包。
 */
public final class ItemUseService {

    /** 金币宝箱开启奖励。 */
    public static final int GOLD_BOX_REWARD = 1000;
    /** 开启金币宝箱的流水原因。 */
    public static final String REASON_GOLD_BOX = "开启金币宝箱";

    private static volatile ItemUseService instance;

    private final InventoryService inventory;
    private final CoinService coins;
    private final PlayerManager players;

    /** 生产用法：单例，转发到 {@link InventoryService} / {@link CoinService} / {@link PlayerManager} 单例。 */
    public static ItemUseService getInstance() {
        ItemUseService local = instance;
        if (local == null) {
            synchronized (ItemUseService.class) {
                local = instance;
                if (local == null) {
                    local = new ItemUseService(InventoryService.getInstance(),
                            CoinService.getInstance(), PlayerManager.getInstance());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 测试 / 多档位注入用构造器。 */
    public ItemUseService(InventoryService inventory, CoinService coins, PlayerManager players) {
        this.inventory = inventory;
        this.coins = coins;
        this.players = players;
    }

    // ============================================================= 使用

    /**
     * 使用一件道具：数量不足直接失败；否则执行效果并消耗一件。
     *
     * @param itemId 道具 id（{@link InventoryService} 目录中的 id）
     * @return 是否使用成功（数量不足 / 未知道具返回 false，且不改动任何数据）
     */
    public synchronized boolean useItem(String itemId) {
        if (itemId == null) {
            return false;
        }
        // 1) 数量校验：没有就什么都不做
        if (inventory.getItemCount(itemId) <= 0) {
            return false;
        }
        // 2) 效果
        if (!applyEffect(itemId)) {
            return false;
        }
        // 3) 消耗一件；理论上此时一定成功
        if (!inventory.removeItem(itemId, 1)) {
            return false;
        }
        // 4) 落盘（removeItem 已保存一次，这里再显式保存以对齐流程）
        players.save();
        return true;
    }

    /** 分发道具效果；返回是否成功（未知道具返回 false）。 */
    private boolean applyEffect(String itemId) {
        PlayerProfile profile = players.getProfile();
        switch (itemId) {
            case InventoryService.LUCKY_CARD:
                // 幸运状态 +1：本阶段只记录，不参与结算
                profile.setLuckyCount(profile.getLuckyCount() + 1);
                return true;
            case InventoryService.DOUBLE_EXP_CARD:
                // 双倍经验状态 +1：本阶段只记录，不参与结算
                profile.setDoubleExpCount(profile.getDoubleExpCount() + 1);
                return true;
            case InventoryService.GOLD_BOX:
                // 立即获得金币：余额 / 流水 / 落盘统一由 CoinService 完成
                return coins.addGold(GOLD_BOX_REWARD, REASON_GOLD_BOX);
            default:
                return false;
        }
    }
}
