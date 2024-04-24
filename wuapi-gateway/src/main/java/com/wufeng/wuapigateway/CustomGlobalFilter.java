package com.wufeng.wuapigateway;

import com.wufeng.wuapiclientsdk.utils.SignUtils;
import com.wufeng.wuapicommon.model.entity.InterfaceInfo;
import com.wufeng.wuapicommon.model.entity.User;
import com.wufeng.wuapicommon.model.entity.UserInterfaceInfo;
import com.wufeng.wuapicommon.service.InnerInterfaceInfoService;
import com.wufeng.wuapicommon.service.InnerUserInterfaceInfoService;
import com.wufeng.wuapicommon.service.InnerUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author wufeng
 * @date 2024/4/22 16:21
 * @Description: 全局过滤
 */
@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    @DubboReference
    private InnerInterfaceInfoService innerInterfaceInfoService;

    @DubboReference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;

    @DubboReference
    private InnerUserService innerUserService;


    private static final List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1");
    private static final String INTERFACE_HOST = "http://localhost:8123";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.用户发送请求到 API 网关
        // 2.请求日志
        ServerHttpRequest request = exchange.getRequest();
        String path = INTERFACE_HOST+ request.getPath().value();
        String method = request.getMethod().toString();
        log.info("请求唯一标识："+request.getId());
        log.info("请求路径："+path);
        log.info("请求方法："+method);
        log.info("请求参数："+request.getQueryParams());
        log.info("请求来源地址："+request.getRemoteAddress());
        String sourceAddress = request.getLocalAddress().getHostString();
        log.info("请求来源地址："+sourceAddress);
        // 3.访问控制-(黑白名单)
        ServerHttpResponse response = exchange.getResponse();
        if (!IP_WHITE_LIST.contains(sourceAddress)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }
        // 4. 用户鉴权(判断 ak、sk 是否合法)
        // 从请求头中获取参数
        HttpHeaders headers = request.getHeaders();
        String accessKey = headers.getFirst("accessKey");
        String nonce = headers.getFirst("noce");
        String timeStamp = headers.getFirst("timeStamp");
        String sign = headers.getFirst("sign");
        String body = headers.getFirst("body");

        User invokeUser = null;
        // 使用try catch 捕获异常
        try {
            invokeUser = innerUserService.getInvokeUser(accessKey);
        } catch (Exception e){
            log.error("getInvokeUser error", e);
        }
        if(invokeUser==null){
            return HandleNoAuth(response);
        }
        if(Long.parseLong(nonce.substring(0,3))>10000L){
            return HandleNoAuth(response);
        }
        // 时间和当前时间超过5分钟
        long currentTime = System.currentTimeMillis() / 1000;
        final long FIVE_MINUTES = 60 * 5L;
        if((currentTime-Long.parseLong(timeStamp))>=FIVE_MINUTES){
            return HandleNoAuth(response);
        }

        String secretKey = invokeUser.getSecretKey();
        // 使用获取到的密钥进行签名验证
        String serverSign = SignUtils.genSign(body, secretKey);
        if(sign!=null&&!sign.equals(serverSign)){
            return HandleNoAuth(response);
        }

        // 5.请求的模拟接口是否存在?
        InterfaceInfo interfaceInfo = null;
        // 使用try catch 捕获异常
        try {
            interfaceInfo = innerInterfaceInfoService.getInterfaceInfo(path, method);
        } catch (Exception e){
            log.error("getInterfeceInfo error", e);
        }
        if(interfaceInfo==null){
            return HandleNoAuth(response);
        }
        // 校验接口是否有剩余调用次数
        UserInterfaceInfo userInterfaceInfo = null;
        // 使用try catch 捕获异常
        try {
            userInterfaceInfo = innerUserInterfaceInfoService.getUserInterfaceInfoOfGtLeftNum(interfaceInfo.getId(), invokeUser.getId());
        } catch (Exception e){
            log.error("getUserInterfeceInfo error", e);
        }
        if(userInterfaceInfo==null){
            return HandleNoAuth(response);
        }
        // 6.请求转发，调用模拟接口
        //Mono<Void> filter = chain.filter(exchange);
        //log.info("响应："+response.getStatusCode());
        // 7.响应日志
        return HandleResponse(exchange, chain, interfaceInfo.getId(), invokeUser.getId());
    }

    @Override
    public int getOrder() {
        return -1;
    }

    /**
     * 处理鉴权不通过
     * @param response
     * @return
     */
    public Mono<Void> HandleNoAuth(ServerHttpResponse response){
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    public Mono<Void> HandleInvokeError(ServerHttpResponse response){
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return response.setComplete();
    }

    /**
     * 处理响应(使用装饰器模式增强原始过滤器)
     * @param exchange
     * @param chain
     * @return
     */
    public Mono<Void> HandleResponse(ServerWebExchange exchange, GatewayFilterChain chain, Long interfaceInfoId, Long userId){
        try {
            // 获取原始的响应对象
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 获取数据缓冲工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            // 获取响应状态码
            HttpStatus statusCode = originalResponse.getStatusCode();
            // 判断状态码是否为200 OK(按道理来说，现在还没有调用，是拿不到响应码的，对这个保持怀疑)
            if (statusCode == HttpStatus.OK) {
                // 创建一个装饰后的响应对象（开始穿装备，增强能力）
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                    // 重写writeWith方法，用于处理响应体数据
                    // 这段方法就是等我模拟接口调用完成之后，等他返回结果，就会调用writeWith方法，就能根据响应结果做一些处理
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        // 判断响应体是否为Flux类型
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            // 返回一个处理后的响应体
                            // （这里可以理解为它在拼接字符串，它把缓冲区的数据读取出来，一点一点拼接好）
                            return super.writeWith(fluxBody.map(dataBuffer -> {
                                // 8.调用成功，接口调用次数 +1 invokeCount
                                try {
                                    // 调用内部用户接口信息服务，记录接口调用次数
                                    innerUserInterfaceInfoService.invokeCount(interfaceInfoId, userId);
                                }catch (Exception e){
                                    log.error("invokeCount error", e);
                                }
                                // 获取响应体的内容并转化为字节数组
                                byte[] content = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(content);
                                // 释放掉内存
                                DataBufferUtils.release(dataBuffer);
                                // 构建返回日志
                                StringBuilder sb = new StringBuilder(200);
                                List<Object> rspArgs = new ArrayList<>();
                                rspArgs.add(originalResponse.getStatusCode());
                                String data = new String(content, StandardCharsets.UTF_8);
                                sb.append(data);
                                log.info("响应结果："+data);
                                // 将处理后的内容重新包装成DataBuffer对象并返回
                                return bufferFactory.wrap(content);
                            }));
                        } else {
                            log.error("<-- {} 响应code异常", getStatusCode());
                        }
                        return super.writeWith(body);
                    }
                };
                // 对于200 OK的请求，将装饰后的响应对象传递给下一个过滤器链，并继续处理（设置response对象为装饰过的）
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            // 对于非200 OK的请求，直接降级处理返回数据
            return chain.filter(exchange);
        } catch (Exception e) {
            log.error("网关处理异常" + e);
            return chain.filter(exchange);
        }
    }
}


