package com.allst.boot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Hutu
 * @since 2024-08-09 下午 10:41
 */
@Data
@AllArgsConstructor
public class Conf {
    private String host;
    private int port;
    private String username;
    private String passwd;
}
