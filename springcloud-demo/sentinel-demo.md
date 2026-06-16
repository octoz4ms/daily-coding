# Sentinel 流控效果演示

本目录基于 `springcloud-demo` 演示 **Alibaba Sentinel** 的多种流控效果。所有规则均已通过代码方式
注册到 `SentinelRuleManager`，无需依赖 Dashboard 即可立即生效；Dashboard 启动后也会自动同步。

## 1. 演示覆盖的流控效果

| # | 效果 | 资源名 | 接口 | 关键参数 |
|---|------|--------|------|----------|
| 1 | QPS 直接拒绝 | `demo:qps` | `GET /sentinel-demo/qps` | QPS=5 |
| 2 | 线程数拒绝 | `demo:thread` | `GET /sentinel-demo/thread` | max=2 |
| 3 | 关联资源 | `demo:write` / `demo:read` | `GET /sentinel-demo/write` `/read` | write 关联 read |
| 4 | 链路限流 | `demo:commonService` | `GET /sentinel-demo/link/{flag}` | 仅限制 `flag=A` 入口 |
| 5 | 热点参数 | `demo:hot` | `GET /sentinel-demo/hot/{id}` | 默认 QPS=5；`id=1` 阈值 100；`id=2` 阈值 1 |
| 6 | 冷启动 | `demo:warmup` | `GET /sentinel-demo/warmup` | WarmUp 10s，阈值 10 |
| 7 | 排队等待 | `demo:queue` | `GET /sentinel-demo/queue` | QPS=5，超时 5s |
| 8 | 编程式 API | `demo:programmatic` | `GET /sentinel-demo/programmatic` | QPS=3 |
| 9 | Context 来源 | `demo:context` | `GET /sentinel-demo/context/{source}` | 仅限制 `source=web` |
| 10 | 服务端入口限流 | `user:getById` | `GET /user/{id}` (user-service) | QPS=10 |
| 11 | Feign 客户端降级 | `GET /user/{id}` 经 Feign | - | 多种 Sentinel 异常分类处理 |

所有规则集中在：

- `order-service/.../config/SentinelRuleInitializer.java`（order-service 侧）
- `user-service/.../config/SentinelUserRuleInitializer.java`（user-service 侧）

## 2. 启动顺序

```bash
# 1. 启动 Nacos（可选，仅做服务发现时需要）
#    也可以在 application.yaml 中把 nacos discovery 改为不依赖

# 2. 启动 Sentinel Dashboard（可选，仅作可视化）
java -Dserver.port=8080 -Dcsp.sentinel.dashboard.server=localhost:8080 \
     -jar sentinel-dashboard-1.8.6.jar
# 登录 http://localhost:8080 (sentinel/sentinel)

# 3. 启动 user-service
mvn -pl user-service -am spring-boot:run

# 4. 启动 order-service
mvn -pl order-service -am spring-boot:run

# 5. 启动 gateway（可选）
mvn -pl gateway -am spring-boot:run
```

> 端口约定：gateway=8080 / user-service=8081 / order-service=8082

## 3. 压测脚本

下面的命令假设你直接调用 `order-service`（端口 8082），如走 `gateway`，将 URL 改为
`http://localhost:8080/api/order/sentinel-demo/...` 即可（注意 `StripPrefix=1` 会去掉 `/api`）。

### 3.1 QPS 限流（直接拒绝）

```bash
# Linux / macOS
for i in $(seq 1 20); do
  curl -s http://localhost:8082/sentinel-demo/qps
  echo
done

# Windows PowerShell
1..20 | ForEach-Object { (Invoke-WebRequest http://localhost:8082/sentinel-demo/qps).Content }
```

预期：每秒钟最多 5 条 `QPS 限流演示 - 业务正常返回`，其余全部 `429 QPS 触发限流...`

### 3.2 线程数限流

```bash
# 使用 ab 并发 10 个线程压测
ab -n 50 -c 10 http://localhost:8082/sentinel-demo/thread
```

预期：因方法内 `Thread.sleep(500)`，并发 10 时实际并发被限制在 2，多余请求被拒绝。

### 3.3 关联资源

```bash
# 终端1：持续压测 write
while true; do curl -s http://localhost:8082/sentinel-demo/write > /dev/null; done

# 终端2：观察 read
for i in $(seq 1 20); do
  curl -s http://localhost:8082/sentinel-demo/read
  echo
done
```

预期：当 write 的 QPS > 1 时，read 也会被拒绝（关联规则）。

### 3.4 链路限流

```bash
# A 入口的请求被限流
for i in $(seq 1 20); do
  curl -s http://localhost:8082/sentinel-demo/link/A
  echo
done

# B 入口的请求不受限
for i in $(seq 1 20); do
  curl -s http://localhost:8082/sentinel-demo/link/B
  echo
done
```

预期：入口 A 的请求被限流，B 全部通过。
> 链路模式需要配置 `spring.cloud.sentinel.web-context-unify=false`（已配置）。

### 3.5 热点参数

```bash
# id=1（白名单，阈值 100）几乎不会触发
hey -n 200 -c 50 http://localhost:8082/sentinel-demo/hot/1

# id=2（黑名单，阈值 1）大部分被限流
hey -n 200 -c 50 http://localhost:8082/sentinel-demo/hot/2

# 其他 id 默认 QPS=5
hey -n 200 -c 50 http://localhost:8082/sentinel-demo/hot/999
```

### 3.6 冷启动（Warm Up）

```bash
# 服务刚启动时立即打满流量，会看到前几秒被拒绝
hey -n 1000 -c 50 -z 15s http://localhost:8082/sentinel-demo/warmup
```

预期：启动后前 ~10s 内大量请求被拒绝（系统处于预热），10s 后逐渐放行到 QPS=10。

### 3.7 排队等待

```bash
# 高并发下请求被匀速放行
hey -n 200 -c 30 http://localhost:8082/sentinel-demo/queue
```

预期：请求以约 5 QPS 的稳定速率通过，超出排队时长的请求被拒绝（不会瞬间全拒）。

### 3.8 编程式 API

```bash
for i in $(seq 1 20); do
  curl -s http://localhost:8082/sentinel-demo/programmatic
  echo
done
```

### 3.9 Context 来源

```bash
# 仅 source=web 触发限流
for i in $(seq 1 20); do
  curl -s http://localhost:8082/sentinel-demo/context/web
  echo
done
```

### 3.10 Feign 调用方降级（order-service -> user-service）

启动后，order-service 通过 Feign 调用 `user-service` 的 `/user/{id}`，可同时压测两端的限流。

```bash
# 压测 order 的下单接口，间接压测 user-service
hey -n 500 -c 50 -m POST -H "Content-Type: application/json" \
    -d '{"userId":1,"productName":"book","amount":99.9}' \
    http://localhost:8082/order
```

预期：
- 当 user-service 触发限流（`FlowException`），order 侧 Feign fallback 返回 `[限流] 用户服务 QPS 超限...`
- 当 user-service 触发熔断（`DegradeException`），fallback 返回 `[熔断] 用户服务暂时不可用...`
- 当 user-service 不可用（宕机/超时），fallback 返回 `用户服务暂时不可用...`

> order-service 已在 `application.yaml` 中启用 `feign.sentinel.enabled=true`，
> 通过 `UserFeignClient` 的 `fallbackFactory` 处理异常。

## 4. Dashboard 集成（可选）

启动 Sentinel Dashboard 后，服务启动时会自动注册到 `127.0.0.1:8729`（user-service）
和 `127.0.0.1:8722`（order-service）。打开 http://localhost:8080 即可看到所有资源与
实时监控曲线，并可动态调整规则。

> 注意：Dashboard 中调整的规则仅保存在内存中，重启后会被 `SentinelRuleInitializer` 重新加载为初始规则。
> 如果需要持久化，把规则写入 Nacos / Apollo / 本地文件等。

## 5. 监控

Sentinel 默认在 `${user.home}/logs/csp/` 输出监控日志，可以通过 Dashboard 的
"实时监控" 看到 QPS、拒绝数、响应时间等指标。
