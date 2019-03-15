package com.imooc.product.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductInfoDao {
    int findVersionById(@Param("productId") String productId);
    int decreaseStock(@Param("productId") String productId, @Param("quantity") int quantity);
}
