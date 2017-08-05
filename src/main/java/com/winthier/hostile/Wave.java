package com.winthier.hostile;

import java.util.Map;
import java.util.HashMap;

enum Wave {
    VANILLA,
    ;

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
