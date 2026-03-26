package com.skillserver.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class IdGenerator {

    private static final DateTimeFormatter REVISION_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private IdGenerator() {
    }

    public static String skillUid() {
        return "sk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static String revision() {
        return "rev_" + LocalDateTime.now().format(REVISION_FORMAT) + "_" +
            UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
