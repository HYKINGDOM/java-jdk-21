package com.java.learn;

import org.junit.jupiter.api.Test;

public class RecordPatternsTest {



    record Person(String name, int age) {}

    @Test
    public void test_demo_01(){
        Person person = new Person("Alice", 25);

        if (person instanceof Person p) {
            System.out.println(p.name()); // 输出 "Alice"
            System.out.println(p.age()); // 输出 25
        }
    }


}
