package com.java.learn;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class JdkVirtualThreadSchedulerTest {



    private Runnable runnable = new Runnable(){

        /**
         * Runs this operation.
         */
        @Override
        public void run() {
            try {
                Thread.sleep(Duration.ofSeconds(1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("执行结束");
        }
    };


    @Test
    public void test_virtual_Thread_Scheduler_demo_01(){
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 10_000).forEach(i -> {
                executor.submit(() -> {
                    // 任务即休眠1s
                    Thread.sleep(Duration.ofSeconds(1));
                    return i;
                });
            });
        }
    }


    @Test
    public void test_virtual_Thread_Scheduler_demo_02(){

        // 创建一个名为"duke"的新的未启动的虚拟线程
        Thread thread = Thread.ofVirtual().name("duke").unstarted(runnable);

    }


    @Test
    public void test_virtual_Thread_Scheduler_demo_03(){

        // 创建一个名为"duke"的新的未启动的虚拟线程
        Thread.startVirtualThread(runnable);

    }
}
