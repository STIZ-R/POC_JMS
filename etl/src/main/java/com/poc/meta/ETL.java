package com.poc.meta;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

@SpringBootApplication
public class ETL {

    public static void main(String[] args) {
        SpringApplication.run(ETL.class, args);
    }

    @Bean
    public DataSource analyticsDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(System.getenv("ANALYTICS_DB_URL")); // ex: jdbc:postgresql://analytics-db:5432/analytics
        ds.setUsername(System.getenv("ANALYTICS_DB_USER"));
        ds.setPassword(System.getenv("ANALYTICS_DB_PASSWORD"));
        return ds;
    }

    @Bean
    public JdbcTemplate analyticsJdbcTemplate(DataSource analyticsDataSource) {
        return new JdbcTemplate(analyticsDataSource);
    }

    @Bean
    public CommandLineRunner runETL(JdbcTemplate analyticsJdbc) {
        return args -> {
            String[] garageUrls = {
                    System.getenv("GARAGEA_DB_URL"),
                    System.getenv("GARAGEB_DB_URL"),
                    System.getenv("GARAGEC_DB_URL")
            };
            String[] garageNames = {"GarageA", "GarageB", "GarageC"};

            for (int i = 0; i < garageUrls.length; i++) {
                DriverManagerDataSource ds = new DriverManagerDataSource();
                ds.setDriverClassName("org.postgresql.Driver");
                ds.setUrl(garageUrls[i]);
                ds.setUsername(System.getenv("GARAGE_DB_USER"));
                ds.setPassword(System.getenv("GARAGE_DB_PASSWORD"));

                JdbcTemplate garageJdbc = new JdbcTemplate(ds);

                List<String> items = garageJdbc.query("SELECT item FROM stock", (rs, rowNum) -> rs.getString("item"));

                int location = garageJdbc.queryForObject("SELECT location FROM garage_info LIMIT 1", Integer.class);

                for (String item : items) {
                    analyticsJdbc.update(
                            "INSERT INTO garage_stock_analytics (garage, item, location) VALUES (?, ?, ?)", garageNames[i], item, location
                    );
                }
                System.out.println("Données du " + garageNames[i] + " insérées dans la base analytique.");
            }

            System.out.println("ETL terminé : toutes les données consolidées dans PostgreSQL !");
            AnalyseStock.main(new String[]{});
        };
    }
}
