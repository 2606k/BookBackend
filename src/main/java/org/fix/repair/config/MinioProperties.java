package org.fix.repair.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gp.minio")
public class MinioProperties {
    private String endpointUrl;
    private String accessKey;
    private String secreKey;
    private String bucketName;
}
