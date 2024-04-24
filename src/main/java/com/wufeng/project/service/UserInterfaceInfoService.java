package com.wufeng.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wufeng.wuapicommon.model.entity.UserInterfaceInfo;

import java.util.List;

/**
* @author wufeng
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service
* @createDate 2024-04-22 09:59:32
*/
public interface UserInterfaceInfoService extends IService<UserInterfaceInfo> {

    void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add);

    /**
     * 调用接口统计
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceInfoId, long userId);

    /**
     * 查询接口调用次数top N的接口信息
     * @param limit
     * @return
     */
    List<UserInterfaceInfo> listTopInvokeInterfaceInfo(int limit);
}
