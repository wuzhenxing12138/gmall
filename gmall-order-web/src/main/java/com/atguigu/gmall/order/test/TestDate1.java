package com.atguigu.gmall.order.test;

import com.atguigu.gmall.bean.enums.OrderStatus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TestDate1 {

    public static void main(String[] args){

        // 时间格式化
        Date d = new Date();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH:mm:ss");
        String format = sdf.format(d);
        System.out.println(format);

        // 日期加减
        Calendar c = Calendar.getInstance();

        c.add(Calendar.DATE,-1);

        System.out.println(c.getTime());

        OrderStatus orderStatus =OrderStatus.UNPAID;

        String comment = orderStatus.getComment();

        System.out.println(comment);

    }
}
