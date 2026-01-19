package com.octo.shorturl.controller;

import com.octo.shorturl.common.Result;
import com.octo.shorturl.dto.CreateShortUrlRequest;
import com.octo.shorturl.entity.ShortUrl;
import com.octo.shorturl.service.AccessStatService;
import com.octo.shorturl.service.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * 短链接控制器
 * 
 * API接口：
 * - POST /api/short-url/create  创建短链接
 * - GET  /{shortCode}           访问短链接（302重定向）
 * - GET  /api/short-url/info    获取短链接信息
 * - GET  /api/short-url/stats   获取访问统计
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;
    private final AccessStatService statService;

    /**
     * 创建短链接
     */
    @PostMapping("/api/short-url/create")
    public Result<String> createShortUrl(@Valid @RequestBody CreateShortUrlRequest request) {
        String shortUrl = shortUrlService.createShortUrl(
                request.getLongUrl(), 
                request.getCreatorId(),
                request.getExpireTime()
        );
        return Result.success("创建成功", shortUrl);
    }

    /**
     * 访问短链接（302重定向）
     */
    @GetMapping("/{shortCode}")
    public void redirect(@PathVariable String shortCode, 
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        String longUrl = shortUrlService.getLongUrl(shortCode);
        
        if (longUrl == null || longUrl.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "短链接不存在或已过期");
            return;
        }

        // 异步记录访问日志
        statService.recordAccess(shortCode, request);

        // 302重定向
        response.sendRedirect(longUrl);
    }

    /**
     * 获取短链接信息
     */
    @GetMapping("/api/short-url/info")
    public Result<ShortUrl> getShortUrlInfo(@RequestParam String shortCode) {
        ShortUrl shortUrl = shortUrlService.getShortUrlInfo(shortCode);
        if (shortUrl == null) {
            return Result.notFound("短链接不存在");
        }
        return Result.success(shortUrl);
    }

    /**
     * 获取访问统计
     */
    @GetMapping("/api/short-url/stats")
    public Result<Map<String, Object>> getStats(@RequestParam String shortCode) {
        Map<String, Object> stats = statService.getStats(shortCode);
        return Result.success(stats);
    }

    /**
     * 禁用短链接
     */
    @PostMapping("/api/short-url/disable")
    public Result<Void> disableShortUrl(@RequestParam String shortCode) {
        shortUrlService.disableShortUrl(shortCode);
        return Result.success();
    }
}

