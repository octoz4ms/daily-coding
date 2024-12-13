package com.octo.cloud.us.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.octo.cloud.us.openfeign.OrderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @GetMapping("/limit/{id}")
    @SentinelResource(value = "limit")
    public String limit(@PathVariable int id) {
        return "用户id：" + id;
    }


    @GetMapping("/refuse/{id}")
    @SentinelResource(value = "refuse")
    public String refuse(@PathVariable int id) throws InterruptedException {
        Thread.sleep(60);
        return "用户id：" + id;
    }

    public String handleFallback(int id) {
        return "熔断！";
    }

    public String handleBlockHandler(int id, BlockException ex) {
        ex.printStackTrace();
        return "限流！";
    }
}
