package com.octo.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.seckill.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}

