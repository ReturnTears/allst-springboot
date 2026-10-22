package com.allst.springboot.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页数据。size 是实际采用的每页条数，total 是总记录数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageData<T> {

    private long total;
    private int page;
    private int size;
    private List<T> records;
}
