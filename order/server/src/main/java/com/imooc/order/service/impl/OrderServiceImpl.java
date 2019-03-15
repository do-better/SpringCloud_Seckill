package com.imooc.order.service.impl;

import com.imooc.order.constant.OrderConst;
import com.imooc.order.dataobject.OrderDetail;
import com.imooc.order.dataobject.OrderMaster;
import com.imooc.order.dataobject.SeckillOrderDetail;
import com.imooc.order.dto.OrderDTO;
import com.imooc.order.enums.ExceptionEnum;
import com.imooc.order.enums.OrderStatusEnum;
import com.imooc.order.enums.PayStatusEnum;
import com.imooc.order.enums.ResultEnum;
import com.imooc.order.exception.OrderException;
import com.imooc.order.message.SeckillOrderReceiver;
import com.imooc.order.repository.OrderDetailRepository;
import com.imooc.order.repository.OrderMasterDao;
import com.imooc.order.repository.OrderMasterRepository;
import com.imooc.order.service.OrderService;
import com.imooc.order.service.RedisService;
import com.imooc.order.utils.JsonUtil;
import com.imooc.order.utils.KeyUtil;
import com.imooc.product.client.ProductClient;
import com.imooc.product.common.DecreaseStockInput;
import com.imooc.product.common.ProductInfoOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import sun.rmi.runtime.Log;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by 廖师兄
 * 2017-12-10 16:44
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private OrderMasterRepository orderMasterRepository;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private RedisService redisService;

    @Autowired
    AmqpTemplate amqpTemplate;
    @Autowired
    SeckillOrderReceiver seckillOrderReceiver;
    @Autowired
    OrderMasterDao orderMasterDao;

    //内存标记，判断该商品是否被处理过
    private HashMap<String, Boolean> localOverMap = new HashMap<String, Boolean>();

    private static class RedisHandler{
        static {
            log.error("inner init");

        }
    }
    @Override
    public int seckill(OrderDTO orderDTO) {

        /**
         * 2. 查询商品信息(调用商品服务)
         * 3. 计算总价
         * 4. 扣库存(调用商品服务)
         * 5. 订单入库
         */

        /**
         * 高并发思路
         */
        //库存保存在redis中
        //减库存并将新值重新设置进redis
        //分布式锁
        //多表，snowflake算法生成分布式订单号
        //订单入库异常需要手动回滚redis
        //订单服务创建订单写入数据库，并发送消息
        //上一步也可以发送消息，让商品服务和订单服务来订阅该消息，并写入数据库，但是如果商品服务或者订单服务写入失败，都需要做相应的数据一致性处理

        OrderDetail orderDetail = orderDTO.getOrderDetailList().get(0);
        //内存标记库存是否不足，减少redis访问
        Boolean over = localOverMap.get(orderDetail.getProductId());
        if (over != null && over) {
            return 1;
        }
        //检查redis中的秒杀结果，如果成功，返回重复秒杀
        String result = (String)redisService.get(OrderConst.SECKILL_STATUS_PREFIX + orderDetail.getProductId() + ":" + orderDTO.getBuyerOpenid());
        if (result != null) {
            //重复秒杀
            return 2;
        }
//        if (OrderConst.SECKILL_RESULT_WAITING.equals(result)) {
//            // 正在排队中
//            return 3;
//        }
//        if (result != null && !OrderConst.SECKILL_RESULT_FAIL.equals(result)) {
//            // result为订单号，重复秒杀
//            return 2;
//        }

        //预减库存,如果不存在，返回-1,库存不足时，也会返回负数 todo 分布式锁
        Long stock = redisService.decr(OrderConst.PRODUCT_STOCK_TEMPLATE + orderDetail.getProductId(), -1);

//        if (stock < 0 && !localOverMap.containsKey(orderDetail.getProductId())) {
//        if (stock < 0) {
//            ProductInfoOutput productInfoOutput = productClient.getProductById(orderDetail.getProductId());
//            //库存不足
//            if (productInfoOutput == null || productInfoOutput.getProductStock() <= 0 ) {
//                localOverMap.put(orderDetail.getProductId(), true);
//                return 1;
//            }
//            log.error("库存：{}",productInfoOutput.getProductStock());
//            //初始化redis和本地map
//            redisService.set(OrderConst.PRODUCT_STOCK_TEMPLATE + orderDetail.getProductId(), productInfoOutput.getProductStock());
//            localOverMap.put(orderDetail.getProductId(), false);
//            Long stock2 = redisService.decr(OrderConst.PRODUCT_STOCK_TEMPLATE + orderDetail.getProductId(), -1);
//            //库存不足
//            if(stock2 < 0) {
//                localOverMap.put(orderDetail.getProductId(), true);
//                return 1;
//            }
//        }
        // 售完
        if (stock <= 0) {
            localOverMap.put(orderDetail.getProductId(), true);
            return 1;
        }

        //如果多表，snowflake算法生成分布式订单号
        String orderId = KeyUtil.genUniqueKey();
        orderDTO.setOrderId(orderId);
        //入队
        amqpTemplate.convertAndSend("seckill_order", JsonUtil.toJson(orderDTO));
        //排队中
//        setSeckillResult(orderDTO.getBuyerOpenid(), orderDetail.getProductId(), OrderConst.SECKILL_RESULT_WAITING);
        return 0;
    }

    @Override
    //记录秒杀的结果
    public void setSeckillResult(String openId, String productId, String status) {
        redisService.setex(OrderConst.SECKILL_STATUS_PREFIX + productId + ":" + openId, status, 3600);
    }

    @Override
    public String getSeckillResult(String openId, String productId) {
        String result = (String)redisService.get(OrderConst.SECKILL_STATUS_PREFIX + productId + ":" + openId);
        if (null != result) {
            return result;
        }
        SeckillOrderDetail orderDetail1 = orderMasterDao.findByProductIdAndOpenId(productId, openId);
        if (orderDetail1 != null) {
            return orderDetail1.getOrderId();
        }
        //未下单
        return null;
    }

    @Override
    @Transactional
    public OrderDTO create(OrderDTO orderDTO) {
        String orderId = KeyUtil.genUniqueKey();

       //查询商品信息(调用商品服务)
//        List<String> productIdList = orderDTO.getOrderDetailList().stream()
//                .map(OrderDetail::getProductId)
//                .collect(Collectors.toList());
//        List<ProductInfoOutput> productInfoList = productClient.listForOrder(productIdList);

        //扣库存(调用商品服务),如果扣库存失败，会抛异常，就不用执行下面的动作
        List<DecreaseStockInput> decreaseStockInputList = orderDTO.getOrderDetailList().stream()
                .map(e -> new DecreaseStockInput(e.getProductId(), e.getProductQuantity()))
                .collect(Collectors.toList());
        List<ProductInfoOutput> productInfoList = productClient.decreaseStock(decreaseStockInputList);

       //计算总价
        BigDecimal orderAmout = new BigDecimal(BigInteger.ZERO);
        for (OrderDetail orderDetail: orderDTO.getOrderDetailList()) {
            for (ProductInfoOutput productInfo: productInfoList) {
                if (productInfo.getProductId().equals(orderDetail.getProductId())) {
                    //单价*数量
                    orderAmout = productInfo.getProductPrice()
                            .multiply(new BigDecimal(orderDetail.getProductQuantity()))
                            .add(orderAmout);
                    BeanUtils.copyProperties(productInfo, orderDetail);
                    orderDetail.setOrderId(orderId);
                    orderDetail.setDetailId(KeyUtil.genUniqueKey());
                    orderDetail.setCreateTime(new Date());
                    orderDetail.setUpdateTime(new Date());
                    //订单详情入库
                    orderDetail = orderDetailRepository.save(orderDetail);
                }
            }
        }

        //订单入库
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setOrderId(orderId);
        BeanUtils.copyProperties(orderDTO, orderMaster);
        orderMaster.setOrderAmount(orderAmout);
        orderMaster.setOrderStatus(OrderStatusEnum.NEW.getCode());
        orderMaster.setPayStatus(PayStatusEnum.WAIT.getCode());
        orderMaster.setCreateTime(new Date());
        orderMaster.setUpdateTime(new Date());
        orderMasterRepository.save(orderMaster);
        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO finish(String orderId) {
        //1. 先查询订单
        Optional<OrderMaster> orderMasterOptional = orderMasterRepository.findById(orderId);
        if (!orderMasterOptional.isPresent()) {
            throw new OrderException(ExceptionEnum.ORDER_NOT_EXIST);
        }

        //2. 判断订单状态
        OrderMaster orderMaster = orderMasterOptional.get();
        if (OrderStatusEnum.NEW.getCode() != orderMaster.getOrderStatus()) {
            throw new OrderException(ExceptionEnum.ORDER_STATUS_ERROR);
        }

        //3. 修改订单状态为完结
        orderMaster.setOrderStatus(OrderStatusEnum.FINISHED.getCode());
        orderMasterRepository.save(orderMaster);

        //查询订单详情
        List<OrderDetail> orderDetailList = orderDetailRepository.findByOrderId(orderId);
        if (CollectionUtils.isEmpty(orderDetailList)) {
            throw new OrderException(ExceptionEnum.ORDER_DETAIL_NOT_EXIST);
        }

        OrderDTO orderDTO = new OrderDTO();
        BeanUtils.copyProperties(orderMaster, orderDTO);
        orderDTO.setOrderDetailList(orderDetailList);

        return orderDTO;
    }


}
