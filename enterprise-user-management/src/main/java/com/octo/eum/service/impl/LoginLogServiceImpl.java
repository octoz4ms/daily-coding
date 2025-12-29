package com.octo.eum.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.octo.eum.common.PageResult;
import com.octo.eum.config.RabbitMQConfig;
import com.octo.eum.dto.request.PageRequest;
import com.octo.eum.entity.LoginLog;
import com.octo.eum.mapper.LoginLogMapper;
import com.octo.eum.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 登录日志服务实现
 *
 * @author octo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void saveLoginLog(LoginLog loginLog) {
        save(loginLog);
        log.debug("登录日志已保存: user={}, type={}, status={}",
                loginLog.getUsername(), loginLog.getType(), loginLog.getStatus());
    }

    @Override
    public void asyncSaveLoginLog(LoginLog loginLog) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.LOGIN_LOG_EXCHANGE,
                    RabbitMQConfig.LOGIN_LOG_ROUTING_KEY,
                    loginLog
            );
            log.debug("登录日志已发送到MQ: user={}", loginLog.getUsername());
        } catch (Exception e) {
            log.error("发送登录日志到MQ失败，直接保存: {}", e.getMessage());
            // 降级处理：直接保存
            saveLoginLog(loginLog);
        }
    }

    @Override
    public PageResult<LoginLog> pageLoginLogs(PageRequest request, String username, Integer status) {
        // 构建查询条件
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(username), LoginLog::getUsername, username)
                .eq(status != null, LoginLog::getStatus, status)
                .orderByDesc(LoginLog::getLoginTime);

        // 分页查询
        IPage<LoginLog> page = new Page<>(request.getPageNum(), request.getPageSize());
        page = page(page, wrapper);

        return PageResult.from(page);
    }
}

