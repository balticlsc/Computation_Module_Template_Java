package lv.lumii.balticlsc.module.task;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadConfig {

    @Bean
    public ExecutorService threadPoolTaskExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        return executor;
    }
}
