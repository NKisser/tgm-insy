package com.asuender.jdbc.a01;

import java.sql.*;

/**
 * @author Andreas Suender
 * @version 09-21-2022
 */
public class Main {
    public static void main(String[] args) {
        try {
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/test", "admin", "Ej4ke1fhvoru"
            );

            Statement statement = con.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM person");

            while(resultSet.next()) {
                System.out.println(resultSet.getString("vorname"));
            }

        } catch (SQLException sqle) {
            System.err.println(sqle.getMessage());
        }
    }
}
