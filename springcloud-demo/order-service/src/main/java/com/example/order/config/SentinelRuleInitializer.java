package com.example.order.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sentinel 流控规则代码方式注册器
 *
 * <p>通过 {@link ApplicationReadyEvent} 在应用完全启动后加载规则到 Sentinel，
 * 此时 Sentinel transport 已就绪，Dashboard 可正常发现服务。
 * 无需依赖 Dashboard 即可立即生效；Dashboard 启动后会自动同步这些规则。
 *
 * <p>规则配置参考：
 * <ul>
 *   <li>{@link com.example.order.controller.SentinelDemoController} 中各接口对应的资源名</li>
 *   <li>Sentinel FlowRule 字段说明参见：
 *       <a href="https://sentinelguard.io/zh-cn/docs/flow-control.html">Sentinel 官方文档</a></li>
 * </ul>
 */
@Slf4j
@Component
public class SentinelRuleInitializer {

    @EventListener(ApplicationReadyEvent.class)
    public void initRules() {
        log.info("[Sentinel] 开始以代码方式注册演示流控规则 ...");
        loadFlowRules();
        loadParamFlowRules();
        log.info("[Sentinel] 演示流控规则注册完成，共注册 {} 条 FlowRule, {} 条 ParamFlowRule",
                FlowRuleManager.getRules().size(), ParamFlowRuleManager.getRules().size());
    }

    private void loadFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // ===== 1. QPS 限流：每秒最多 5 次 =====
        rules.add(buildFlowRule("demo:qps", RuleConstant.CONTROL_BEHAVIOR_DEFAULT, 5));

        // ===== 2. 线程数限流：最大并发 2 =====
        rules.add(buildFlowRule("demo:thread", RuleConstant.CONTROL_BEHAVIOR_DEFAULT, 2)
                .setGrade(RuleConstant.FLOW_GRADE_THREAD));

        // ===== 3. 关联资源限流 =====
        // 写接口：QPS 阈值 1，作为高优先级资源
        rules.add(buildFlowRule("demo:write", RuleConstant.CONTROL_BEHAVIOR_DEFAULT, 1));
        // 读接口：QPS 阈值 100（基本不限），但与写接口关联
        FlowRule readRule = buildFlowRule("demo:read", RuleConstant.CONTROL_BEHAVIOR_DEFAULT, 100);
        readRule.setRefResource("demo:write");
        rules.add(readRule);

        // ===== 4. 链路限流：CHAIN 策略 + limitApp=linkA =====
        // 仅对 origin=linkA 的调用限流；linkB 不受影响
        FlowRule linkRuleA = buildFlowRule("demo:commonService", RuleConstant.CONTROL_BEHAVIOR_DEFAULT, 1);
        linkRuleA.setStrategy(RuleConstant.STRATEGY_CHAIN);
        linkRuleA.setLimitApp("linkA");
        rules.add(linkRuleA);

        // ===== 5. 冷启动（Warm Up）：10 秒内逐渐放行到 QPS=10 =====
        FlowRule warmupRule = buildFlowRule("demo:warmup", RuleConstant.CONTROL_BEHAVIOR_WARM_UP, 10);
        warmupRule.setWarmUpPeriodSec(10);
        rules.add(warmupRule);

        // ===== 6. 排队等待（匀速通过）：5 QPS，超时 5s =====
        FlowRule queueRule = buildFlowRule("demo:queue", RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER, 5);
        queueRule.setMaxQueueingTimeMs(5_000);
        rules.add(queueRule);

        // ===== 7. 编程式 API：QPS=3 =====
        rules.add(buildFlowRule("demo:programmatic", RuleConstant.CONTROL_BEHAVIOR_DEFAULT, 3));

        // ===== 8. Context 来源限流：source=web 的 QPS=2 =====
        FlowRule contextRule = buildFlowRule("demo:context", RuleConstant.CONTROL_BEHAVIOR_DEFAULT, 2);
        contextRule.setLimitApp("web");
        rules.add(contextRule);

        FlowRuleManager.loadRules(rules);
        log.info("[Sentinel] FlowRule 注册: {}", rules);
    }

    /**
     * 热点参数限流：对 demo:hot 的第一个参数（id）进行精细化限流。
     * - 默认阈值 QPS=5
     * - id=1 阈值 100（白名单）
     * - id=2 阈值 1（黑名单严格限流）
     */
    private void loadParamFlowRules() {
        ParamFlowRule hotRule = new ParamFlowRule("demo:hot")
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setParamIdx(0)
                .setCount(5);

        // 参数例外项（构造器签名：String, Integer, String）
        List<ParamFlowItem> items = new ArrayList<>();
        items.add(new ParamFlowItem().setObject("1").setCount(100).setClassType("java.lang.Long"));   // id=1 阈值 100
        items.add(new ParamFlowItem().setObject("2").setCount(1).setClassType("java.lang.Long"));     // id=2 阈值 1（黑名单）
        hotRule.setParamFlowItemList(items);

        ParamFlowRuleManager.loadRules(Collections.singletonList(hotRule));
        log.info("[Sentinel] ParamFlowRule 注册: {}", hotRule);
    }

    private FlowRule buildFlowRule(String resource, int controlBehavior, int count) {
        return new FlowRule(resource)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setControlBehavior(controlBehavior)
                .setCount(count);
    }
}
