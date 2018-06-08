package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class TestP1 {

    public static void main(String[] args) throws  Exception{

        // 创建连接工厂
        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try {
            // 创建连接
            Connection connection = connect.createConnection();
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            // 通过连接穿件一个回话session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);


            // 队列式的消息，不管多少个consumer，只要有一个消费，任务完成，不在重复消费消息
            Queue queue = session.createQueue("ganggeceshi");

            // 订阅式消息，多少个consumer，同时消费
            Topic topic = session.createTopic("ganggeceshi2");

            MessageProducer producer = session.createProducer(topic);

            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText("刚哥说：为教育事业伟大复兴而努力奋斗！");
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);//设置消息的是否持久化
            producer.send(textMessage);//发送消息
            session.commit();//开启事务了，必须提交

            producer.close();//关闭回话
            connection.close();//关闭连接

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

}
