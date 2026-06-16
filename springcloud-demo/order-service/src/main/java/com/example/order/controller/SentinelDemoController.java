package com.example.order.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.common.result.Result;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sentinel 流控效果演示 Controller
 *
 * <p>覆盖 Sentinel 常见的 5 种流控效果：
 * <ul>
 *   <li>1. QPS 直接拒绝 - {@code GET /sentinel-demo/qps}</li>
 *   <li>2. 线程数拒绝 - {@code GET /sentinel-demo/thread}</li>
 *   <li>3. 关联资源（读、写互斥）- {@code GET /sentinel-demo/write}, {@code GET /sentinel-demo/read}</li>
 *   <li>4. 链路限流（只针对来自特定入口的流量）- {@code GET /sentinel-demo/link/{flag}}</li>
 *   <li>5. 热点参数限流 - {@code GET /sentinel-demo/hot/{id}}</li>
 *   <li>6. 冷启动（Warm Up）- {@code GET /sentinel-demo/warmup}</li>
 *   <li>7. 排队等待 - {@code GET /sentinel-demo/queue}</li>
 * </ul>
 *
 * <p>对应的流控规则在 {@link com.example.order.config.SentinelRuleInitializer} 中以代码方式声明，
 * 服务启动后自动注册到 Dashboard，也支持压测时直接生效（无需依赖 Dashboard）。
 */
@Slf4j
@RestController
@RequestMapping("/sentinel-demo")
public class SentinelDemoController {

    private final AtomicInteger threadCounter = new AtomicInteger(0);

    /**
     * 1. QPS 限流：每秒最多 5 次请求，超过直接拒绝。
     * 压测命令：wrk -t4 -c20 -d10s http://localhost:8082/sentinel-demo/qps
     */
    @GetMapping("/qps")
    @SentinelResource(value = "demo:qps", blockHandler = "qpsBlockHandler", fallback = "qpsFallback")
    public Result<String> qps() {
        return Result.success("QPS 限流演示 - 业务正常返回");
    }

    public Result<String> qpsBlockHandler(BlockException ex) {
        log.warn("[Sentinel] /qps 被限流, rule={}", ex.getRule());
        return Result.fail(429, "QPS 触发限流（每秒最多 5 次），请稍后重试");
    }

    public Result<String> qpsFallback(Throwable ex) {
        log.error("[Sentinel] /qps 业务异常降级, ex={}", ex.getMessage());
        return Result.fail(500, "QPS 接口业务异常降级: " + ex.getMessage());
    }

    /**
     * 2. 线程数限流：同时最多 2 个线程处理，否则排队/拒绝。
     * 由于方法体内会 sleep 500ms 模拟慢调用，并发请求时容易触发线程数限制。
     * 压测命令：ab -n 50 -c 10 http://localhost:8082/sentinel-demo/thread
     */
    @GetMapping("/thread")
    @SentinelResource(value = "demo:thread", blockHandler = "threadBlockHandler")
    public Result<String> thread() throws InterruptedException {
        int current = threadCounter.incrementAndGet();
        log.info("[Sentinel] /thread 占用线程, 当前并发线程数={}", current);
        try {
            Thread.sleep(500);
            return Result.success("线程数限流演示 - 业务正常返回");
        } finally {
            threadCounter.decrementAndGet();
        }
    }

    public Result<String> threadBlockHandler(BlockException ex) {
        log.warn("[Sentinel] /thread 线程数超限被拒绝, rule={}", ex.getRule());
        return Result.fail(429, "线程数触发限流（最大并发 2），请稍后重试");
    }

    /**
     * 3a. 关联资源 - 写接口。
     * 关联规则：写接口为高优先级资源，读接口为关联资源；
     * 当写接口 QPS > 1 时，自动拒绝读接口。
     */
    @GetMapping("/write")
    @SentinelResource(value = "demo:write", blockHandler = "writeBlockHandler")
    public Result<String> write() {
        return Result.success("写接口执行成功");
    }

    public Result<String> writeBlockHandler(BlockException ex) {
        return Result.fail(429, "写接口被限流");
    }

    /**
     * 3b. 关联资源 - 读接口（被写接口关联影响）。
     * 压测脚本：先高频压 write，再观察 read。
     * <pre>
     * # 终端1 - 持续压测 write
     * while true; do curl -s http://localhost:8082/sentinel-demo/write > /dev/null; done
     * # 终端2 - 压测 read
     * for i in {1..20}; do curl -s http://localhost:8082/sentinel-demo/read; echo; done
     * </pre>
     */
    @GetMapping("/read")
    @SentinelResource(value = "demo:read", blockHandler = "readBlockHandler")
    public Result<String> read() {
        return Result.success("读接口执行成功");
    }

    public Result<String> readBlockHandler(BlockException ex) {
        return Result.fail(429, "读接口因【写接口】关联影响被限流");
    }

    /**
     * 4. 链路限流：仅针对来自 demo:link-A 入口的流量进行限流，
     * 来自 demo:link-B 入口的同源调用不受影响。
     * <p>需要在 application.yaml 中设置 {@code spring.cloud.sentinel.web-context-unify: false}，
     * 否则不同 URL 会被合并到同一个 Context，链路限流会失效（已配置）。
     * <p>链路模式关键：通过 {@link ContextUtil#enter(String, String)} 为入口命名，规则针对 origin 做限流。
     */
    @GetMapping("/link/{flag}")
    public Result<String> link(@PathVariable("flag") String flag) {
        // 入口名固定为 demo:linkEntry；origin 根据 flag 区分（linkA / linkB）
        ContextUtil.enter("demo:linkEntry", flag);
        try {
            return commonService(flag);
        } finally {
            ContextUtil.exit();
        }
    }

    /**
     * 公共服务 - 内部会进入名为 "demo:commonService" 的资源。
     * 链路限流规则只针对来自 demo:link-A 入口的调用生效。
     */
    @SentinelResource(value = "demo:commonService", blockHandler = "commonServiceBlockHandler")
    public Result<String> commonService(String flag) {
        return Result.success("commonService 被 flag=" + flag + " 调用成功");
    }

    public Result<String> commonServiceBlockHandler(String flag, BlockException ex) {
        return Result.fail(429, "链路限流：flag=" + flag + " 调用 commonService 被拒绝");
    }

    /**
     * 5. 热点参数限流：针对 id 参数进行限流。
     * 规则：默认 QPS 阈值 5；当 id=1 时阈值 100（白名单），id=2 时阈值 1（黑名单严格限流）。
     * 压测命令：hey -n 1000 -c 50 http://localhost:8082/sentinel-demo/hot/1
     */
    @GetMapping("/hot/{id}")
    @SentinelResource(value = "demo:hot", blockHandler = "hotBlockHandler")
    public Result<String> hot(@PathVariable("id") Long id) {
        return Result.success("热点参数限流演示 - id=" + id);
    }

    public Result<String> hotBlockHandler(Long id, BlockException ex) {
        return Result.fail(429, "热点参数限流触发：id=" + id);
    }

    /**
     * 6. 冷启动（Warm Up）：系统刚启动时 QPS 阈值较低，随后逐渐升至最大阈值。
     * 适用场景：秒杀系统冷启动、数据库连接池预热。
     */
    @GetMapping("/warmup")
    @SentinelResource(value = "demo:warmup", blockHandler = "warmupBlockHandler")
    public Result<String> warmup() {
        return Result.success("冷启动限流演示 - 已通过");
    }

    public Result<String> warmupBlockHandler(BlockException ex) {
        return Result.fail(429, "冷启动限流：系统正在预热，请稍后重试");
    }

    /**
     * 7. 排队等待：匀速通过，让请求以稳定间隔处理。
     * 适用场景：消息消费、平滑削峰。
     */
    @GetMapping("/queue")
    @SentinelResource(value = "demo:queue", blockHandler = "queueBlockHandler")
    public Result<String> queue() {
        return Result.success("排队等待限流演示 - 业务正常返回");
    }

    public Result<String> queueBlockHandler(BlockException ex) {
        return Result.fail(429, "排队等待超时：超过最大等待时间，请稍后重试");
    }

    /**
     * 编程式 API 使用示例（不依赖注解）。
     * 对应资源名 {@code demo:programmatic}，规则通过 {@code SentinelRuleInitializer} 注册。
     */
    @GetMapping("/programmatic")
    public Result<String> programmatic() {
        try (Entry entry = SphU.entry("demo:programmatic")) {
            return Result.success("编程式 API 流控演示 - 业务正常返回");
        } catch (BlockException ex) {
            log.warn("[Sentinel] /programmatic 被限流, rule={}", ex.getRule());
            Map<String, Object> data = new HashMap<>();
            data.put("rule", String.valueOf(ex.getRule()));
            return Result.fail(429, "编程式 API 触发限流");
        }
    }

    /**
     * ContextUtil 用法示例 - 用于链路追踪和来源标记。
     */
    @GetMapping("/context/{source}")
    public Result<String> context(@PathVariable("source") String source) {
        ContextUtil.enter("demo:context", source);
        try (Entry entry = SphU.entry("demo:context")) {
            return Result.success("Context 演示 - source=" + source);
        } catch (BlockException ex) {
            return Result.fail(429, "Context 流控：source=" + source + " 被限流");
        } finally {
            ContextUtil.exit();
        }
    }

    @PostConstruct
    public void logHelp() {
        log.info("\n" +
                "================ Sentinel 流控演示已就绪 ================\n" +
                " 接口清单（已注册流控规则）：\n" +
                "  1. QPS 限流        : GET /sentinel-demo/qps          (QPS=5)\n" +
                "  2. 线程数限流      : GET /sentinel-demo/thread        (max=2)\n" +
                "  3. 关联资源        : GET /sentinel-demo/write, /read\n" +
                "  4. 链路限流        : GET /sentinel-demo/link/{flag}\n" +
                "  5. 热点参数        : GET /sentinel-demo/hot/{id}\n" +
                "  6. 冷启动          : GET /sentinel-demo/warmup        (WarmUp 10s)\n" +
                "  7. 排队等待        : GET /sentinel-demo/queue         (5 QPS)\n" +
                "  8. 编程式 API      : GET /sentinel-demo/programmatic  (QPS=3)\n" +
                "  9. Context 来源    : GET /sentinel-demo/context/{source}\n" +
                "==========================================================");
    }
}
