//package com.imooc.order.config;
//
//import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import javax.sql.DataSource;
//
//@Configuration
//@ConditionalOnClass(com.alibaba.druid.pool.DruidDataSource.class)
//public class DruidConfig {
//
//    @Bean(initMethod = "init")
//    @ConfigurationProperties("spring.datasource")
//    public DataSource dataSource(){
//        return DruidDataSourceBuilder.create().build();
//    }
//}
