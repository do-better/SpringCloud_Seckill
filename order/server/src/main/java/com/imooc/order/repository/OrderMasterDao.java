package com.imooc.order.repository;

import com.imooc.order.dataobject.SeckillOrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

//使用@Repository注册不进去
@Mapper
public interface OrderMasterDao {
    SeckillOrderDetail findByProductIdAndOpenId(@Param("productId") String productId, @Param("openId") String openId);
}
