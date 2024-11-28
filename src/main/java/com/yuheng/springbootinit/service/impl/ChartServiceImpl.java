package com.yuheng.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuheng.springbootinit.model.entity.Chart;
import com.yuheng.springbootinit.service.ChartService;
import com.yuheng.springbootinit.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author ZhangYuheng
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-08-05 14:41:40
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




