package com.winthier.hostile;

import java.util.HashMap;
import java.util.Map;

enum Wave {
    VANILLA;

    Map<MobType, Integer> getMobTypes() {
        Map<MobType, Integer> result = new HashMap<>();
        switch (this) {
        case VANILLA: default:
            for (VanillaMob type: VanillaMob.values()) {
                result.put(type, 1);
            }
        }
        return result;
    }
}
