package com.dochiri.nplusguard.starter;

import com.dochiri.nplusguard.core.scope.ThreadLocalGuardScopeManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class NPlusGuardAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ThreadLocalGuardScopeManager threadLocalGuardScopeManager() {
        return new ThreadLocalGuardScopeManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public NPlusGuardStatementInspector nPlusGuardStatementInspector(ThreadLocalGuardScopeManager scopeManager) {
        return new NPlusGuardStatementInspector(scopeManager);
    }

    @Bean
    public HibernatePropertiesCustomizer nPlusGuardHibernatePropertiesCustomizer(
            NPlusGuardStatementInspector statementInspector
    ) {
        // Hibernate SessionFactory에 inspector를 주입해 SQL 실행 직전 hook을 연결한다.
        return properties -> properties.put("hibernate.session_factory.statement_inspector", statementInspector);
    }
}
