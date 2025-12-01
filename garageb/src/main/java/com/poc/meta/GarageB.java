package com.poc.meta;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

@SpringBootApplication
@EnableJms
public class GarageB {

    private final String garageName = "GarageB";

    public static void main(String[] args) {
        SpringApplication.run(GarageB.class, args);
    }

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        String brokerUrl = System.getenv("ACTIVEMQ_URL");
        return new ActiveMQConnectionFactory(brokerUrl);
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory cf) {
        return new JmsTemplate(cf);
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(System.getenv("GARAGE_DB_URL"));
        ds.setUsername(System.getenv("GARAGE_DB_USER"));
        ds.setPassword(System.getenv("GARAGE_DB_PASSWORD"));
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public DefaultMessageListenerContainer listener(ConnectionFactory cf, JmsTemplate jms, JdbcTemplate jdbc) {
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

                        List<String> stock = jdbc.query("SELECT item FROM stock",
                                (rs, rowNum) -> rs.getString("item"));

                        if (!stock.contains(service)) return;

                        int location = jdbc.queryForObject(
                                "SELECT location FROM garage_info LIMIT 1", Integer.class);

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
