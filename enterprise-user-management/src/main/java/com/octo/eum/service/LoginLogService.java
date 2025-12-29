package com.octo.eum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.octo.eum.common.PageResult;
import com.octo.eum.dto.request.PageRequest;
import com.octo.eum.entity.LoginLog;

/**
 * 登录日志服务接口
 *
 * @author octo
 */
public interface LoginLogService extends IService<LoginLog> {

    /**
     * 记录登录日志
     *
     * @param loginLog 登录日志
     */
    void saveLoginLog(LoginLog loginLog);

    /**
     * 异步记录登录日志（通过MQ）
     *
     * @param loginLog 登录日志
     */
    void asyncSaveLoginLog(LoginLog loginLog);

    /**
     * 分页查询登录日志
     *
     * @param request  分页请求
     * @param username 用户名
     * @param status   状态
     * @return 分页结果
     */
    PageResult<LoginLog> pageLoginLogs(PageRequest request, String username, Integer status);
}

