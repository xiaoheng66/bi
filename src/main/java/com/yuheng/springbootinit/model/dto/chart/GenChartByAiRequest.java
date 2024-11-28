package com.yuheng.springbootinit.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenChartByAiRequest implements Serializable {

    //图表名称
    private String name;


    private String goal;

    private String chartType;



    private static final long serialVersionUID = 1L;



}
