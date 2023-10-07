package com.java.learn;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Supplier;

public class StructuredTaskScopeTest {

    @Test
    public void test_demo_01() throws InterruptedException {

        StructuredTaskScope.ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure();

        Supplier<String> orderSupplier = scope.fork(new GetOrderTask());

        Supplier<String> userSupplier = scope.fork(new GetUserTask());


        scope.join();

        String order = orderSupplier.get();
        String user = userSupplier.get();

        System.out.println(order + user);

    }


    public class GetOrderTask implements Callable<String> {

        @Override
        public String call() throws Exception {

            Thread.sleep(1);

            throw new NullPointerException();

        }
    }


    public class GetUserTask implements Callable<String> {


        @Override
        public String call() throws Exception {
            return "GetUserTask";
        }
    }
}
