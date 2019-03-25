# SpringCloudSeckill

基于慕课网《SpringCloud微服务实战》开发的秒杀项目

秒杀方法在order模块下

秒杀思路：

通过Nginx负载均衡到SpringCloud的geteway路由模块，并通过RateLimiter令牌桶算法限流；

通过Feign自带的Ribbon负载均衡到order订单模块；

通过Hystrix隔离、熔断、降级实现容错；

通过Redis缓存秒杀商品数量和下单成功订单号，减轻数据库压力；

通过内存标记库存是否不足，减轻Redis压力；

通过RabbitMQ实现异步下单，调用product商品服务；

新增获取秒杀结果接口，前端可通过轮询方式获取结果；

通过SpringCloud集群、Redis集群、RabbitMQ镜像集群实现系统高可用；

数据库层可使用读写分离，分库分表技术分摊流量（未做）

使用JMeter进行压力测试。
