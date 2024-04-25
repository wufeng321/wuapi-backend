package com.wufeng.wuapiclientsdk;

import com.wufeng.wuapiclientsdk.client.WuApiClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author wufeng
 * @date 2024/4/18 14:16
 * @Description:
 */
// 通过@Configuation注解，将WuApiClientConfig类标记为一个配置类，告诉Spring Boot需要加载该配置类。
@Configuration
// 能够读取application.yml的配置,读取到配置之后,把这个读到的配置设置到我们这里的属性中，
// 这里给所有的配置加上前缀为"wuapi.client"
@ConfigurationProperties("wuapi.client")
// lombok注解，用于自动生成getter和setter方法
@Data
// @ComponentScan注解，用于扫描指定包下的所有组件，并将其注册到Spring容器中。
@ComponentScan
public class WuApiClientConfig {

    private String accessKey;

    private String secretKey;

    // 创建一个WuApiClient实例，并将其注册到Spring容器中。
    @Bean
    public WuApiClient wuApiClient() {
        // 创建WuApiClient实例时，传入accessKey和secretKey
        return new WuApiClient(accessKey, secretKey);
    }
}
