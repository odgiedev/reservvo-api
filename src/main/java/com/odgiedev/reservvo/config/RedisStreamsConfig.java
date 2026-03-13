package com.odgiedev.reservvo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import com.odgiedev.reservvo.service.NotificationService;

import java.time.Duration;

@Slf4j
@Configuration
public class RedisStreamsConfig {

    public static final String STREAM_KEY = "reservvo:notifications";
    public static final String DLQ_KEY = "reservvo:notifications:dlq";
    public static final String CONSUMER_GROUP = "notification-group";
    public static final String CONSUMER_NAME = "notification-consumer-1";
    public static final int MAX_RETRY_ATTEMPTS = 2;

    @Bean
    public RedisTemplate<String, String> redisStreamTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer serializer = new StringRedisSerializer();

        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamListenerContainer(
            RedisConnectionFactory factory,
            StreamListener<String, MapRecord<String, String, String>> streamListener,
            NotificationService notificationService,
            @Qualifier("redisStreamTemplate") RedisTemplate<String, String> redisTemplate) {

        // cria o stream e consumer group se não existirem
        initializeStream(redisTemplate);

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(factory, options);

        Subscription subscription = container.receive(
                org.springframework.data.redis.connection.stream.Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
                notificationService
        );

        container.start();
        return container;
    }

    private void initializeStream(RedisTemplate<String, String> redisTemplate) {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0-0"), CONSUMER_GROUP);
            log.info("Consumer group '{}' criado no stream '{}'", CONSUMER_GROUP, STREAM_KEY);
        } catch (Exception e) {
            //
        }
    }
}