package org.fix.repair.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 * 主要用于配置分页插件
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus拦截器配置
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 添加分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        
        // 设置数据库类型
        paginationInnerInterceptor.setDbType(DbType.MYSQL);
        
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInnerInterceptor.setMaxLimit(1000L);
        
        // 当超过最大单页限制数量时，是否开启count查询，默认true
        paginationInnerInterceptor.setOptimizeJoin(true);
        
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        
        return interceptor;
    }
}
