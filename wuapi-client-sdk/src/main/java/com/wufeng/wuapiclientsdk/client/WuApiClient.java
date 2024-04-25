package com.wufeng.wuapiclientsdk.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.wufeng.wuapiclientsdk.model.User;
import com.wufeng.wuapiclientsdk.utils.SignUtils;

import java.util.HashMap;

/**
 * @author wufeng
 * @date 2024/4/17 16:52
 * @Description:
 */
public class WuApiClient {

    private static final String GATEWAY_HOST = "http://localhost:8090";

    private String accessKey;
    private String secretKey;


    public WuApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getNameByGet(String name){
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String result = HttpUtil.get(GATEWAY_HOST+"/api/name/getNameByGet", paramMap);
        System.out.println(result);
        return result;
    }
    public String getNameByPost(String name){
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String result = HttpUtil.post(GATEWAY_HOST+"/api/name/getNameByPost", paramMap);
        System.out.println(result);
        return result;
    }
    public HttpResponse getUserNameByPost(User user){
        String json = JSONUtil.toJsonStr(user);
        HttpResponse httpReponse = HttpRequest.post(GATEWAY_HOST+"/api/name/getUserNameByPost")
                .addHeaders(getHeaderMap(json))
                .body(json)
                .execute();
        String result = httpReponse.body();
        System.out.println(result);
        return httpReponse;
    }

    private HashMap<String, String> getHeaderMap(String body){
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("accessKey", accessKey);
        headerMap.put("noce", RandomUtil.randomNumbers(100));
        headerMap.put("body", body);
        headerMap.put("timestamp", String.valueOf(System.currentTimeMillis()/1000));
        headerMap.put("sign", SignUtils.genSign(body, secretKey));
        return headerMap;
    }
}
