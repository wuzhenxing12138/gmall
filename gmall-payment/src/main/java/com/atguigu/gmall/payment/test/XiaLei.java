package com.atguigu.gmall.payment.test;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class XiaLei {

    public static void main(String[] args) {
        ConnectionFactory connect = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER,ActiveMQConnection.DEFAULT_PASSWORD,"tcp://localhost:61616");
        try {
            Connection connection = connect.createConnection();
            connection.setClientID("1");
            connection.start();

            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // 队列式消息
            //Destination queue = session.createQueue("ganggeceshi");

            // 订阅式消息
            Topic topic = session.createTopic("ganggeceshi2");


            TopicSubscriber durableSubscriber = session.createDurableSubscriber(topic, "1");

            MessageConsumer consumer = session.createConsumer(topic);
            durableSubscriber.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    if(message instanceof TextMessage){
                        try {
                            String text = ((TextMessage) message).getText();
                            System.out.println("夏雷老师发现："+text+"。周末来加班");

                            //session.rollback();
                        } catch (JMSException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            });
            System.out.println("夏雷老师来到办公室。。。监听启动。。。ganggeceshi");

        }catch (Exception e){
            e.printStackTrace();;
        }
    }

}
