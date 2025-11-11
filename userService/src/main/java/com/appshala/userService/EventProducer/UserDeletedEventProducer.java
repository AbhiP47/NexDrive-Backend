package com.appshala.userService.EventProducer;

import com.appshala.userService.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserDeletedEventProducer {

    @Value("${kafka.topics.user-deletion}")
    private  String USER_DELETION_TOPIC;

    private final KafkaTemplate<String,Object> kafkaTemplate;

    public void publishUserDeletedEvent(UserDeletedEvent event){
        UUID id = event.userId();
         log.info("Publishing UserDeletedEvent for user Id : {}",id);

         kafkaTemplate.send(USER_DELETION_TOPIC,id.toString(),event)
                 .whenComplete((result ,ex)->{
                     if(ex==null){
                         log.info("KAFKA: Successfully published UserDeletedEvent for ID {} to partition {} with offset {}",
                                 result.getRecordMetadata().partition(),result.getRecordMetadata().offset());
                     }
                     else{
                         log.error("KAFKA ERROR: Failed to publish UserDeletedEvent for ID {}: {}", id, ex.getMessage());
                     }
                 });
    }
}
