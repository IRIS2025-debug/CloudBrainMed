package com.cloudbrainmed.admin.service.impl;

import com.cloudbrainmed.admin.entity.Account;
import com.cloudbrainmed.admin.mapper.AccountMapper;
import com.cloudbrainmed.admin.service.AccountService;
import com.cloudbrainmed.admin.vo.AdminProfileVo;
import com.cloudbrainmed.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountMapper accountMapper;

    @Value("${upload.avatar.admin-dir:${user.dir}/uploads/avatar/admin}")
    private String avatarUploadDir;

    @Override
    public AdminProfileVo getAdminInfo(String adminId) {
        Account admin = accountMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员信息不存在");
        }
        AdminProfileVo vo = new AdminProfileVo();
        vo.setAdminId(admin.getAdminId());
        vo.setAvatar(admin.getAvatar());
        vo.setName(admin.getName());
        vo.setGender(admin.getGender());
        vo.setPhone(admin.getPhone());
        vo.setEmail(admin.getEmail());
        vo.setPosition(admin.getPosition());
        return vo;
    }

    @Override
    public void updateAdminProfile(String adminId, String email, String phone) {
        Account admin = accountMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员信息不存在");
        }
        if (email != null) admin.setEmail(email);
        if (phone != null) admin.setPhone(phone);
        accountMapper.updateById(admin);
    }

    @Override
    public String uploadAvatar(String adminId, byte[] fileBytes, String originalFilename) {
        // 1. 校验管理员存在
        Account admin = accountMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员信息不存在");
        }

        // 2. 提取扩展名，生成唯一文件名
        String ext = "";
        int dotIdx = originalFilename.lastIndexOf(".");
        if (dotIdx >= 0) {
            ext = originalFilename.substring(dotIdx);
        }
        String filename = adminId + "_" + UUID.randomUUID().toString().replace("-", "") + ext;

        // 3. 确保目录存在，写入文件到本地磁盘
        try {
            Path dir = Paths.get(avatarUploadDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            Files.write(target, fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("头像上传失败，无法保存文件", e);
        }

        // 4. 构造可访问的 URL 路径
        String avatarUrl = "/files/avatar/admin/" + filename;

        // 5. 更新数据库中的 avatar 字段
        admin.setAvatar(avatarUrl);
        accountMapper.updateById(admin);

        return avatarUrl;
    }

    @Override
    public void changePassword(String adminId, String oldPassword, String newPassword) {
        Account admin = accountMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员信息不存在");
        }
        if (!admin.getPassword().equals(oldPassword)) {
            throw new BusinessException("原密码错误");
        }
        admin.setPassword(newPassword);
        accountMapper.updateById(admin);
    }
}