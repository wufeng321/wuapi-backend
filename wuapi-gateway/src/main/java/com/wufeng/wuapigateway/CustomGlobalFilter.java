package com.wufeng.wuapigateway;

import com.wufeng.wuapiclientsdk.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
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

    private static final List<String> IP_WHITE_LIST = Arrays.asList("127.0.0.1");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.用户发送请求到 API 网关
        // 2.请求日志
        ServerHttpRequest request = exchange.getRequest();
        log.info("请求唯一标识："+request.getId());
        log.info("请求路径："+request.getPath().value());
        log.info("请求方法："+request.getMethod());
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

        // todo: 模拟校验,实际是从数据库中查询
        if(!accessKey.equals("taco")){
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

        // todo:实际情况获取数据库中的ak、sk
        String serveSign = SignUtils.genSign(body, "abcd");
        if(!sign.equals(serveSign)){
            throw new RuntimeException("签名验证失败");
        }
        // 5.请求的模拟接口是否存在?
        // todo:从数据库中查询接口是否存在

        // 6.请求转发，调用模拟接口
        //Mono<Void> filter = chain.filter(exchange);
        //log.info("响应："+response.getStatusCode());
        // 7.响应日志
        return HandleResponse(exchange, chain);

        //log.info("custom global filter");
        //return filter;
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
    public Mono<Void> HandleResponse(ServerWebExchange exchange, GatewayFilterChain chain){
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
                                // 8. todo:调用成功，接口调用次数 +1 invokeCount
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


