package ru.geekbrains.cloudstorage.auth;

import java.util.Objects;

public interface AuthService {
    Record findRecord(String login, String password);
    void logout(Record record);

    class Record {
        private int id;
        private String login;
        private String password;
        private String role;

        public Record(int id, String login, String password, String role) {
            this.id = id;
            this.login = login;
            this.password = password;
            this.role = role;
        }

        public long getId() {
            return id;
        }

        public String getRole() {
            return role;
        }

        public String getLogin() {
            return login;
        }

        public String getPassword() {
            return password;
        }

        @Override
        public String toString() {
            return "Record{" +
                    "id=" + id +
                    ", login='" + login + '\'' +
                    ", password='" + password + '\'' +
                    ", role='" + role + '\'' +
                    '}';
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return id == record.id &&
                    role.equals(record.role) &&
                    login.equals(record.login) &&
                    password.equals(record.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, login, password, role);
        }
    }
}
