package com.allst.boot;

import com.allst.boot.domain.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

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

    @Test
    public void TestPreparedStatementSetter() {
        String sql = "insert into book (name, pageCount) values (?, ?)";
        jdbcTemplate.update(sql, (PreparedStatement ps) -> {
           ps.setString(1, "Vue实战");
           ps.setInt(2, 520);
        });
    }

    @Test
    public void testResultSetExtractor() {
        String sql = "select * from book where id = ?";
        Book result = jdbcTemplate.query(sql, (ResultSet rs) -> {
            rs.next();
            Book book = new Book();
            book.setId(rs.getLong("id"));
            book.setName(rs.getString("name"));
            book.setPageCount(rs.getString("pageCount"));
            return book;
        }, 13);
        System.out.println(result);
    }

    @Test
    public void testRowCallbackHandler() {
        String sql = "select * from book where id = ?";
        Book book = new Book();
        jdbcTemplate.query(sql, (ResultSet rs) -> {
            book.setId(rs.getLong("id"));
            book.setName(rs.getString("name"));
            book.setPageCount(rs.getString("pageCount"));
        }, 15);
        System.out.println(book);
    }

    @Test
    public void testRowMapper() {
        String sql = "select * from book";
        List<Book> bookList = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Book book = new Book();
            book.setId(rs.getLong("id"));
            book.setName(rs.getString("name"));
            book.setPageCount(rs.getString("pageCount"));
            return book;
        });
        bookList.forEach(System.out::println);
    }
}
