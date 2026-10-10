package zm.agriswift.common;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfig implements DisposableBean {

    private ThreadPoolTaskScheduler scheduler;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("agriswift-sched-");

        // Daemon threads ensure they don't block JVM shutdown
        // and are easily killed when the context closes.
        scheduler.setThreadFactory(r -> {
            Thread t = new Thread(r, "agriswift-sched-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });

        // Do not wait for tasks to finish on DevTools restart
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.setAwaitTerminationSeconds(0);

        return scheduler;
    }

    @Override
    public void destroy() {
        if (scheduler != null) {
            // Force kill any lingering ghost threads immediately
            // when DevTools triggers a context close.
            scheduler.getScheduledThreadPoolExecutor().shutdownNow();
        }
    }
}