package com.example.ssd.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(redisConnectionFactory);
//
//        // 使用StringRedisSerializer来序列化key
//        template.setKeySerializer(new StringRedisSerializer());
//
//        // 使用Jackson2JsonRedisSerializer来序列化value
//        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
//        ObjectMapper om = new ObjectMapper();
//        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//        jackson2JsonRedisSerializer.setObjectMapper(om);
//        template.setValueSerializer(jackson2JsonRedisSerializer);
//
//        // 同样的，设置hash key 和 hash value 的序列化器
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(jackson2JsonRedisSerializer);
//
//        template.afterPropertiesSet();
//        return template;
//    }
//
//    @Bean
//    public RedissonClient redissonClient() {
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://111.229.106.212:6379").setPassword("Shuai3198@");
//        return Redisson.create(config);
//    }

}
