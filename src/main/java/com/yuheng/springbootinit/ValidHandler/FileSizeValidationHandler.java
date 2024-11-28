package com.yuheng.springbootinit.ValidHandler;

import com.yuheng.springbootinit.common.ErrorCode;
import com.yuheng.springbootinit.exception.ThrowUtils;

// 校验文件大小
public class FileSizeValidationHandler extends ValidationHandler {
    private static final long ONE_MB = 50 * 1024L;

    @Override
    public void validate(ValidationContext context) {
        long size = context.getMultipartFile().getSize();
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过 50kb");
        super.validate(context);
    }
}
