package com.spectre.config;

import com.spectre.common.RateLimitFilter;
import com.spectre.common.RequestIdFilter;
import com.spectre.common.SecurityHeadersFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@RequiredArgsConstructor
public class FilterOrderingConfig {

    private final RequestIdFilter requestIdFilter;
    private final RateLimitFilter rateLimitFilter;
    private final SecurityHeadersFilter securityHeadersFilter;

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilterReg() {
        FilterRegistrationBean<RequestIdFilter> bean = new FilterRegistrationBean<>(requestIdFilter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE); 
        return bean;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterReg() {
        FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>(rateLimitFilter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilterReg() {
        FilterRegistrationBean<SecurityHeadersFilter> bean = new FilterRegistrationBean<>(securityHeadersFilter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return bean;
    }
}
