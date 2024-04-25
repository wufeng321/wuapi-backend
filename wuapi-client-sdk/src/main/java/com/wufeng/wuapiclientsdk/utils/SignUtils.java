package com.wufeng.wuapiclientsdk.utils;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;

/**
 * @author wufeng
 * @date 2024/4/18 10:44
 * @Description:
 */
public class SignUtils {

    /**
     * 生成签名
     * @param body 请求体内容
     * @param secretKey 秘钥
     * @return 生成的签名字符串
     */
    public static String genSign(String body, String secretKey) {
        Digester md5 = new Digester(DigestAlgorithm.SHA256);
        String content = body + secretKey;
        return md5.digestHex(content);
    }
}
