package com.csu.pokergame.liarspoker;

/**
 * 玩家手枪状态。6 仓左轮，子弹装在 bulletPosition 仓（1–6），
 * 已扣扳机 shotsFired 次（顺序从第 1 仓打起，下次扣第 shotsFired+1 仓）。
 * 打到子弹仓即淘汰；重新洗牌时保留此状态。
 */
public record GunState(int bulletPosition, int shotsFired) {

    /** 下次要打的仓号（1–6）。 */
    public int nextChamber() {
        return shotsFired + 1;
    }

    /** 下次扣扳机是否中弹。 */
    public boolean wouldHit() {
        return nextChamber() == bulletPosition;
    }

    /** 扣一次扳机后的新状态。 */
    public GunState fire() {
        return new GunState(bulletPosition, shotsFired + 1);
    }

    /** 剩余未扣仓数（含子弹仓）。 */
    public int remainingChambers() {
        return Math.max(0, 6 - shotsFired);
    }
}
