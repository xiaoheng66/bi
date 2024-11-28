package com.yuheng.springbootinit.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *  队列测试
 *
 * @author YuhengZhang
 *
 */
@RestController
@RequestMapping("/queue")
@Slf4j
@Profile({"dev", "local"})
public class QueueController {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;


    // 提交任务到线程池
    @GetMapping("/add")
    public void add(String name){

        CompletableFuture.runAsync(() -> {
            System.out.println("任务执行中" + name + "...." + "执行的线程是: " + Thread.currentThread().getName());
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },threadPoolExecutor);
    }


    // 查看线程池的状态
    @GetMapping("/get")
    public String get(){

        Map<String,Object> map = new HashMap<>();


        int size = threadPoolExecutor.getQueue().size();
        map.put("队列长度", size);

        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("任务总数: ", taskCount);

        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();

        map.put("已经成功执行完成的任务数: ", completedTaskCount);

        int activeCount = threadPoolExecutor.getActiveCount();

        map.put("正在工作的线程数: ", activeCount);


        return JSONUtil.toJsonStr(map);

    }


}
