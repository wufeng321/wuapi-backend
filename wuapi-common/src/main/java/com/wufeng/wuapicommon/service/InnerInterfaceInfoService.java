package com.wufeng.wuapicommon.service;

import com.wufeng.wuapicommon.model.entity.InterfaceInfo;

/**
* @author wufeng
* @description 针对表【interface_info(接口信息)】的数据库操作Service
* @createDate 2024-04-15 11:30:54
*/
public interface InnerInterfaceInfoService{
    /**
     * 从数据库中查询接口是否存在
     * @param path 请求路径
     * @param method 请求方法
     * @return
     */
    InterfaceInfo getInterfaceInfo(String path, String method);
}
