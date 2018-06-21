package test.java.util;

import org.junit.Test;

import java.util.Arrays;

public class ArraysTest {

    @Test
    public void copyOf(){
        int[] a = {1,2,3};
        long[] b = {};
        System.arraycopy(a,0, b, 0, 3);
    }

    @Test
    public void a(){
        java.io.File file = new java.io.File("/");
        System.out.println(file.getAbsoluteFile());
    }

}
