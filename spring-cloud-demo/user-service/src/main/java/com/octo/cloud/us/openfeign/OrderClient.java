package com.octo.cloud.us.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "order-service")
public interface OrderClient {
    @GetMapping("/order")
    String getOrder();
}
