package com.wufeng.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wufeng.wuapicommon.model.entity.UserInterfaceInfo;

import java.util.List;

/**
* @author wufeng
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Mapper
* @createDate 2024-04-22 09:59:32
* @Entity com.wufeng.project.model.entity.UserInterfaceInfo
*/
public interface UserInterfaceInfoMapper extends BaseMapper<UserInterfaceInfo> {

    List<UserInterfaceInfo> listTopInvokeInterfaceInfo(int limit);

}




