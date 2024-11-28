package com.yuheng.springbootinit.ValidHandler;

import com.alibaba.excel.util.StringUtils;
import com.yuheng.springbootinit.common.ErrorCode;
import com.yuheng.springbootinit.exception.ThrowUtils;

// 校验名称
public class NameValidationHandler extends ValidationHandler {
    @Override
    public void validate(ValidationContext context) {
        String name = context.getName();
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        super.validate(context);
    }
}
