package vip.aevlp.disruptor.spring.boot;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration                // 配置控制
@EnableScheduling
@SpringBootApplication
public class DisruptorApplication implements ApplicationRunner, CommandLineRunner {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(DisruptorApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

    }

    @Override
    public void run(String... args) throws Exception {

    }

    @Primary
    @Bean
    public TaskExecutor primaryTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        return executor;
    }

}
