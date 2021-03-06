package com.imooc.product.client;

import com.imooc.product.common.DecreaseStockInput;
import com.imooc.product.common.ProductInfoOutput;
import com.netflix.ribbon.proxy.annotation.Hystrix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

//import org.springframework.cloud.netflix.feign.FeignClient;


/**
 * 降级
 * feign使用Ribbon负载均衡，默认轮询
 */
@Hystrix
@FeignClient(name = "product", fallback = ProductClient.ProductClientFallback.class)
public interface ProductClient {

    @PostMapping("/product/listForOrder")
    List<ProductInfoOutput> listForOrder(@RequestBody List<String> productIdList);

    @GetMapping("/product/products/{productId}")
    //@PathVariable(value = "productId") 必须写value，否则会报错
    ProductInfoOutput getProductById(@PathVariable(value = "productId") String productId);

    @PostMapping("/product/decreaseStock")
    List<ProductInfoOutput> decreaseStock(@RequestBody List<DecreaseStockInput> decreaseStockInputList);

    @Component
    @Slf4j
    class ProductClientFallback implements ProductClient {

        @Override
        public List<ProductInfoOutput> listForOrder(List<String> productIdList) {
            return null;
        }

        @Override
        public ProductInfoOutput getProductById(String productId) {
            return null;
        }

        @Override
        public List<ProductInfoOutput> decreaseStock(List<DecreaseStockInput> decreaseStockInputList) {
            log.error("减库存失败！");
            return null;
        }
    }
}
