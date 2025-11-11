package com.appshala.emailnotificationservice.serviceImpl;

import com.appshala.emailnotificationservice.event.GenericActivityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class InAppNotificationService {

    private final ConcurrentMap<String, String> notificationStore = new ConcurrentHashMap<>();

    public void processGenericActivity(GenericActivityEvent event) {
        log.info("Processing In-App Notification for User {}. Type: {}", event.recipientUserId(), event.activityType());
        String notificationId = event.sourceObjectId() + "-" + event.activityType() + "-" + Instant.now().toEpochMilli();

        // 1. Persistence (for Notification History Center)
        // Store the detailed notification message.
        String persistedMessage = String.format("[%s] %s: %s (Source: %s)",
                Instant.now(), event.activityType(), event.messageDetail(), event.sourceObjectId());

        notificationStore.put(notificationId, persistedMessage);
        log.debug("Saved to persistence store: {}", persistedMessage);

        // 2. Real-time Push (Simulated)
        // In a live system, this would push a message to a WebSocket/STOMP topic:
        // messagingTemplate.convertAndSend("/topic/user/" + event.recipientUserId(), persistedMessage);
        log.info("PUSHED REAL-TIME Notification to WebSocket channel for user: {}", event.recipientUserId());
    }

}
