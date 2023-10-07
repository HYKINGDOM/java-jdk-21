package com.java.learn;

import org.junit.jupiter.api.Test;

import jdk.incubator.foreign.*;



public class NativeFunctionTest {

    @Test
    public void test_demo_1(){
//        try (MemorySegment segment = MemorySegment.allocateNative(4)) {
//            CLinker linker = CLinker.getInstance();
//            FunctionDescriptor descriptor = FunctionDescriptor.of(CLinker.C_INT, CLinker.C_POINTER);
//            LibraryLookup lookup = LibraryLookup.ofDefault();
//            Symbol symbol = lookup.lookup("printf");
//            FunctionHandle handle = linker.downcallHandle(symbol, descriptor);
//
//            String message = "Hello, World!";
//            MemoryAccess.setCString(segment.baseAddress(), message);
//            int result = (int) handle.invokeExact(segment.baseAddress());
//
//            System.out.println("Result: " + result);
//        }
    }
}
