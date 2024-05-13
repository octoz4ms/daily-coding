package com.octo.cssb;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({HelloProperties.class})
public class HelloConfiguration {

    @Bean
    public HelloService helloService(HelloProperties helloProperties) {
        return new HelloService(helloProperties.getName());
    }
}
