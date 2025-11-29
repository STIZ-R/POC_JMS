package com.poc.meta;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import java.util.List;

@SpringBootApplication
@EnableJms
public class GarageC {

    private final String garageName = "GarageC";
    private final int location = 900;
    private final List<String> stock = List.of("pneu", "huile");

    public static void main(String[] args) {
        SpringApplication.run(GarageC.class, args);
    }

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        String brokerUrl = System.getenv("ACTIVEMQ_URL");
        System.out.println(garageName + " connecting to broker: " + brokerUrl);
        return new ActiveMQConnectionFactory(brokerUrl);
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory cf) {
        return new JmsTemplate(cf);
    }

    @Bean
    public DefaultMessageListenerContainer listener(ConnectionFactory cf, JmsTemplate jms) {
        DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
        container.setConnectionFactory(cf);
        container.setDestinationName(garageName + ".queue");

        container.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage txt = (TextMessage) message;
                        String[] parts = txt.getText().split(":");
                        String service = parts[0];
                        int clientLoc = Integer.parseInt(parts[1]);
                        if (!stock.contains(service)) return;

                        int distance = Math.abs(location - clientLoc);
                        String reply = garageName + " , distance=" + distance;
                        System.out.println(garageName + " -> répond : " + reply);

                        Destination replyTo = txt.getJMSReplyTo();
                        if (replyTo != null) {
                            jms.convertAndSend(replyTo, reply);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return container;
    }

}
