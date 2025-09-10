package com.octo.ssd.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author author
 * @since 2025-09-09
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @GetMapping("/{id}")
    public String getUser(@PathVariable String id) {
        return "get user " + id;
    }
}
