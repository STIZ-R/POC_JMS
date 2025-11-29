package com.poc.meta;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * La classe client est la classe qui effectur directement la demande de rdv
 * pour une révision (exemple)
 * De ce fait, l'utilisateur envoie une demande de rdv avec SA localisation et
 * SON besoin (exemple pneu et 30) localisation en 1D
 * Chaque garage à sa queue et écoute la demande des clients, et répond avec sa
 * distance par rapport aux clients (s'il a le type de ressource)
 * Le client peut ensuite lire les réponse envoyé dans une queue JMS et choisit le
 * garage avec la distance la plus proche de lui.
 *
 *
 */
@SpringBootApplication
@EnableJms
public class ClientApp {

    public static void main(String[] args) {
        SpringApplication.run(ClientApp.class, args);
    }

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        String brokerUrl = System.getenv("ACTIVEMQ_URL");
        System.out.println("Connecting to broker: " + brokerUrl);
        return new ActiveMQConnectionFactory(brokerUrl);
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory cf) {
        return new JmsTemplate(cf);
    }

    @Bean
    public CommandLineRunner sendRequest(JmsTemplate jms) {
        return args -> {

            String request = "pneu:30";
            System.out.println("CLIENT -> Envoi demande : " + request);

            String clientQueue = "client.reply.queue." + UUID.randomUUID();
            CountDownLatch latch = new CountDownLatch(3);


            final Object lock = new Object();
            final int[] bestDistance = { Integer.MAX_VALUE };
            final String[] bestResponse = { null };


            DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
            container.setConnectionFactory(jms.getConnectionFactory());
            container.setDestinationName(clientQueue);
            container.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof TextMessage) {
                            TextMessage txt = (TextMessage) message;

                            String response = txt.getText();
                            int distance = Integer.parseInt(response.split("distance=")[1]);

                            synchronized (lock) {
                                if (distance < bestDistance[0]) {
                                    bestDistance[0] = distance;
                                    bestResponse[0] = response;
                                }
                            }

                            latch.countDown();
                            System.out.println("CLIENT <- Réponse reçue : " + response);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });


            container.afterPropertiesSet();
            container.start();

            // envoie aux garages
            List<String> garages = List.of("GarageA.queue", "GarageB.queue", "GarageC.queue");

            for (String queue : garages) {
                jms.convertAndSend(queue, request, message -> {
                    message.setJMSReplyTo(new ActiveMQQueue(clientQueue));
                    return message;
                });
            }
            latch.await(5, TimeUnit.SECONDS);

            synchronized (lock) {
                System.out.println("CLIENT <- Meilleure réponse : " + bestResponse[0]);
            }

            container.stop();
        };
    }
}

