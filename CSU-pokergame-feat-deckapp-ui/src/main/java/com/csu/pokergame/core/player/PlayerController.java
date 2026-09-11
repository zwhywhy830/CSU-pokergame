package com.csu.pokergame.core.player;

import com.csu.pokergame.core.engine.GameCommand;
import com.csu.pokergame.core.engine.GameSnapshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 玩家控制端口。本地玩家控制器通过 UI 取得指令（按钮点击时完成 future）；
 * 规则机器人立即返回选定命令；未来 AI / 网络控制器只需替换此端口实现。
 */
public interface PlayerController {

    /**
     * 根据快照与合法指令列表返回一个指令。
     * 返回结果必须落在 {@code legal} 内（引擎会再次校验）。
     */
    CompletableFuture<GameCommand> choose(GameSnapshot snapshot, List<GameCommand> legal);
}
