package com.example.product.controller;

import com.example.feign.client.UserFeignClient;
import com.example.common.entity.Product;
import com.example.common.entity.User;
import com.example.common.result.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    public UserFeignClient userFeignClient;

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getProductWithUser() {
        Product product = new Product(1L, "手机壳");
        Result<User> userResult = userFeignClient.getUserById(1L);

        HashMap<String, Object> result = new HashMap<>();
        result.put("order", product);
        result.put("user", userResult.getData());
        return Result.success(result);
    }
}
