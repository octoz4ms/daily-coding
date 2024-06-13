package com.example.ssd.controller;


import com.example.ssd.entity.User;
import com.example.ssd.service.IUserService;
import com.example.ssd.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author zms
 * @since 2024-05-30
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @GetMapping("/{id}")
    public ApiResponse<User> getUser(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        int a = 1 / 0;
        return ApiResponse.success(user);
    }
}
