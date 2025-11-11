package com.appshala.emailnotificationservice.event;

public record GenericActivityEvent(
        String recipientUserId,
        String activityType,
        String messageDetail,
        String sourceObjectId
) {
}
