package com.yuheng.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
 */
public class BiInitMain {

    public static void main(String[] args) {
        try {
            // 创建连接工厂
            ConnectionFactory factory = new ConnectionFactory();


            factory.setHost("124.222.227.235"); // 设置远程服务器的IP地址
            factory.setPort(5672); // 设置RabbitMQ的端口号
            factory.setUsername("yuhengzhang"); // 设置RabbitMQ的用户名
            factory.setPassword("zyh020325"); // 设置RabbitMQ的密码

            // 创建连接
            Connection connection = factory.newConnection();
            // 创建通道
            Channel channel = connection.createChannel();
            // 定义交换机的名称为"code_exchange"
            String EXCHANGE_NAME = "code_exchange";
            // 声明交换机，指定交换机类型为 direct
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            // 创建队列，随机分配一个队列名称
            String queueName = "code_queue";
            // 声明队列，设置队列持久化、非独占、非自动删除，并传入额外的参数为 null
            channel.queueDeclare(queueName, true, false, false, null);
            // 将队列绑定到交换机，指定路由键为 "my_routingKey"
            channel.queueBind(queueName, EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY);
        } catch (Exception e) {
        	// 异常处理
        }
    }
}