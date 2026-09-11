package com.csu.pokergame.player;

/** Achievement: id / title / description / rewardGold / rewardExp / unlocked. */
public final class Achievement {

    private final String id;
    private final String title;
    private final String description;
    private final int rewardGold;
    private final int rewardExp;
    private boolean unlocked;

    public Achievement(String id, String title, String description, int rewardGold, int rewardExp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.rewardGold = Math.max(0, rewardGold);
        this.rewardExp = Math.max(0, rewardExp);
        this.unlocked = false;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getRewardGold() {
        return rewardGold;
    }

    public int getRewardExp() {
        return rewardExp;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    /** 置为已解锁；返回本次是否发生了状态变化（用于防重复发奖）。 */
    boolean markUnlocked() {
        if (unlocked) {
            return false;
        }
        unlocked = true;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Achievement a && id != null && id.equals(a.id));
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "Achievement[" + id + " " + title + (unlocked ? " 已解锁" : " 未解锁") + "]";
    }
}