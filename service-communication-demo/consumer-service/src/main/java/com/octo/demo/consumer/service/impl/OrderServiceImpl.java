package com.octo.demo.consumer.service.impl;

import com.octo.demo.common.client.UserClient;
import com.octo.demo.common.dto.OrderDTO;
import com.octo.demo.common.dto.Result;
import com.octo.demo.common.dto.UserDTO;
import com.octo.demo.consumer.service.OrderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单服务实现类
 * <p>
 * 演示三种常见的服务间 HTTP 调用方式：
 * 1. OpenFeign
 * 2. RestTemplate
 * 3. WebClient
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final UserClient userClient;
    private final RestTemplate restTemplate;
    private final WebClient providerWebClient;

    @Value("${provider.service.url}")
    private String providerServiceUrl;

    @Value("${deployment.mode:local}")
    private String deploymentMode;

    private final Map<Long, OrderDTO> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @PostConstruct
    public void init() {
        log.info("初始化订单模拟数据...");
        createOrder(OrderDTO.builder()
                .userId(1L)
                .productName("MacBook Pro")
                .amount(new BigDecimal("12999.00"))
                .status(1)
                .build());
        createOrder(OrderDTO.builder()
                .userId(2L)
                .productName("iPhone 15 Pro")
                .amount(new BigDecimal("8999.00"))
                .status(1)
                .build());
        createOrder(OrderDTO.builder()
                .userId(1L)
                .productName("AirPods Pro")
                .amount(new BigDecimal("1999.00"))
                .status(2)
                .build());
        log.info("订单模拟数据初始化完成，共 {} 条", orderStore.size());
    }

    @Override
    public OrderDTO getOrderWithFeign(Long orderId) {
        log.info("【OpenFeign】获取订单，ID: {}", orderId);
        OrderDTO order = getOrderSnapshot(orderId);
        if (order == null) {
            return null;
        }

        Result<UserDTO> result = userClient.getUserById(order.getUserId());
        if (result.isSuccess() && result.getData() != null) {
            order.setUser(result.getData());
            log.info("【OpenFeign】成功获取用户信息: {}", result.getData().getUsername());
        } else {
            log.warn("【OpenFeign】获取用户信息失败: {}", result.getMessage());
        }
        return order;
    }

    @Override
    public OrderDTO getOrderWithRestTemplate(Long orderId) {
        log.info("【RestTemplate】获取订单，ID: {}", orderId);
        OrderDTO order = getOrderSnapshot(orderId);
        if (order == null) {
            return null;
        }

        try {
            String url = providerServiceUrl + "/api/users/" + order.getUserId();
            ResponseEntity<Result<UserDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            Result<UserDTO> result = response.getBody();
            if (result != null && result.isSuccess() && result.getData() != null) {
                order.setUser(result.getData());
                log.info("【RestTemplate】成功获取用户信息: {}", result.getData().getUsername());
            }
        } catch (Exception e) {
            log.error("【RestTemplate】调用用户服务失败: {}", e.getMessage());
        }
        return order;
    }

    @Override
    public OrderDTO getOrderWithWebClient(Long orderId) {
        log.info("【WebClient】获取订单，ID: {}", orderId);
        OrderDTO order = getOrderSnapshot(orderId);
        if (order == null) {
            return null;
        }

        try {
            Result<UserDTO> result = providerWebClient.get()
                    .uri("/api/users/{id}", order.getUserId())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<UserDTO>>() {})
                    .block();

            if (result != null && result.isSuccess() && result.getData() != null) {
                order.setUser(result.getData());
                log.info("【WebClient】成功获取用户信息: {}", result.getData().getUsername());
            }
        } catch (Exception e) {
            log.error("【WebClient】调用用户服务失败: {}", e.getMessage());
        }
        return order;
    }

    @Override
    public Map<String, Object> compareCommunicationMethods() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recommended", "OpenFeign");
        result.put("methods", List.of(
                Map.of(
                        "name", "OpenFeign",
                        "type", "声明式 HTTP 调用",
                        "bestFor", "标准微服务 RPC/HTTP 调用",
                        "advantages", List.of("接口化调用", "代码简洁", "支持降级", "便于统一治理"),
                        "limitations", List.of("本质仍是 HTTP", "依赖 Spring Cloud 生态")
                ),
                Map.of(
                        "name", "RestTemplate",
                        "type", "编程式同步 HTTP 调用",
                        "bestFor", "老项目或需要完全控制请求细节",
                        "advantages", List.of("灵活", "易理解", "适合传统同步代码"),
                        "limitations", List.of("模板代码较多", "维护成本更高")
                ),
                Map.of(
                        "name", "WebClient",
                        "type", "响应式 HTTP 调用",
                        "bestFor", "高并发、异步链路、响应式项目",
                        "advantages", List.of("非阻塞", "适合高吞吐", "链式 API"),
                        "limitations", List.of("学习成本更高", "阻塞项目中优势不明显")
                ),
                Map.of(
                        "name", "MQ",
                        "type", "异步消息通信",
                        "bestFor", "解耦、削峰、最终一致性",
                        "advantages", List.of("异步解耦", "削峰填谷", "容错性好"),
                        "limitations", List.of("不是实时同步返回", "需要消息中间件")
                )
        ));
        return result;
    }

    @Override
    public Map<String, Object> compareDeploymentScenarios() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentDeploymentMode", deploymentMode);
        result.put("providerServiceUrl", providerServiceUrl);
        result.put("localDeployment", Map.of(
                "networkPath", "localhost 或 127.0.0.1",
                "characteristics", List.of("链路短", "延迟低", "便于调试", "常用固定端口直接调用"),
                "risks", List.of("只能本机访问", "不适合多机扩展")
        ));
        result.put("crossServerDeployment", Map.of(
                "networkPath", "目标服务器 IP、域名、内网地址、网关地址",
                "characteristics", List.of("经过真实网络", "需开放端口或走网关", "需考虑防火墙、DNS、证书"),
                "risks", List.of("网络抖动", "超时与重试", "安全认证", "跨机房延迟")
        ));
        result.put("coreDifferences", List.of(
                "本地调用通常写 localhost，跨服务器必须写可达的 IP/域名。",
                "本地问题多是端口冲突或服务没启动，跨服务器还要排查网络、路由、防火墙、安全组。",
                "跨服务器场景更需要网关、注册中心、负载均衡、限流、熔断和链路追踪。",
                "跨服务器场景通常不能依赖硬编码地址，建议使用配置中心或服务发现。"
        ));
        result.put("suggestion", "开发环境可用固定 URL，本地用 localhost；测试/生产环境建议通过配置或注册中心调用。 ");
        return result;
    }

    @Override
    public List<OrderDTO> listOrders() {
        return new ArrayList<>(orderStore.values());
    }

    @Override
    public OrderDTO createOrder(OrderDTO order) {
        Long id = idGenerator.incrementAndGet();
        order.setId(id);
        order.setOrderNo("ORD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCreateTime(LocalDateTime.now());
        orderStore.put(id, order);
        log.info("创建订单成功，ID: {}, 订单号: {}", id, order.getOrderNo());
        return order;
    }

    private OrderDTO getOrderSnapshot(Long orderId) {
        OrderDTO order = orderStore.get(orderId);
        if (order == null) {
            return null;
        }
        return OrderDTO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .productName(order.getProductName())
                .amount(order.getAmount())
                .status(order.getStatus())
                .createTime(order.getCreateTime())
                .user(order.getUser())
                .build();
    }
}
