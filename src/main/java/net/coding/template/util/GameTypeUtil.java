package net.coding.template.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class GameTypeUtil {
    private static final Map<String, String> GAME_MAP = new HashMap<>();

    static {
        GAME_MAP.put("DELTA", "三角洲行动");
        GAME_MAP.put("AREA18", "暗区突围");
        GAME_MAP.put("TARKOV", "逃离塔科夫");
    }

    public String getGameName(String gameCode) {
        return GAME_MAP.getOrDefault(gameCode, "未知游戏");
    }

    public boolean isValidGameType(String gameCode) {
        return GAME_MAP.containsKey(gameCode);
    }
}
