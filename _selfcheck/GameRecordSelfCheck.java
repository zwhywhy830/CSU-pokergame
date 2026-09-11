import com.csu.pokergame.network.JsonCodec;
import com.csu.pokergame.player.GameRecord;
import com.csu.pokergame.player.GameRecordService;
import com.csu.pokergame.player.PlayerManager;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 阶段 17 战绩系统自检（无界面，纯数据）：
 * <ol>
 *   <li>文件推导：{@code playerA.json → playerA_records.json}；</li>
 *   <li>空账号：不主动建文件、总场次 0、胜率 0.0（不是 NaN）；</li>
 *   <li>写入 3 局后：落盘 JSON 每条<b>恰好 7 个字段且顺序固定</b>、统计数字正确；</li>
 *   <li>同一秒多局：id 自动补 {@code -2}；</li>
 *   <li>账号隔离：切换存档后读不到上一个账号的战绩，旧文件不被污染。</li>
 * </ol>
 */
public class GameRecordSelfCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("recordcheck");
        Path playerA = dir.resolve("playerA.json");
        Path playerB = dir.resolve("playerB.json");
        Files.writeString(playerA, "{}");
        Files.writeString(playerB, "{}");

        PlayerManager players = new PlayerManager(playerA);
        GameRecordService records = new GameRecordService(players);

        System.out.println("== 1. 文件推导 ==");
        Path fileA = records.getRecordsFile();
        System.out.println("  recordsFile = " + fileA);
        check("由存档名推导出 playerA_records.json",
                fileA != null && "playerA_records.json".equals(fileA.getFileName().toString()));

        System.out.println();
        System.out.println("== 2. 空账号（不建文件 / 胜率不为 NaN）==");
        check("首次访问不主动建文件", !Files.isRegularFile(fileA));
        check("总场次 = 0", records.getTotalGames() == 0);
        check("胜率 = 0.0（非 NaN）", records.getWinRate() == 0.0);
        check("胜率百分数 = 0", records.getWinRatePercent() == 0);
        check("没有记录时 getLatestRecord() = null", records.getLatestRecord() == null);

        System.out.println();
        System.out.println("== 3. 写入 3 局（胜 / 负 / 胜）==");
        long t1 = 1789052910767L;
        String baseId = GameRecord.makeId(t1);
        records.addRecord(new GameRecord(baseId, GameRecordService.GAME_PDK, true, 100, 50, t1,
                GameRecordService.OPPONENT_AI));
        // 与上一条同一秒 → id 应自动补 -2
        records.addRecord(new GameRecord(baseId, GameRecordService.GAME_PDK, false, -20, 20, t1 + 1000,
                GameRecordService.OPPONENT_AI));
        records.addRecord(GameRecordService.GAME_PDK, true, 100, 50, GameRecordService.OPPONENT_AI);

        check("文件已落盘", Files.isRegularFile(fileA));

        JsonNode arr = JsonCodec.mapper().readTree(fileA.toFile());
        check("顶层是数组", arr.isArray());
        check("共 3 条", arr.size() == 3);

        List<String> expectedOrder = List.of(
                "id", "gameType", "win", "goldChange", "expChange", "time", "opponentInfo");
        boolean fieldsOk = true;
        for (JsonNode node : arr) {
            List<String> actual = new ArrayList<>();
            node.fieldNames().forEachRemaining(actual::add);
            if (!actual.equals(expectedOrder)) {
                fieldsOk = false;
                System.out.println("    字段异常: " + actual);
            }
        }
        check("每条恰好 7 个字段且顺序固定（无派生字段）", fieldsOk);

        check("总场次 = 3", records.getTotalGames() == 3);
        check("胜 = 2", records.getWinCount() == 2);
        check("负 = 1", records.getLossCount() == 1);
        check("胜率 = 67%", records.getWinRatePercent() == 67);

        List<GameRecord> all = records.getRecords();
        check("同秒第二条 id 补 -2", all.size() == 3 && (baseId + "-2").equals(all.get(1).getId()));
        check("getRecords() 时间升序（末尾最新）", all.get(2).getId().equals(records.getLatestRecord().getId()));

        List<GameRecord> recent = records.getRecentRecords();
        check("最近列表长度 = 3", recent.size() == 3);
        check("最近列表最新在前", recent.get(0).getId().equals(all.get(2).getId()));

        System.out.println();
        System.out.println("== 4. 账号隔离 ==");
        players.switchPlayer(playerB);
        check("切换后文件改为 playerB_records.json",
                "playerB_records.json".equals(records.getRecordsFile().getFileName().toString()));
        check("B 账号读不到 A 的战绩（0 场）", records.getTotalGames() == 0);

        records.addRecord(GameRecordService.GAME_PDK, true, 100, 50, GameRecordService.OPPONENT_AI);
        check("B 写入后为 1 场", records.getTotalGames() == 1);
        JsonNode aAgain = JsonCodec.mapper().readTree(fileA.toFile());
        check("A 的记录文件仍是 3 条（未被污染）", aAgain.size() == 3);

        System.out.println();
        System.out.println("== 结果：通过 " + passed + " / 失败 " + failed + " ==");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  [PASS] " + label);
        } else {
            failed++;
            System.out.println("  [FAIL] " + label);
        }
    }
}
