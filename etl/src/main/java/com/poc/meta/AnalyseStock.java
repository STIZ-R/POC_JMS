package com.poc.meta;

import java.sql.*;
import java.util.*;

public class AnalyseStock {

    public static void main(String[] args) throws Exception {

        String user = "user";
        String pass = "pass";
        String analyticsUrl = "jdbc:postgresql://analytics-db:5432/analytics";

        try (Connection conn = DriverManager.getConnection(analyticsUrl, user, pass);
             Statement stmt = conn.createStatement()) {


            /**
             * Version ultra simpliste pour le POC
             * On "triche" sur la marchandise manquante
             * ultérieurement, on pourrait faire un algo de ML qui
             * étudie ce qu'il y a habituellement commme demande dans
             * chaque garage afin de décider ce qu'il manque.
             */
            Map<String, Integer> clientDemands = new HashMap<>();
            clientDemands.put("pneu", 10);
            clientDemands.put("huile", 5);
            clientDemands.put("freins", 2);

            for (Map.Entry<String, Integer> entry : clientDemands.entrySet()) {
                String item = entry.getKey();
                int quantityNeeded = entry.getValue();

                ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) as total FROM garage_stock_analytics WHERE item='" + item + "'"
                );
                rs.next();
                int totalAvailable = rs.getInt("total");

                int missing = Math.max(0, quantityNeeded - totalAvailable);
                if (missing > 0) {
                    System.out.println("Rupture de stock pour " + item + ", manquant: " + missing);
                } else {
                    System.out.println("Stock suffisant pour " + item + ": " + totalAvailable);
                }
            }
        }
    }

}
