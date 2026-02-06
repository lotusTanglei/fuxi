package com.fuxi.script;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fuxi.script.mapper")
public class FuxiScriptApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuxiScriptApplication.class, args);
    }

}
