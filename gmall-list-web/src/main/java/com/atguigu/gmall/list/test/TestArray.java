package com.atguigu.gmall.list.test;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestArray {

    public static void main(String[] args){
        List<String> list = new ArrayList<>();

        for (int i =0;i<5;i++){
            list.add("数据"+i);
        }

        Iterator<String> iterator = list.iterator();

        while (iterator.hasNext()){
            String next = iterator.next();

            if(next.equals("数据3")){
                iterator.remove();
            }
        }

        System.out.println(JSON.toJSONString(list));
    }
}
