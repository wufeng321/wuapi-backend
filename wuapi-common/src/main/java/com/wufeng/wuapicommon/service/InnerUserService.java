package com.wufeng.wuapicommon.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.wufeng.wuapicommon.model.entity.User;


/**
 * 用户服务
 *
 * @author wufeng
 */
public interface InnerUserService extends IService<User> {
    /**
     * 查询数据库中是否给用户分配秘钥（accessKey）
     * @param accessKey
     * @return
     */
    User getInvokeUser(String accessKey);
}
