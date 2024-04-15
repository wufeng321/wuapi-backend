package com.wufeng.project.service;

import com.wufeng.project.model.entity.InterfaceInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author wufeng
* @description 针对表【interface_info(接口信息)】的数据库操作Service
* @createDate 2024-04-15 11:30:54
*/
public interface InterfaceInfoService extends IService<InterfaceInfo> {
    void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add);
}
