package com.csu.pokergame.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 背包物品（阶段 13）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {

    private String id;
    private String name;
    private String description;
    private int count;
    private String icon;
    private String type;

    public Item() {
    }

    public Item(String id, String name, String description, int count, String icon, String type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.count = Math.max(0, count);
        this.icon = icon;
        this.type = type;
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = Math.max(0, count);
    }

    public String getIcon() {
        return icon == null ? "" : icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getType() {
        return type == null ? TYPE_CONSUMABLE : type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /** 物品类型常量：消耗品。本阶段只有这一种，后续可扩展。 */
    public static final String TYPE_CONSUMABLE = "CONSUMABLE";

    /** 是否还有剩余数量（不参与序列化）。 */
    @JsonIgnore
    public boolean isEmpty() {
        return count <= 0;
    }

    /** 叠加数量（负数不生效），返回叠加后的数量。 */
    public int add(int delta) {
        count = Math.max(0, count + Math.max(0, delta));
        return count;
    }

    /** 扣减数量；不足时返回 false 且不改动。 */
    public boolean reduce(int delta) {
        if (delta <= 0 || delta > count) {
            return false;
        }
        count -= delta;
        return true;
    }

    /** 深拷贝：PlayerProfile.copy() 用，避免两个档案共享同一个物品实例。 */
    public Item copy() {
        return new Item(id, name, description, count, icon, type);
    }

    @Override
    public String toString() {
        return getIcon() + " " + getName() + " x" + count + " [" + getType() + "]";
    }
}
