package com.xiaoying.base.numbersender.config;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.xiaoying.base.numbersender.domain.snowflake.SnowflakeZookeeperHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author:lijin,E-mail:jin.li@quvideo.com>
 * @created:2019/3/13
 * @function: 业务配置
 */

@Configuration
@EnableApolloConfig
public class BizConfig {
    @Value("${number.sender.zookeeper.address}")
    private String zkAddress;

    private final static Integer PORT = 3434;

    @Bean
    public SnowflakeZookeeperHolder snowflakeZookeeperHolder() {
        String ip = "unknow";
        try {
            InetAddress inetAddress = Inet4Address.getLocalHost();
            ip = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {

        }

        SnowflakeZookeeperHolder snowflakeZookeeperHolder = new SnowflakeZookeeperHolder(ip,String.valueOf(PORT),zkAddress);
        return snowflakeZookeeperHolder;
    }
}
