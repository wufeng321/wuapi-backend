package com.wufeng.wuapiinterface.controller;

import com.wufeng.wuapiclientsdk.model.User;
import com.wufeng.wuapiclientsdk.utils.SignUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author wufeng
 * @date 2024/4/17 15:58
 * @Description:
 */

@RestController
@RequestMapping("/name")
public class NameController {
    @GetMapping("/getNameByGet")
    public String getNameByGet(String name, HttpServletRequest request){
        System.out.println(request.getHeader("taco"));
        return "GET 你的名字是"+ name;
    }
    @PostMapping("/getNameByPost")
    public String getNameByPost(@RequestParam String name){
        return "POST 你的名字是"+ name;
    }
    @PostMapping("/getUserNameByPost")
    public String getUserNameByPost(@RequestBody User user, HttpServletRequest request){
        //// 从请求头中获取参数
        //String accessKey = request.getHeader("accessKey");
        //String nonce = request.getHeader("nonce");
        //String timeStamp = request.getHeader("timeStamp");
        //String sign = request.getHeader("sign");
        //String body = request.getHeader("body");
        //
        //// todo: 模拟校验
        //if(!accessKey.equals("taco")){
        //    throw new RuntimeException("无权限");
        //}
        //
        //String serveSign = SignUtils.genSign(body, "abcd");
        //if(!sign.equals(serveSign)){
        //    throw new RuntimeException("签名验证失败");
        //}

        String result = "POST 用户名字是"+ user.getUsername();
        return result;
    }
}
