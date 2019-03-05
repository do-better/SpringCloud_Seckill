package com.imooc.order.message;

import com.imooc.order.constant.OrderConst;
import com.imooc.order.dataobject.OrderMaster;
import com.imooc.order.dataobject.SeckillOrderDetail;
import com.imooc.order.dto.SeckillOrderDTO;
import com.imooc.order.enums.ExceptionEnum;
import com.imooc.order.enums.OrderStatusEnum;
import com.imooc.order.enums.PayStatusEnum;
import com.imooc.order.exception.OrderException;
import com.imooc.order.repository.OrderMasterDao;
import com.imooc.order.repository.OrderMasterRepository;
import com.imooc.order.repository.SeckillOrderDetailRepository;
import com.imooc.order.service.OrderService;
import com.imooc.order.service.RedisService;
import com.imooc.order.utils.JsonUtil;
import com.imooc.order.utils.KeyUtil;
import com.imooc.product.client.ProductClient;
import com.imooc.product.common.DecreaseStockInput;
import com.imooc.product.common.ProductInfoOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SeckillOrderReceiver {

    @Autowired
    private ProductClient productClient;

    @Autowired
    private OrderMasterRepository orderMasterRepository;
    @Autowired
    private OrderMasterDao orderMasterDao;

    @Autowired
    private RedisService redisService;

    @Autowired
    private SeckillOrderDetailRepository seckillOrderDetailRepository;

    @Autowired
    OrderService ordrerService;

    @RabbitListener(queuesToDeclare = @Queue("seckill_order"))
    @Transactional(rollbackFor = Exception.class)
    public void seckillProcess(String message) {
        SeckillOrderDTO orderDTO = (SeckillOrderDTO) JsonUtil.fromJson(message,
                SeckillOrderDTO.class);
        log.info("从队列【{}】接收到消息：{}", "seckill_order", orderDTO);

        //查询商品信息(调用商品服务)
        if (null == orderDTO) {
            return;
        }
        SeckillOrderDetail orderDetail = orderDTO.getOrderDetailList().get(0);
        SeckillOrderDetail seckillOrderDetail = orderMasterDao.findByProductIdAndOpenId(orderDetail.getProductId(), orderDTO.getBuyerOpenid());
        //重复秒杀
        if (seckillOrderDetail != null) {
            log.error("-------重复秒杀---------openid: {},productid: {}", orderDTO.getBuyerOpenid(), orderDetail.getProductId());
            ordrerService.setSeckillResult(orderDTO.getBuyerOpenid(), orderDetail.getProductId(), OrderConst.SECKILL_RESULT_FAIL);
            return;
        }
        // todo 乐观锁，获取版本号
        ProductInfoOutput productInfo = productClient.getProductById(orderDetail.getProductId());
        //库存不足
        if (productInfo.getProductStock() <= 0) {
            log.error("----------商品库存不足-------productid: {}", productInfo);
        }

        //计算总价
        BigDecimal orderAmout = new BigDecimal(BigInteger.ZERO);
        if (!productInfo.getProductId().equals(orderDetail.getProductId())) {
            log.error("-------商品id有误---------productInfo.getProductId: {},orderDetail.getProductId: {}", productInfo.getProductId(), orderDetail.getProductId());
            ordrerService.setSeckillResult(orderDTO.getBuyerOpenid(), orderDetail.getProductId(), OrderConst.SECKILL_RESULT_FAIL);
            return;
        }

        try {
            //单价*数量
            orderAmout = productInfo.getProductPrice()
                    .multiply(new BigDecimal(orderDetail.getProductQuantity()))
                    .add(orderAmout);
            BeanUtils.copyProperties(productInfo, orderDetail);
            orderDetail.setOrderId(orderDTO.getOrderId());
            orderDetail.setBuyerOpenid(orderDTO.getBuyerOpenid());
            orderDetail.setDetailId(KeyUtil.genUniqueKey());
            orderDetail.setCreateTime(new Date());
            orderDetail.setUpdateTime(new Date());
            //订单详情入库
            seckillOrderDetailRepository.save(orderDetail);

            //扣库存(调用商品服务)
            List<DecreaseStockInput> decreaseStockInputList = orderDTO.getOrderDetailList().stream()
                    .map(e -> new DecreaseStockInput(e.getProductId(), e.getProductQuantity()))
                    .collect(Collectors.toList());
            productClient.decreaseStock(decreaseStockInputList);

            //订单入库
            OrderMaster orderMaster = new OrderMaster();
            BeanUtils.copyProperties(orderDTO, orderMaster);
            orderMaster.setOrderAmount(orderAmout);
            orderMaster.setOrderStatus(OrderStatusEnum.NEW.getCode());
            orderMaster.setPayStatus(PayStatusEnum.WAIT.getCode());
            orderMaster.setCreateTime(new Date());
            orderMaster.setUpdateTime(new Date());
            orderMasterRepository.save(orderMaster);
        } catch (Exception e) {
            //记录秒杀结果为失败
            ordrerService.setSeckillResult(orderDTO.getBuyerOpenid(), orderDetail.getProductId(), OrderConst.SECKILL_RESULT_FAIL);
            //抛出异常，让事务回滚
            throw new OrderException(ExceptionEnum.SECKILL_ORDER_ERROR);
        }
        //如果成功，将orderId存入redis中
        ordrerService.setSeckillResult(orderDTO.getBuyerOpenid(), orderDetail.getProductId(), orderDTO.getOrderId());
    }

}
