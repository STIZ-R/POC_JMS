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

/**
 * Classe générique pour un garage, on en a mit 3 pour notre POC.
 * Un garage possède un type de biens, par exemple, pneus, huiles, moteurs, etc.
 * Et un localisation (seulement en 1D)
 */
@SpringBootApplication
@EnableJms
public class GarageA {
    //nom du garage + localisation (1d plus simple) + liste des objets en stock
    private final String garageName = "GarageA";
    private final int location = -17;
    private final List<String> stock = List.of( "huile", "pneu");

    public static void main(String[] args) {
        SpringApplication.run(GarageA.class, args);
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
