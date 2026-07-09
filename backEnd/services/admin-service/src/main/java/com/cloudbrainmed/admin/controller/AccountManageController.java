package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.service.AccountService;
import com.cloudbrainmed.admin.vo.AdminProfileVo;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/admin-service/profile")
public class AccountManageController {

    @Resource
    private AccountService accountService;

    /**
     * 查询管理员个人信息
     */
    @GetMapping("/info")
    public Result<AdminProfileVo> info(@RequestHeader(value = "token", required = false) String token) {
        String adminId = extractAdminId(token);
        return Result.ok(accountService.getAdminInfo(adminId));
    }

    /**
     * 更新管理员个人信息
     */
    @PutMapping("/update")
    public Result<?> update(@RequestHeader(value = "token", required = false) String token,
                            @RequestBody Map<String, String> body) {
        String adminId = extractAdminId(token);
        accountService.updateAdminProfile(adminId, body.get("email"), body.get("phone"));
        return Result.ok();
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestHeader(value = "token", required = false) String token,
                                    @RequestBody Map<String, String> body) {
        String adminId = extractAdminId(token);
        accountService.changePassword(adminId, body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar-upload")
    public Result<?> avatarUpload(@RequestHeader(value = "token", required = false) String token,
                                  @RequestParam("file") MultipartFile file) throws IOException {
        String adminId = extractAdminId(token);
        String avatarUrl = accountService.uploadAvatar(adminId, file.getBytes(), file.getOriginalFilename());
        return Result.ok(java.util.Map.of("avatarUrl", avatarUrl));
    }

    /**
     * 从 JWT token 中提取 adminId
     */
    private String extractAdminId(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录，请先登录");
        }
        try {
            return DoctorJwtUtil.getUserId(token);
        } catch (Exception e) {
            return token;
        }
    }
}