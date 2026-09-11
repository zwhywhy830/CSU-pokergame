package com.csu.pokergame.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家商城服务（阶段 14）：金币购买道具的唯一入口。
 *
 * <p>本阶段只做<b>商城框架 + 购买流程</b>，不涉及活动 / VIP / 限时商品 / 支付，
 * 购买的也只是数据道具（道具实际效果由后续阶段实现）。
 *
 * <p>购买动作严格按顺序执行：
 * <pre>{@code
 * buy(shopId)
 *   ├─ 1. 查货架：找不到商品 → 返回 false
 *   ├─ 2. 扣金币：CoinService.costGold(price, "购买xxx")
 *   │        └─ 余额不足 → 返回 false（不扣款、不发道具、不写流水）
 *   ├─ 3. 发道具：InventoryService.addItem(itemId, 1)
 *   └─ 4. 落盘  ：PlayerManager.save()
 * }</pre>
 *
 * <p>金币的唯一写入口永远是 {@link CoinService}，背包的唯一写入口永远是
 * {@link InventoryService}，本类只负责"把这两步按正确顺序串起来"，不自己改余额 / 背包。
 */
public final class ShopService {

    private static volatile ShopService instance;

    private final CoinService coins;
    private final InventoryService inventory;
    private final PlayerManager players;

    /** 生产用法：单例，转发到 {@link CoinService} / {@link InventoryService} / {@link PlayerManager} 单例。 */
    public static ShopService getInstance() {
        ShopService local = instance;
        if (local == null) {
            synchronized (ShopService.class) {
                local = instance;
                if (local == null) {
                    local = new ShopService(CoinService.getInstance(),
                            InventoryService.getInstance(), PlayerManager.getInstance());
                    instance = local;
                }
            }
        }
        return local;
    }

    /** 测试 / 多档位注入用构造器。 */
    public ShopService(CoinService coins, InventoryService inventory, PlayerManager players) {
        this.coins = coins;
        this.inventory = inventory;
        this.players = players;
    }

    // ============================================================= 货架

    private static final String ICON_LUCKY = "\uD83C\uDF40";
    private static final String ICON_EXP = "⭐";
    private static final String ICON_BOX = "🎁";

    /**
     * 预置货架（阶段 14）。与本阶段需求一一对应：
     * 幸运牌 500、双倍经验卡 1000、金币宝箱 800；购买后分别进入背包的
     * {@link InventoryService#LUCKY_CARD} / {@link InventoryService#DOUBLE_EXP_CARD} /
     * {@link InventoryService#GOLD_BOX}。
     */
    private static final List<ShopItem> CATALOG = List.of(
            new ShopItem(InventoryService.LUCKY_CARD, "幸运牌", "增加游戏幸运效果", 500,
                    InventoryService.LUCKY_CARD, ICON_LUCKY),
            new ShopItem(InventoryService.DOUBLE_EXP_CARD, "双倍经验卡", "增加经验收益", 1000,
                    InventoryService.DOUBLE_EXP_CARD, ICON_EXP),
            new ShopItem(InventoryService.GOLD_BOX, "金币宝箱", "打开获得金币", 800,
                    InventoryService.GOLD_BOX, ICON_BOX));

    /** 全部商品（只读快照，顺序与货架定义一致）。 */
    public List<ShopItem> getShopItems() {
        List<ShopItem> result = new ArrayList<>(CATALOG.size());
        for (ShopItem item : CATALOG) {
            result.add(item.copy());
        }
        return Collections.unmodifiableList(result);
    }

    /** 按商品 id 查货架；不存在返回 null。 */
    public ShopItem getItem(String id) {
        if (id == null) {
            return null;
        }
        for (ShopItem item : CATALOG) {
            if (id.equals(item.getId())) {
                return item.copy();
            }
        }
        return null;
    }

    // ============================================================= 购买

    /**
     * 用金币购买商品，成功后道具进入背包。
     *
     * <p><b>失败原子性：</b>只要任一步失败都不会留下半成品——
     * 商品不存在、金币不足都不会扣款也不会发道具；万一发道具环节异常失败，
     * 会把已扣的金币原路退回，绝不允许"扣了钱没拿到货"。
     *
     * @param shopId 商品 id（如 {@code LUCKY_CARD}）
     * @return 是否购买成功
     */
    public synchronized boolean buy(String shopId) {
        ShopItem item = getItem(shopId);
        if (item == null) {
            return false;
        }
        // 1) 扣金币：余额不足会直接返回 false，且不产生任何副作用
        if (!coins.costGold(item.getPrice(), "购买" + item.getName())) {
            return false;
        }
        // 2) 发道具：进入背包
        boolean added = inventory.addItem(item.getItemId(), 1);
        if (!added) {
            // 兜底：货架与背包目录理论上一致，不会走到这里；真发生则退款，保证账物一致
            coins.addGold(item.getPrice(), "购买失败退款：" + item.getName());
            return false;
        }
        // 3) 落盘：InventoryService.addItem 已保存一次，这里再显式保存一次以对齐流程
        players.save();
        // 阶段 21：购买成功 → 播放金币音效（无音频资源时 AudioService 静默跳过）
        com.csu.pokergame.audio.AudioService.getInstance()
                .playEffect(com.csu.pokergame.audio.SoundEffect.COIN_GAIN);
        return true;
    }

    /** 当前金币余额（转发 {@link CoinService#getGold()}，界面刷新用）。 */
    public int getGold() {
        return coins.getGold();
    }
}
