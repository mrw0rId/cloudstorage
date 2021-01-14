package ru.geekbrains.cloudstorage.auth;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class BasicAuthService implements AuthService {
    private final Set<Record> authorizedRecords;
    private final Set<Record> records;

    private static final String url = "jdbc:mysql://localhost:3306/cloud_users_db?" +
            "createDatabaseIfNotExist=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String user = "root";
    private static final String password = "Twodaysago1!";

    private static Connection con;

    public BasicAuthService() {
        records = new HashSet<>();
        authorizedRecords = new HashSet<>();
        records.add(new Record(1, "user1", "user1", "guest"));
        records.add(new Record(2, "user2", "user2", "guest"));
        records.add(new Record(3, "user3", "user3", "guest"));
        records.add(new Record(4, "admin", "admin", "admin"));

        //Не удалять, конфигурация для JDBC
//        try {
//            Class.forName("com.mysql.cj.jdbc.Driver");
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//        try {
//            con = DriverManager.getConnection(url, user, password);
//        } catch (SQLException throwables) {
//            throwables.printStackTrace();
//        }

    }

    @Override
    public void logout(Record record) {
        authorizedRecords.remove(record);
    }

    @Override
    public Record findRecord(String login, String password) {
        for (Record record : records) {
            if (record.getLogin().equals(login) && record.getPassword().equals(password)) {
                if(authorizedRecords.contains(record)){
                    return new Record(0,"","","");
                } else {
                    authorizedRecords.add(record);
                    return record;
                }
            }
        }
        return null;
//        Не удалять, конфигурация для JDBC
//        try (PreparedStatement find = con.prepareStatement("SELECT * from users where login = ?")) {
//            find.setString(1, login);
//            ResultSet rs = find.executeQuery();
//            System.out.println(2);
//            while (rs.next()) {
//                if (rs.getString("login").equals(login) && rs.getString("password").equals(password)) {
//                    System.out.println(3);
//                    for (Record record : authorizedRecords) {
//                        if (record.getLogin().equals(login) && record.getPassword().equals(password)) {
//                            System.out.println(4);
//                            return new Record(0,"","","");
//                        }
//                    }
//                    System.out.println(5);
//                    authorizedRecords.add(new Record(rs.getInt("id"),rs.getString("login"),
//                            rs.getString("password"), rs.getString("role")));
//                    for (Record record : authorizedRecords) {
//                        if (record.getLogin().equals(login) && record.getPassword().equals(password)) {
//                            System.out.println(6);
//                            return record;
//                        }
//                    }
//                } else {
//                    System.out.println(7);
//                    return null;
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        System.out.println(8);
//        return null;
    }
}
