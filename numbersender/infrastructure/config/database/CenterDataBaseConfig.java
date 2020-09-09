package com.xiaoying.base.numbersender.infrastructure.config.database;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.pool.DruidDataSource;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.google.common.collect.Lists;
import com.xiaoying.base.numbersender.infrastructure.dao.SegmentAllocDAO;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.sql.SQLException;
import java.util.Properties;

/**
 * @author:lijin,E-mail:jin.li@quvideo.com>
 * @created:2019/3/12
 * @function: 中心数据库配置
 */
@Configuration
@EnableApolloConfig(value = "viva_common", order = 2)
public class CenterDataBaseConfig {
    @Value("${com.quwei.xiaoying.mysql.jdbc.driver}")
    private String jdbcDriver;

    @Value("${com.quwei.xiaoying.mysql.jdbc.url}")
    private String jdbcUrl;

    @Value("${com.quwei.xiaoying.mysql.jdbc.username}")
    private String userName;

    @Value("${com.quwei.xiaoying.mysql.jdbc.v2.username}")
    private String userNameV2;


    @Value("${com.quwei.xiaoying.mysql.jdbc.password}")
    private String password;


    @Value("${com.quwei.xiaoying.mysql.jdbc.v2.password}")
    private String passwordV2;

    @Value("${com.quwei.xiaoying.mysql.jdbc.v2.publickey}")
    private String publicKey;

    @Value("${com.quwei.xiaoying.mysql.slowSqlMillis}")
    private Long slowSqlMillis;

    @Value("${com.quwei.xiaoying.mysql.logSlowSql}")
    private boolean logSlowSql;

    @Value("${com.quwei.xiaoying.mysql.mergeSql}")
    private boolean mergeSql;

    @Bean
    public StatFilter statFilter() {
        StatFilter statFilter = new StatFilter();
        statFilter.setSlowSqlMillis(slowSqlMillis);
        statFilter.setLogSlowSql(logSlowSql);
        statFilter.setMergeSql(mergeSql);
        return statFilter;
    }

    @Bean(destroyMethod = "close")
    public DruidDataSource dataSource() throws SQLException {
        DruidDataSource druidDataSource = new DruidDataSource();
        //连接信息
        druidDataSource.setDriverClassName(jdbcDriver);
        druidDataSource.setUrl(jdbcUrl);
        druidDataSource.setUsername(userNameV2);
        druidDataSource.setPassword(passwordV2);

        //连接池
        druidDataSource.setMaxActive(20);


        druidDataSource.setDefaultAutoCommit(true);


        Properties properties = new Properties();
        properties.setProperty("config.decrypt", "true");
        properties.setProperty("config.decrypt.key", publicKey);

        druidDataSource.setValidationQuery("SELECT 'x'");
        druidDataSource.setTestWhileIdle(true);
        druidDataSource.setTestOnBorrow(false);
        druidDataSource.setTestOnReturn(false);

        // 打开PSCache，并且指定每个连接上PSCache的大小
        druidDataSource.setPoolPreparedStatements(true);
        druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(20);

        //监控
        druidDataSource.setFilters("slf4j,config");
        druidDataSource.setConnectProperties(properties);
        druidDataSource.setProxyFilters(Lists.newArrayList(statFilter()));
        return druidDataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource());
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(pathMatchingResourcePatternResolver.getResources("classpath*:center/mapper/*Mapper.xml"));
        //DO
        sqlSessionFactoryBean.setTypeAliasesPackage("com.xiaoying.base.numbersender.infrastructure.dataobject");
        return sqlSessionFactoryBean.getObject();
    }

    @Bean
    public MapperFactoryBean segmentAllocDAO() throws Exception {
        MapperFactoryBean mapperFactoryBean = new MapperFactoryBean(SegmentAllocDAO.class);
        mapperFactoryBean.setSqlSessionFactory(sqlSessionFactory());
        return mapperFactoryBean;
    }
}
