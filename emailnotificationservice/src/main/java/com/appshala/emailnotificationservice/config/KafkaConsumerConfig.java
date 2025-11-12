package com.appshala.emailnotificationservice.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConsumerConfig {

    private final ConsumerFactory<String , Object> consumerFactory;
    private final KafkaTemplate kafkaTemplate;

    public KafkaConsumerConfig(ConsumerFactory<String,Object> consumerFactory, KafkaTemplate kafkaTemplate)
    {
        this.consumerFactory = consumerFactory;
        this.kafkaTemplate = kafkaTemplate;
    }

    private DefaultErrorHandler errorHandler(){
        ExponentialBackOff backOff = new ExponentialBackOff(1000,2);

        backOff.setMaxElapsedTime(60_000);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (r,e)->{
                    return new TopicPartition(r.topic() +".DLT", r.partition());
                }
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,backOff);
        return errorHandler;
    }

    // 2. Define the Custom Kafka Listener Container Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,Object> kafkaListenerContainerFactory()
    {
        ConcurrentKafkaListenerContainerFactory<String,Object> factory =
                 new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);// Process events concurrently with 3 threads
        // Set the custom error handler with retry/DLQ logic
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }
}
