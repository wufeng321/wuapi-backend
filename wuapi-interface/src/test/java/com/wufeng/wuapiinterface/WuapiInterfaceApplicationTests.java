package com.wufeng.wuapiinterface;

import com.wufeng.wuapiclientsdk.client.WuApiClient;
import com.wufeng.wuapiclientsdk.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class WuapiInterfaceApplicationTests {

    @Resource
    private WuApiClient wuApiClient;

    @Test
    void contextLoads() {
        String res1 = wuApiClient.getNameByGet("wufeng");
        User user = new User();
        user.setUsername("taco");
        //String res2 = wuApiClient.getUserNameByPost(user);
        System.out.println(res1);
        //System.out.println(res2);
    }

}
