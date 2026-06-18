package com.cloudbrainmed.admin.service;

import com.cloudbrainmed.admin.vo.AdminProfileVo;

public interface AccountService {
    /**
     * 获取管理员个人信息
     */
    AdminProfileVo getAdminInfo(String adminId);

    /**
     * 更新管理员个人信息
     */
    void updateAdminProfile(String adminId, String email, String phone);

    /**
     * 上传头像
     *
     * @param adminId         管理员ID
     * @param fileBytes       文件字节数组
     * @param originalFilename 原始文件名
     * @return 可访问的头像 URL
     */
    String uploadAvatar(String adminId, byte[] fileBytes, String originalFilename);

    /**
     * 修改密码
     */
    void changePassword(String adminId, String oldPassword, String newPassword);
}