package com.allst.springboot;

import com.allst.springboot.enums.BookStatusEnum;
import com.allst.springboot.mapper.BookMapper;
import com.allst.springboot.model.BookVo;
import com.allst.springboot.pojo.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

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
        Optional<Book> optional = Optional.ofNullable(bookMapper.selectBookById(3L));
        if (optional.isPresent()) {
            Book book = optional.get();
            System.out.println("book : " + book);
            BookVo vo = new BookVo();
            BeanUtils.copyProperties(book, vo);
            vo.setStatus(book.getStatus().getMessage());
            System.out.println("vo : " + vo);
        }
    }
}
