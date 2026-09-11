package com.csu.pokergame.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 背包服务（阶段 13）。 */
public final class InventoryService {

    private static volatile InventoryService instance;

    private final PlayerManager players;

    public static InventoryService getInstance() {
        InventoryService local = instance;
        if (local == null) {
            synchronized (InventoryService.class) {
                local = instance;
                if (local == null) {
                    local = new InventoryService(PlayerManager.getInstance());
                    instance = local;
                }
            }
        }
        return local;
    }

    public InventoryService(PlayerManager players) {
        this.players = players;
    }

    // ============================================================= 预置道具目录

    /** 幸运牌 id。 */
    public static final String LUCKY_CARD = "LUCKY_CARD";
    /** 双倍经验卡 id。 */
    public static final String DOUBLE_EXP_CARD = "DOUBLE_EXP_CARD";
    /** 金币宝箱 id。 */
    public static final String GOLD_BOX = "GOLD_BOX";
    private static final String ICON_LUCKY = "\uD83C\uDF40";
    private static final String ICON_EXP = "⭐";
    private static final String ICON_BOX = "🎁";

    /** 预置道具目录（模板，count=0）。 */
    private static final List<Item> CATALOG = List.of(
            new Item(LUCKY_CARD, "幸运牌", "增加游戏幸运效果", 0, ICON_LUCKY, Item.TYPE_CONSUMABLE),
            new Item(DOUBLE_EXP_CARD, "双倍经验卡", "用于成长系统", 0, ICON_EXP, Item.TYPE_CONSUMABLE),
            new Item(GOLD_BOX, "金币宝箱", "打开获得金币", 0, ICON_BOX, Item.TYPE_CONSUMABLE));

    /** 全部道具（含未拥有的 x0）。 */
    public List<Item> getItems() {
        return snapshot();
    }

    /** 按目录顺序生成"目录 + 当前数量"的快照（未拥有的 count=0）。 */
    private List<Item> snapshot() {
        List<Item> result = new ArrayList<>();
        for (Item template : CATALOG) {
            Item owned = players.getProfile().findItem(template.getId());
            if (owned == null) {
                result.add(template.copy());
            } else {
                Item copy = template.copy();
                copy.setCount(owned.getCount());
                result.add(copy);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** 查询某道具数量；不存在返回 0。 */
    public synchronized int getItemCount(String id) {
        Item item = players.getProfile().findItem(id);
        return item == null ? 0 : item.getCount();
    }

    /** 是否拥有（数量 &gt; 0）。 */
    public boolean hasItem(String id) {
        return getItemCount(id) > 0;
    }

    /** 增加物品并落盘；返回是否成功。 */
    public synchronized boolean addItem(String id, int count) {
        if (count <= 0) {
            return false;
        }
        Item template = findTemplate(id);
        if (template == null) {
            return false;
        }
        Item owned = players.getProfile().getOrCreateItem(template);
        owned.add(count);
        players.save();
        return true;
    }

    /** 消耗物品并落盘；数量不足返回 false 且不改动。 */
    public synchronized boolean removeItem(String id, int count) {
        if (count <= 0) {
            return false;
        }
        Item owned = players.getProfile().findItem(id);
        if (owned == null || !owned.reduce(count)) {
            return false;
        }
        players.save();
        return true;
    }

    private static Item findTemplate(String id) {
        if (id == null) {
            return null;
        }
        for (Item template : CATALOG) {
            if (template.getId().equals(id)) {
                return template;
            }
        }
        return null;
    }
}


