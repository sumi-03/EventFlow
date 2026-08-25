package com.example.eventflow.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

// 컨테이너를 Spring Bean으로 등록해 ApplicationContext 캐시와 생명주기를 함께 가져간다.
// JUnit5 @Container/@Testcontainers로 관리하면 테스트 클래스마다 컨테이너가
// 재시작되며 매핑 포트가 바뀌어, 캐시된 컨텍스트가 옛 포트로 접속을 시도해 실패한다.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("eventflow_test")
                .withUsername("eventflow_test")
                .withPassword("eventflow_test");
    }
}
