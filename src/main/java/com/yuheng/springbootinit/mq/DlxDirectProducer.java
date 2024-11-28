package com.yuheng.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;

public class DlxDirectProducer {
  // 定义交换机名称为"direct-exchange"
  private static final String EXCHANGE_NAME = "dlx-direct-exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        channel.exchangeDeclare(EXCHANGE_NAME, "direct");


        //  创建两个死信队列  和死信交换机绑定

        String queueName = "laoban_dlx-queue";
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "laoban");


        String queueName2 = "waibao_dlx-queue";
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "waibao");





        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String userInput = scanner.nextLine();
            String[] strings = userInput.split(" ");
            if (strings.length < 1) {
                continue;
            }
            // 获取消息内容和路由键
            String message = strings[0];
            String routingKey = strings[1];
            
        	// 发布消息到直连交换机
            // 使用通道的basicPublish方法将消息发布到交换机
            // EXCHANGE_NAME表示要发布消息的交换机的名称
            // routingKey表示消息的路由键，用于确定消息被路由到哪个队列
            // null表示不使用额外的消息属性
            // message.getBytes("UTF-8")将消息内容转换为UTF-8编码的字节数组
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            // 打印成功发送的消息信息，包括消息内容和路由键
            System.out.println(" [x] Sent '" + message + " with routing:" + routingKey + "'");
        }
    }
  }
  //..
}
