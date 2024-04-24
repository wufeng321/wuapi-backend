package com.wufeng.wuapicommon.service;

import com.wufeng.wuapicommon.model.entity.UserInterfaceInfo;

/**
* @author wufeng
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service
* @createDate 2024-04-22 09:59:32
*/
public interface InnerUserInterfaceInfoService{
    /**
     * 调用接口统计
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceInfoId, long userId);

    /**
     * 查询剩余调用次数大于0的用户接口信息
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    UserInterfaceInfo getUserInterfaceInfoOfGtLeftNum(Long interfaceInfoId, Long userId);
}
