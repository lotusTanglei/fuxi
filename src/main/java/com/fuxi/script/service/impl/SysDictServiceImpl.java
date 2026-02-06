package com.fuxi.script.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fuxi.script.entity.SysDict;
import com.fuxi.script.mapper.SysDictMapper;
import com.fuxi.script.service.SysDictService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements SysDictService {

    @Override
    public List<SysDict> getDictsByCategory(String category) {
        return this.list(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getCategory, category)
                .orderByAsc(SysDict::getSort));
    }
}
