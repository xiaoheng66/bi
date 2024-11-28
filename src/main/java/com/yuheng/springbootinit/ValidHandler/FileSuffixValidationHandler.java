package com.yuheng.springbootinit.ValidHandler;

import cn.hutool.core.io.FileUtil;
import com.yuheng.springbootinit.common.ErrorCode;
import com.yuheng.springbootinit.exception.ThrowUtils;

import java.util.Arrays;
import java.util.List;

// 校验文件后缀
public class FileSuffixValidationHandler extends ValidationHandler {
    private static final List<String> VALID_FILE_SUFFIX_LIST = Arrays.asList("xlsx", "xls");

    @Override
    public void validate(ValidationContext context) {
        String suffix = FileUtil.getSuffix(context.getMultipartFile().getOriginalFilename());
        ThrowUtils.throwIf(!VALID_FILE_SUFFIX_LIST.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
        super.validate(context);
    }
}
