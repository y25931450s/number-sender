package com.xiaoying.base.numbersender;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

/**
 * Created by lijin on 2018/9/25.
 * 系统启动类
 */

@SpringBootApplication
@EnableDubbo(scanBasePackages = "com.xiaoying.base.numbersender")
@ComponentScan(basePackages = "com.xiaoying.base.numbersender")
@EnableApolloConfig
public class ApplicationBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationBootstrap.class, args);
    }
}
