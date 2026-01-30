package com.example.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.dto.request.CreateOrderRequest;
import com.example.payment.dto.response.OrderResponse;
import com.example.payment.entity.Order;
import com.example.payment.entity.Product;
import com.example.payment.enums.OrderStatus;
import com.example.payment.exception.BusinessException;
import com.example.payment.mapper.OrderMapper;
import com.example.payment.mapper.ProductMapper;
import com.example.payment.mq.OrderMessageProducer;
import com.example.payment.service.OrderService;
import com.example.payment.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OrderMessageProducer orderMessageProducer;

    @Value("${order.timeout-minutes:30}")
    private Integer orderTimeoutMinutes;

    @Value("${order.prefix:ORD}")
    private String orderPrefix;

    private static final String SUBMIT_TOKEN_KEY = "order:submit:token:";
    private static final String ORDER_LOCK_KEY = "order:lock:";

    @Override
    public String generateSubmitToken(Long userId) {
        // 生成唯一令牌
        String token = IdUtil.fastSimpleUUID();
        String key = SUBMIT_TOKEN_KEY + userId + ":" + token;
        
        // 存入Redis，5分钟有效
        redisTemplate.opsForValue().set(key, "1", 5, TimeUnit.MINUTES);
        
        log.info("生成提交令牌，userId: {}, token: {}", userId, token);
        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("开始创建订单，userId: {}, productId: {}", request.getUserId(), request.getProductId());

        // 1. 验证并消费提交令牌（防重复提交）
        String tokenKey = SUBMIT_TOKEN_KEY + request.getUserId() + ":" + request.getSubmitToken();
        Boolean deleted = redisTemplate.delete(tokenKey);
        if (deleted == null || !deleted) {
            throw new BusinessException("请勿重复提交订单");
        }

        // 2. 查询商品信息
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!product.isOnSale()) {
            throw new BusinessException("商品已下架");
        }

        // 3. 检查库存
        if (!stockService.checkStock(request.getProductId(), request.getQuantity())) {
            throw new BusinessException("库存不足");
        }

        // 4. 锁定库存
        boolean lockResult = stockService.lockStock(request.getProductId(), request.getQuantity());
        if (!lockResult) {
            throw new BusinessException("锁定库存失败，库存不足");
        }

        try {
            // 5. 计算订单金额
            BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            BigDecimal discountAmount = BigDecimal.ZERO; // 简化处理，实际需要查询优惠券
            BigDecimal freightAmount = BigDecimal.ZERO;  // 简化处理，实际需要计算运费
            BigDecimal payAmount = totalAmount.subtract(discountAmount).add(freightAmount);

            // 6. 生成订单号
            String orderNo = generateOrderNo();

            // 7. 创建订单
            Order order = Order.builder()
                    .orderNo(orderNo)
                    .userId(request.getUserId())
                    .productId(request.getProductId())
                    .productName(product.getName())
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalAmount(totalAmount)
                    .discountAmount(discountAmount)
                    .freightAmount(freightAmount)
                    .payAmount(payAmount)
                    .status(OrderStatus.PENDING_PAYMENT.getCode())
                    .receiverName(request.getReceiverName())
                    .receiverPhone(request.getReceiverPhone())
                    .receiverAddress(request.getReceiverAddress())
                    .remark(request.getRemark())
                    .expireTime(LocalDateTime.now().plusMinutes(orderTimeoutMinutes))
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .deleted(0)
                    .build();

            orderMapper.insert(order);

            log.info("订单创建成功，orderNo: {}, payAmount: {}", orderNo, payAmount);

            // 8. 发送延迟消息（订单超时自动取消）
            orderMessageProducer.sendOrderTimeoutMessage(orderNo, orderTimeoutMinutes);

            return OrderResponse.fromEntity(order);

        } catch (Exception e) {
            // 回滚库存
            stockService.releaseStock(request.getProductId(), request.getQuantity());
            log.error("创建订单失败，已回滚库存", e);
            throw new BusinessException("创建订单失败：" + e.getMessage());
        }
    }

    @Override
    public OrderResponse getOrderByOrderNo(String orderNo) {
        Order order = getEntityByOrderNo(orderNo);
        return OrderResponse.fromEntity(order);
    }

    @Override
    public Order getEntityByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        return orderMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String orderNo, String reason) {
        log.info("取消订单，orderNo: {}, reason: {}", orderNo, reason);

        Order order = getEntityByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (currentStatus == null || !currentStatus.canCancel()) {
            throw new BusinessException("当前订单状态不允许取消");
        }

        // 更新订单状态
        int updated = orderMapper.updateStatusByOrderNo(
                orderNo,
                OrderStatus.CANCELLED.getCode(),
                OrderStatus.PENDING_PAYMENT.getCode()
        );

        if (updated > 0) {
            // 释放库存
            stockService.releaseStock(order.getProductId(), order.getQuantity());
            
            // 更新取消原因和时间
            Order updateOrder = new Order();
            updateOrder.setId(order.getId());
            updateOrder.setCancelReason(reason);
            updateOrder.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(updateOrder);

            log.info("订单取消成功，orderNo: {}", orderNo);
            return true;
        }

        log.warn("订单取消失败，可能已被处理，orderNo: {}", orderNo);
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaySuccess(String orderNo) {
        log.info("处理支付成功，orderNo: {}", orderNo);

        Order order = getEntityByOrderNo(orderNo);
        if (order == null) {
            log.warn("订单不存在，orderNo: {}", orderNo);
            return false;
        }

        // 更新订单状态
        int updated = orderMapper.updateStatusByOrderNo(
                orderNo,
                OrderStatus.PAID.getCode(),
                OrderStatus.PENDING_PAYMENT.getCode()
        );

        if (updated > 0) {
            // 扣减库存（将锁定库存转为已售）
            stockService.deductStock(order.getProductId(), order.getQuantity());

            // 更新支付时间
            Order updateOrder = new Order();
            updateOrder.setId(order.getId());
            updateOrder.setPayTime(LocalDateTime.now());
            orderMapper.updateById(updateOrder);

            log.info("订单支付成功处理完成，orderNo: {}", orderNo);
            
            // TODO: 发送支付成功通知、触发发货流程等
            
            return true;
        }

        log.warn("订单状态更新失败，可能已被处理，orderNo: {}", orderNo);
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderTimeout(String orderNo) {
        log.info("处理订单超时，orderNo: {}", orderNo);

        Order order = getEntityByOrderNo(orderNo);
        if (order == null) {
            log.warn("订单不存在，orderNo: {}", orderNo);
            return;
        }

        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        
        // 只有待支付状态的订单才需要超时处理
        if (currentStatus != OrderStatus.PENDING_PAYMENT) {
            log.info("订单已不是待支付状态，无需处理，orderNo: {}, status: {}", orderNo, currentStatus);
            return;
        }

        // 更新订单状态为已关闭
        int updated = orderMapper.updateStatusByOrderNo(
                orderNo,
                OrderStatus.CLOSED.getCode(),
                OrderStatus.PENDING_PAYMENT.getCode()
        );

        if (updated > 0) {
            // 释放库存
            stockService.releaseStock(order.getProductId(), order.getQuantity());

            // 更新取消原因和时间
            Order updateOrder = new Order();
            updateOrder.setId(order.getId());
            updateOrder.setCancelReason("订单超时自动关闭");
            updateOrder.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(updateOrder);

            log.info("订单超时关闭成功，orderNo: {}", orderNo);
            
            // TODO: 释放优惠券、积分等
        }
    }

    /**
     * 生成订单号
     * 格式：前缀 + 日期 + 6位序列号
     */
    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
        return orderPrefix + date + random;
    }
}

