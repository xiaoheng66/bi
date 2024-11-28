package com.yuheng.springbootinit.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuheng.springbootinit.ValidHandler.*;
import com.yuheng.springbootinit.annotation.AuthCheck;
import com.yuheng.springbootinit.bizmq.BiMessageProducer;
import com.yuheng.springbootinit.common.BaseResponse;
import com.yuheng.springbootinit.common.DeleteRequest;
import com.yuheng.springbootinit.common.ErrorCode;
import com.yuheng.springbootinit.common.ResultUtils;
import com.yuheng.springbootinit.constant.CommonConstant;
import com.yuheng.springbootinit.constant.FileConstant;
import com.yuheng.springbootinit.constant.UserConstant;
import com.yuheng.springbootinit.exception.BusinessException;
import com.yuheng.springbootinit.exception.ThrowUtils;
import com.yuheng.springbootinit.manager.AIXingHuoManager;
import com.yuheng.springbootinit.manager.AiManager;
import com.yuheng.springbootinit.manager.RedisLimiterManager;
import com.yuheng.springbootinit.model.dto.chart.*;
import com.yuheng.springbootinit.model.dto.file.UploadFileRequest;
import com.yuheng.springbootinit.model.dto.post.PostQueryRequest;
import com.yuheng.springbootinit.model.entity.Chart;
import com.yuheng.springbootinit.model.entity.Post;
import com.yuheng.springbootinit.model.entity.User;
import com.yuheng.springbootinit.model.enums.FileUploadBizEnum;
import com.yuheng.springbootinit.model.vo.BiResponse;
import com.yuheng.springbootinit.service.ChartService;
import com.yuheng.springbootinit.service.UserService;
import com.yuheng.springbootinit.utils.ExcelUtils;
import com.yuheng.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *   智能分析接口
 *
 * @author YuhengZhang
 *
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;



    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;


    @Resource
    private BiMessageProducer biMessageProducer;

    @Resource
    private AIXingHuoManager aiXingHuoManager;


    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);

        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }



    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                 getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);

        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }




    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 采用责任链模式进行数据安全性校验
        // 构造校验链
        ValidationHandler goalHandler = new GoalValidationHandler();
        ValidationHandler nameHandler = new NameValidationHandler();
        ValidationHandler fileSizeHandler = new FileSizeValidationHandler();
        ValidationHandler fileSuffixHandler = new FileSuffixValidationHandler();
        ValidationHandler contentModerationHandler = new ContentModerationHandler();
        fileSuffixHandler.setNextHandler(contentModerationHandler);

        goalHandler.setNextHandler(nameHandler);
        nameHandler.setNextHandler(fileSizeHandler);
        fileSizeHandler.setNextHandler(fileSuffixHandler);

        // 校验上下文
        ValidationContext validationContext = new ValidationContext(name, goal, multipartFile);

        // 执行校验
        goalHandler.validate(validationContext);


        User loginUser = userService.getLoginUser(request);
        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());


        long biModelId = CommonConstant.BI_MODEL_ID;
        // 分析需求：
        // 分析网站用户的增长情况
        // 原始数据：
        // 日期,用户数
        // 1号,10
        // 2号,20
        // 3号,30

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");


        // 如果想实现对数据的分片可以这么写 之后按片发送给ai  最后再返回汇总的数据


        /*public class CsvSplitter {

            public static List<String> splitCsv(String csvData, int maxLines, int chunkSize) {
                // 将 CSV 数据按行分割
                String[] lines = csvData.split("\n");
                int totalLines = lines.length;

                // 检查总行数是否超出限制
                if (totalLines > maxLines) {
                    System.out.println("行数超过 " + maxLines + "，进行拆分...");
                } else {
                    System.out.println("行数未超过 " + maxLines + "，无需拆分。");
                }

                // 分割成多个块
                List<String> chunks = new ArrayList<>();
                StringBuilder currentChunk = new StringBuilder();
                int currentLineCount = 0;

                for (int i = 0; i < totalLines; i++) {
                    currentChunk.append(lines[i]).append("\n");
                    currentLineCount++;

                    // 每到达一个块大小，或者是最后一行时，保存当前块
                    if (currentLineCount == chunkSize || i == totalLines - 1) {
                        chunks.add(currentChunk.toString());
                        currentChunk.setLength(0); // 清空当前块
                        currentLineCount = 0;
                    }
                }

                return chunks;
            }

        }*/







        String result = aiXingHuoManager.sendMsgToXingHuo(true, userInput.toString());
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setStatus("succeed");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }




    // 智能分析接口(异步)
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                            GenChartByAiRequest genChartByAiRequest,HttpServletRequest request) {

        String chartType = genChartByAiRequest.getChartType();
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,ErrorCode.PARAMS_ERROR,"名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        //校验大小
        ThrowUtils.throwIf(size > ONE_MB,ErrorCode.PARAMS_ERROR,"文件过大 超过1MB");
        //校验后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("png","jpg","xlsx");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        User loginUser = userService.getLoginUser(request);

        //限流判断
        redisLimiterManager.doRateLimit(String.valueOf("genChartByAi_" + loginUser.getId()));

        


//        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
//                "分析需求：\n" +
//                "{数据分析的需求或者目标}\n" +
//                "原始数据：\n" +
//                "{csv格式的原始数据，用,作为分隔符}\n" +
//                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
//                "【【【【【\n" +
//                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
//                "【【【【【\n" +
//                "{明确的数据分析结论、越详细越好，不要生成多余的注释}\n";


        long biModelId = 1820799198148100097L;


        // 拼接用户输入格式如下

//        分析需求:
//        分析网站用户增长情况
//        有图表类型的话  把图表类型也拼上
//        原始数据:
//        日期,用户数
//        1号,10
//        2号,20
//        3号,30

        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)){
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据: ").append("\n");

        //压缩好的csv数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");




        // 在处理前保存图表数据

        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败");


        CompletableFuture.runAsync(() -> {

            //先修改状态为正在执行
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            if (!b){

                handleChartUpdateError(chart.getId(), "更新图表状态为执行中  但是失败了");

                return;
            }



            //调用ai
            String result = aiXingHuoManager.sendMsgToXingHuo(true, userInput.toString());

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
                handleChartUpdateError(chart.getId(), "更新图表成功状态 失败了");
            }


        },threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }


    // 智能分析接口(异步消息队列)
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest,HttpServletRequest request) {

        String chartType = genChartByAiRequest.getChartType();
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,ErrorCode.PARAMS_ERROR,"名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        //校验大小
        ThrowUtils.throwIf(size > ONE_MB,ErrorCode.PARAMS_ERROR,"文件过大 超过1MB");
        //校验后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("png","jpg","xlsx");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        User loginUser = userService.getLoginUser(request);

        //限流判断
        redisLimiterManager.doRateLimit(String.valueOf("genChartByAi_" + loginUser.getId()));

        long biModelId = CommonConstant.BI_MODEL_ID;


        // 拼接用户输入格式如下

//        分析需求:
//        分析网站用户增长情况
//        有图表类型的话  把图表类型也拼上
//        原始数据:
//        日期,用户数
//        1号,10
//        2号,20
//        3号,30

        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)){
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据: ").append("\n");

        //压缩好的csv数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");




        // 在处理前保存图表数据

        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败");


        Long newChartId = chart.getId();

        // 把消费者需要的参数  发送过去  是单独处理  不是线程池了
        biMessageProducer.sendMessage(String.valueOf(newChartId));



        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(newChartId);

        return ResultUtils.success(biResponse);
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




    //查询的封装类
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }



        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();


        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(name),"name",name);
        queryWrapper.eq("isDelete", false);


        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
