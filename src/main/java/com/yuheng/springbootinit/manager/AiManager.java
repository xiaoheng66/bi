package com.yuheng.springbootinit.manager;


import com.yuheng.springbootinit.common.ErrorCode;
import com.yuheng.springbootinit.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import io.github.briqt.spark4j.SparkClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AiManager {



    @Resource
    private YuCongMingClient yuCongMingClient;


    public String doChat(long modelId,String message){
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(message);

        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);


        if (response == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI响应错误");
        }


        return response.getData().getContent();
    }



}
