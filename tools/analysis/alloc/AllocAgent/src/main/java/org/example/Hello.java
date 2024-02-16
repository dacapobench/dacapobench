package org.example;

public class Hello {

    private static int count = 0;

//    static {
//        System.loadLibrary("hello");
//    }
//
//    public static native void hello_object(Object o, int allocation_site);
    public static void hello(int allocation_site) {
//        System.out.println("HELLO");
        ++count;
    }
}
