package com.skillserver.service.model;

public record DownloadPayload(
    String fileName,
    byte[] bytes
) {
}
