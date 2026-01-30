package com.example.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.dto.response.ApiResponse;
import com.example.payment.entity.Product;
import com.example.payment.entity.Stock;
import com.example.payment.mapper.ProductMapper;
import com.example.payment.mapper.StockMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StockMapper stockMapper;

    /**
     * 获取商品列表
     */
    @GetMapping("/list")
    public ApiResponse<List<ProductVO>> listProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .eq(Product::getDeleted, 0);
        
        List<Product> products = productMapper.selectList(wrapper);
        
        List<ProductVO> result = products.stream().map(product -> {
            // 获取库存
            LambdaQueryWrapper<Stock> stockWrapper = new LambdaQueryWrapper<>();
            stockWrapper.eq(Stock::getProductId, product.getId());
            Stock stock = stockMapper.selectOne(stockWrapper);
            
            return ProductVO.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .imageUrl(product.getImageUrl())
                    .availableStock(stock != null ? stock.getAvailableStock() : 0)
                    .build();
        }).collect(Collectors.toList());
        
        return ApiResponse.success(result);
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<ProductVO> getProductDetail(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getDeleted() == 1) {
            return ApiResponse.error(404, "商品不存在");
        }

        LambdaQueryWrapper<Stock> stockWrapper = new LambdaQueryWrapper<>();
        stockWrapper.eq(Stock::getProductId, id);
        Stock stock = stockMapper.selectOne(stockWrapper);

        ProductVO vo = ProductVO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .availableStock(stock != null ? stock.getAvailableStock() : 0)
                .build();

        return ApiResponse.success(vo);
    }

    /**
     * 商品VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVO {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private Integer availableStock;
    }
}

