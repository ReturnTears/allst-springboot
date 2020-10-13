package com.allst.springboot;

import com.allst.springboot.bean.Department;
import com.allst.springboot.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.Date;

@SpringBootTest
class AllstSpringbootAdvsApplicationTests {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    RedisTemplate redisTemplate;

    @Resource
    private DepartmentService departmentService;

    @Autowired
    JavaMailSenderImpl javaMailSender;

    @Test
    void contextLoads() {
        stringRedisTemplate.opsForValue().set("hello", "world");
        String result = stringRedisTemplate.opsForValue().get("user");
        System.out.println(result);
    }

    @Test
    void contextCacheLoads() {
        final Integer id = 9;
        Department department = departmentService.getDepartmentWithCacheById(id);
        redisTemplate.opsForValue().set("department::" + id, department.toString());

        Object result = redisTemplate.opsForValue().get("department::" + id);
        assert result != null;
        System.out.println(result.toString());
    }

    @Test
    void contextMailLoads() {
        // 简单邮件
        SimpleMailMessage message = new SimpleMailMessage();
        // 邮件设置
        message.setSubject("请注意: 重要通知");
        message.setText("Hello,请注意了,我要开始装逼了~");
        message.setTo("15023424015@163.com");
        message.setFrom("709844757@qq.com");
        message.setSentDate(new Date());
        javaMailSender.send(message);
    }

    @Test
    void contextMail2Loads() throws MessagingException {
        // 复杂邮件
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        // 邮件设置
        helper.setSubject("请注意: 重要通知");
        helper.setText("<span style='color: red'><b>Hello</b>,请注意了,我要开始装逼了~</span>", true);
        helper.setTo("15023424015@163.com");
        helper.setFrom("709844757@qq.com");
        helper.setSentDate(new Date());

        // 上传附件
        helper.addAttachment("junn (4).jpg", new File("C:\\Users\\June\\Pictures\\Camera Roll\\pic\\junn (4).jpg"));
        helper.addAttachment("曲婉婷 - Everything In The world.mp3", new File("C:\\Users\\June\\Music\\曲婉婷 - Everything In The world.mp3"));

        javaMailSender.send(mimeMessage);
    }
}
