package com.yuheng.springbootinit.bizmq;


import com.rabbitmq.client.Channel;
import com.yuheng.springbootinit.common.ErrorCode;
import com.yuheng.springbootinit.constant.CommonConstant;
import com.yuheng.springbootinit.exception.BusinessException;
import com.yuheng.springbootinit.manager.AIXingHuoManager;
import com.yuheng.springbootinit.manager.AiManager;
import com.yuheng.springbootinit.model.entity.Chart;
import com.yuheng.springbootinit.service.ChartService;
import com.yuheng.springbootinit.utils.ExcelUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class BiMessageConsumer {


    @Resource
    private ChartService chartService;

    @Resource
    private AIXingHuoManager aiXingHuoManager;




    @SneakyThrows
    // 给方法自动填充对象
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){

log.info("receiveMessage message = {}", message);

        if (StringUtils.isBlank(message)){

            //消息拒绝
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }

        long chartId = Long.parseLong(message);

        Chart chart = chartService.getById(chartId);

        if (chart == null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"图表为空");
        }


        //先修改状态为正在执行
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if (!b){
            channel.basicNack(deliveryTag,false,false);

            handleChartUpdateError(chart.getId(), "更新图表状态为执行中  但是失败了");

            return;
        }
        //调用ai
       // String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, buildUserInput(chart));
        String result = aiXingHuoManager.sendMsgToXingHuo(true, buildUserInput(chart));
        String[] splits = result.split("【【【【【");

        if (splits.length < 3){

            handleChartUpdateError(chart.getId(), "AI生成错误");
            return;
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenResult(genResult);
        updateChartResult.setGenChart(genChart);
        updateChartResult.setStatus("succeed");
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult){
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(), "更新图表成功状态 失败了");
        }


        //  执行成功    消息确认
        channel.basicAck(deliveryTag,false);



    }

    // 构建用户输入
    private String buildUserInput(Chart chart){

        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();


        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)){
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据: ").append("\n");

        userInput.append(csvData).append("\n");

        return userInput.toString();

    }


    private void handleChartUpdateError(long chatId, String execMessage){

        Chart updateChartResult = new Chart();
        updateChartResult.setId(chatId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChartResult);

        if (!updateResult){
            log.error("更新图表失败状态------>>>>>>失败了" + chatId + "     " + execMessage);
        }
    }









}
