package com.allst.exercise.config;

import com.allst.exercise.model.Customers;
import org.springframework.format.Formatter;

import java.text.ParseException;
import java.util.Locale;

/**
 * 数据格式化类
 *
 * @author Hutu
 * @since 2024-07-27 下午 11:39
 */
public class CustomersFormatter implements Formatter<Customers> {
    @Override
    public Customers parse(String text, Locale locale) throws ParseException {
        Customers customers = new Customers();
        if (text.trim().isEmpty()) {
            return customers;
        }
        String[] values = text.split(",");
        customers.setId(Long.parseLong(values[0]));
        customers.setName(values[1]);
        customers.setEmail(values[2]);
        customers.setAddress(values[3]);
        customers.setCity(values[4]);
        customers.setCountry(values[5]);
        return customers;
    }

    @Override
    public String print(Customers object, Locale locale) {
        return String.format("【id = %d, name = %s, email = %s, address = %s, city = %s, country = %s】",
                object.getId(), object.getName(), object.getEmail(), object.getAddress(), object.getCity(), object.getCountry());
    }
}
