package com.octo.shorturl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 短链接系统启动类
 * 
 * 核心技术点：
 * 1. 短码生成算法 - 发号器 + 62进制转换
 * 2. 布隆过滤器 - 快速判断短码是否存在
 * 3. 高并发读写 - 缓存 + 数据库
 * 4. 数据统计分析 - 访问日志 + 统计报表
 * 
 * @author octo
 */
@SpringBootApplication
@MapperScan("com.octo.shorturl.mapper")
@EnableAsync
@EnableScheduling
public class ShortUrlApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortUrlApplication.class, args);
    }
}

