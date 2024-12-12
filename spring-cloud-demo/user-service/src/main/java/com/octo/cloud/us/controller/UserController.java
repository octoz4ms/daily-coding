package com.octo.cloud.us.controller;

import com.octo.cloud.us.openfeign.OrderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private OrderClient orderClient;

    @GetMapping("/hello")
    public void getUser() {
        System.out.println("----------------------");
//        String order = orderClient.getOrder();
//        log.info("用户zms:" + order);
    }
}
