package com.allst.springboot;

import com.allst.springboot.enums.BookStatusEnum;
import com.allst.springboot.mapper.BookMapper;
import com.allst.springboot.pojo.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Hutu
 * @since 2024-09-06 下午 11:22
 */
@SpringBootTest
public class BookTest {

    @Autowired
    private BookMapper bookMapper;

    @Test
    public void test() {
        Book book = new Book();
        book.setName("Java11");
        book.setAuthor("xiaohu");
        book.setStatus(BookStatusEnum.on_history);
        bookMapper.insert(book);
    }

    @Test
    public void test2() {
        Book book = bookMapper.selectBookById(1L);
        System.out.println(book);
    }
}
