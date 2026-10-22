package com.allst.springboot.web;

import org.springframework.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 把请求体缓存在内存里，签名拦截器和控制器可以各读一次。
 * 只用于有上限的 JSON 报文，不适合文件上传。
 */
public class RepeatableReadRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public RepeatableReadRequestWrapper(HttpServletRequest request, int maxBytes) throws IOException {
        super(request);
        byte[] content = StreamUtils.copyToByteArray(request.getInputStream());
        if (content.length > maxBytes) {
            throw new PayloadTooLargeException();
        }
        this.body = content;
    }

    public String bodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 同步读取，不注册监听器
            }

            @Override
            public int read() {
                return input.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }
}
