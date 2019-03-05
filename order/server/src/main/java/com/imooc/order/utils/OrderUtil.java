package com.imooc.order.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 生成JMeter压测订单
 */
public class OrderUtil {

    public static void main(String[] args) {
        createOrder(40000);
    }
    private static void createOrder(int count) {
        File file = new File("D:/GitHub/orders.txt");
        if(file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            //每次程序开始执行默认会从文件开头写（和其他非Random类一样，其他类不能指定任意位置），即相当于raf.seek(0)，如果有内容，会被覆盖
//            raf.seek(raf.length());
            String name = "名字";
            String phone;
            String address = "中国上海市徐汇区漕溪北路555号中国上海市徐汇区漕溪北路555号";
            String openid;
            String order;
            for (int i = 0; i < count; i++) {
                phone = String.valueOf(30000000 + i);
                openid = phone;
                order = name + "," + phone + "," + address + "," + openid + "\r\n";
                raf.write(order.getBytes("UTF-8"));
                System.out.println("roder:" + order);
            }
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("complete");
    }

}
