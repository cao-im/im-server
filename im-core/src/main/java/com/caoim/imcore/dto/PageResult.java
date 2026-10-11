package com.caoim.imcore.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private long page;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords());
        return result;
    }
}
