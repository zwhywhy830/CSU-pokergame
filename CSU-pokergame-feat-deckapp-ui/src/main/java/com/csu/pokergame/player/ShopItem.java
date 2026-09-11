package com.csu.pokergame.player;

/**
 * 商城商品（阶段 14）：一条"用金币购买道具"的货架条目。
 *
 * <p>字段与商品的展示 / 交易一一对应：
 * <pre>{@code
 * id          商品 id（货架主键，如 LUCKY_CARD）
 * name        商品名（如 幸运牌）
 * description 商品说明
 * price       售价（金币，必须 >= 0）
 * itemId      购买后进入背包的道具 id（对应 InventoryService 目录）
 * icon        图标（emoji）
 * }</pre>
 *
 * <p>本类只是<b>货架上的静态数据</b>，不持有玩家余额、不做扣款。
 * 所有交易动作都在 {@link ShopService} 里，最终扣金币仍统一走 {@link CoinService}、
 * 进背包统一走 {@link InventoryService}。
 *
 * <p>商品目录是运行时内置的常量表，<b>不落盘</b>，因此不涉及旧存档兼容问题。
 */
public class ShopItem {

    private String id;
    private String name;
    private String description;
    private int price;
    private String itemId;
    private String icon;

    public ShopItem() {
    }

    public ShopItem(String id, String name, String description, int price, String itemId, String icon) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = Math.max(0, price);
        this.itemId = itemId;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name == null ? id : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** 售价（金币）。 */
    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = Math.max(0, price);
    }

    /** 购买后进入背包的道具 id。 */
    public String getItemId() {
        return itemId == null ? id : itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getIcon() {
        return icon == null ? "" : icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    /** 深拷贝：避免界面拿到货架对象后改动到目录本体。 */
    public ShopItem copy() {
        return new ShopItem(id, name, description, price, itemId, icon);
    }

    @Override
    public String toString() {
        return getIcon() + " " + getName() + " " + price + "金币 -> " + getItemId();
    }
}
