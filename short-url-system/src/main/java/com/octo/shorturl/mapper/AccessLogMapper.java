package com.octo.shorturl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.shorturl.entity.AccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 访问日志Mapper
 */
@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {

    /**
     * 统计短码的访问量（PV）
     */
    @Select("SELECT COUNT(1) FROM t_access_log WHERE short_code = #{shortCode}")
    long countPv(@Param("shortCode") String shortCode);

    /**
     * 统计短码的独立访客数（UV，按IP去重）
     */
    @Select("SELECT COUNT(DISTINCT ip) FROM t_access_log WHERE short_code = #{shortCode}")
    long countUv(@Param("shortCode") String shortCode);

    /**
     * 按日期统计PV
     */
    @Select("SELECT DATE(access_time) as date, COUNT(1) as count " +
            "FROM t_access_log WHERE short_code = #{shortCode} " +
            "AND access_time >= #{startDate} AND access_time < #{endDate} " +
            "GROUP BY DATE(access_time) ORDER BY date")
    List<Map<String, Object>> countPvByDate(@Param("shortCode") String shortCode,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 按设备类型统计
     */
    @Select("SELECT device_type, COUNT(1) as count FROM t_access_log " +
            "WHERE short_code = #{shortCode} GROUP BY device_type")
    List<Map<String, Object>> countByDeviceType(@Param("shortCode") String shortCode);

    /**
     * 按浏览器统计
     */
    @Select("SELECT browser, COUNT(1) as count FROM t_access_log " +
            "WHERE short_code = #{shortCode} GROUP BY browser ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> countByBrowser(@Param("shortCode") String shortCode);
}

