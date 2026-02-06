package com.fuxi.script.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fuxi.script.entity.SysDict;

import java.util.List;

public interface SysDictService extends IService<SysDict> {
    List<SysDict> getDictsByCategory(String category);
}
