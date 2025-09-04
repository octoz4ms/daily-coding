package com.octo.ssd.controller;


import com.octo.ssd.entity.User;
import com.octo.ssd.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author zms
 * @since 2024-04-25
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private IUserService userService;

    @GetMapping("{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:user')")
    public User findUserById(@PathVariable String id) {
        System.out.println(SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        return userService.getById(id);
    }
}
