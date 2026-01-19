package com.octo.seckill.controller;

import com.octo.seckill.annotation.RateLimit;
import com.octo.seckill.common.Result;
import com.octo.seckill.dto.SeckillRequest;
import com.octo.seckill.entity.SeckillActivity;
import com.octo.seckill.mapper.SeckillActivityMapper;
import com.octo.seckill.service.SeckillService;
import com.octo.seckill.service.StockCacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 秒杀控制器
 * 
 * API接口：
 * - POST /api/seckill/do       执行秒杀
 * - GET  /api/seckill/result   查询秒杀结果
 * - GET  /api/seckill/activities 查询秒杀活动列表
 * - GET  /api/seckill/stock/{activityId} 查询库存
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;
    private final SeckillActivityMapper activityMapper;
    private final StockCacheService stockCacheService;

    /**
     * 执行秒杀
     * 
     * 限流配置：每秒1000次请求，超时500ms
     */
    @PostMapping("/do")
    @RateLimit(permitsPerSecond = 1000, timeout = 500)
    public Result<String> doSeckill(@Valid @RequestBody SeckillRequest request) {
        return seckillService.doSeckill(request);
    }

    /**
     * 查询秒杀结果
     */
    @GetMapping("/result")
    public Result<String> getSeckillResult(
            @RequestParam Long userId,
            @RequestParam Long activityId) {
        return seckillService.getSeckillResult(userId, activityId);
    }

    /**
     * 查询秒杀活动列表
     */
    @GetMapping("/activities")
    public Result<List<SeckillActivity>> getActivities() {
        List<SeckillActivity> activities = activityMapper.selectList(null);
        return Result.success(activities);
    }

    /**
     * 查询库存
     */
    @GetMapping("/stock/{activityId}")
    public Result<Map<String, Integer>> getStock(@PathVariable Long activityId) {
        Map<String, Integer> stockInfo = new HashMap<>();
        
        // Redis库存
        stockInfo.put("redisStock", stockCacheService.getStock(activityId));
        
        // 数据库库存
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity != null) {
            stockInfo.put("dbStock", activity.getAvailableStock());
        }
        
        return Result.success(stockInfo);
    }
}

