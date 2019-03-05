package com.imooc.order.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.imooc.order.constant.OrderConst;
import com.imooc.order.utils.JsonUtil;
import com.imooc.product.common.ProductInfoOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 廖师兄
 * 2018-02-21 11:01
 */
@Component
@Slf4j
public class ProductInfoReceiver {

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@RabbitListener(queuesToDeclare = @Queue("productInfo"))
	public void process(String message) {
		//message => ProductInfoOutput
		List<ProductInfoOutput> productInfoOutputList = (List<ProductInfoOutput>)JsonUtil.fromJson(message,
				new TypeReference<List<ProductInfoOutput>>() {});
		log.info("从队列【{}】接收到消息：{}", "productInfo", productInfoOutputList);

		//存储到redis中
		for (ProductInfoOutput productInfoOutput : productInfoOutputList) {
			stringRedisTemplate.opsForValue().set(String.format("product_stock:%s", productInfoOutput.getProductId()),
					String.valueOf(productInfoOutput.getProductStock()));
		}
	}
}
