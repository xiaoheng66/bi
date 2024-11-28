package com.yuheng.springbootinit.ValidHandler;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingResponse;
import com.qcloud.cos.region.Region;
import com.yuheng.springbootinit.utils.ExcelUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

public class ContentModerationHandler extends ValidationHandler {

    private static final String SECRET_ID = "your_secret_id"; // 替换为实际的 secretId
    private static final String SECRET_KEY = "your_secret_key"; // 替换为实际的 secretKey
    private static final String REGION = "ap-shanghai"; // 替换为实际的 Region
    private static final String BUCKET_NAME = "your_bucket_name"; // 替换为实际的 BucketName

    @Override
    public void validate(ValidationContext context) {
        // 初始化腾讯云 COS 客户端
        COSCredentials cred = new BasicCOSCredentials(SECRET_ID, SECRET_KEY);
        ClientConfig clientConfig = new ClientConfig(new Region(REGION));
        COSClient client = new COSClient(cred, clientConfig);

        // 审核 goal 内容
        auditText(client, context.getGoal(), "目标内容");

        // 审核文件内容（CSV 文件）
        String csvData = ExcelUtils.excelToCsv(context.getMultipartFile());
        String fileContent = extractFileContent(context.getMultipartFile());
        auditText(client, fileContent, "文件内容");

        super.validate(context);
    }

    private void auditText(COSClient client, String text, String contentType) {
        if (text == null || text.isEmpty()) {
            return; // 跳过空内容的审核
        }
        try {
            // 转换内容为 Base64
            byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String encodedString = Base64.getEncoder().encodeToString(bytes);

            // 构造审核请求
            TextAuditingRequest request = new TextAuditingRequest();
            request.setBucketName(BUCKET_NAME);
            request.getInput().setContent(encodedString);

            // 发送审核请求
            TextAuditingResponse response = client.createAuditingTextJobs(request);

            // 查询审核结果
            String jobId = response.getJobsDetail().getJobId();
            request.setJobId(jobId);
            TextAuditingResponse queryResponse = client.describeAuditingTextJob(request);

            // 检查审核结果
            if (!"Normal".equals(queryResponse.getJobsDetail().getLabel())) {
                throw new RuntimeException(contentType + "涉嫌违规，请重新输入或上传合法内容！");
            }
        } catch (Exception e) {
            throw new RuntimeException(contentType + "审核失败：" + e.getMessage(), e);
        }
    }

    private String extractFileContent(MultipartFile file) {
        try {
            // 假设文件是 CSV 文件，直接读取内容为字符串
            return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("文件内容读取失败：" + e.getMessage(), e);
        }
    }
}
