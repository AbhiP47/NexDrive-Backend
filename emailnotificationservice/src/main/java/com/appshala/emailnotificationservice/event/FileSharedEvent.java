package com.appshala.emailnotificationservice.event;


public record FileSharedEvent(
        String recipientUserId,
        String sharerName,
        String sharerId,
        String fileName,
        String fileType,
        String accessType,
        String fileId,
        String activityTimestamp
) {
}
