package com.java.learn;

import org.junit.jupiter.api.Test;

public class PatternMatchingForSwitchTest {
    
    @Test
    public void test_demo_1(){

        Object obj = 1111;


        int result = switch (obj) {
            case String s -> s.length();
            case Integer i -> i * 2;
            default -> -1;
        };

        System.out.println(result);
    }
}
