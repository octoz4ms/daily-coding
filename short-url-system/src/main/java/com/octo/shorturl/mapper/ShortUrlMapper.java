package com.octo.shorturl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.shorturl.entity.ShortUrl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 短链接Mapper
 */
@Mapper
public interface ShortUrlMapper extends BaseMapper<ShortUrl> {

    /**
     * 根据短码查询
     */
    @Select("SELECT * FROM t_short_url WHERE short_code = #{shortCode} AND status = 1")
    ShortUrl findByShortCode(@Param("shortCode") String shortCode);

    /**
     * 根据长链接Hash查询（去重）
     */
    @Select("SELECT * FROM t_short_url WHERE long_url_hash = #{hash} AND status = 1 LIMIT 1")
    ShortUrl findByLongUrlHash(@Param("hash") String hash);

    /**
     * 检查短码是否存在
     */
    @Select("SELECT COUNT(1) FROM t_short_url WHERE short_code = #{shortCode}")
    int checkShortCodeExists(@Param("shortCode") String shortCode);
}

