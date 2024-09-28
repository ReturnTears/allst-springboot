package com.allst.boot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

@SpringBootTest
public class AppJpaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void TestStatementCallback() {
        jdbcTemplate.execute((Statement statement) -> {
            String sql = "insert into book (name, pageCount) values ('C++实战', 650)";
            return statement.executeUpdate(sql);
        });
    }

    @Test
    public void TestPreparedStatementCallback() {
        jdbcTemplate.execute((Connection conn) -> {
            String sql = "insert into book (name, pageCount) values (?, ?)";
            return conn.prepareStatement(sql);
        }, (PreparedStatement ps) -> {
            ps.setString(1, "JS实战");
            ps.setInt(2, 490);
            return ps.executeUpdate();
        });
    }
}
