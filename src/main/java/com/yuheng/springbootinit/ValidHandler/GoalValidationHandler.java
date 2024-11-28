package com.yuheng.springbootinit.ValidHandler;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.util.StringUtils;
import com.yuheng.springbootinit.common.ErrorCode;
import com.yuheng.springbootinit.exception.ThrowUtils;

import java.util.Arrays;
import java.util.List;

// 校验目标
public class GoalValidationHandler extends ValidationHandler {
    @Override
    public void validate(ValidationContext context) {
        String goal = context.getGoal();
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        super.validate(context);
    }
}

