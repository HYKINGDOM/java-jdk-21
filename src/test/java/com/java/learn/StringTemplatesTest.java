package com.java.learn;

import org.junit.jupiter.api.Test;

public class StringTemplatesTest {


    @Test
    public void test_demo_01(){

        String name = "Alice";
        int age = 25;
        String message = "My name is ${name} and I'm ${age} years old.";
        System.out.println(message);
    }
}
