package org.exam.hahaha;

import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class CallableClass implements Callable<Object> {
    @Override
    public Fish call() throws Exception {
        Fish result = new Fish();
        for (int i=0;i<100;i++)
        {
            result.age = result.age+i;
            result.weight = result.weight.concat(String.valueOf(result.age));
        }
        return result;
    }

    static class Fish{
        public String weight;
        public int age;
    }
}
