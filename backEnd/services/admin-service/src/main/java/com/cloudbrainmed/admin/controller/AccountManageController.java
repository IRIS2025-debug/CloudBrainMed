package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.exception.AdminAuthException;
import com.cloudbrainmed.admin.service.AccountService;
import com.cloudbrainmed.admin.vo.AdminProfileVo;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import io.jsonwebtoken.Claims;
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
     * 从已验证的 JWT token 中提取管理员 adminId。
     * 管理员身份只能来自合法 JWT，禁止把原始 token 当作 adminId 使用，
     * 也禁止从请求参数或请求体接收 adminId。
     */
    private String extractAdminId(String token) {
        if (token == null || token.isBlank()) {
            throw AdminAuthException.unauthorized("未登录，请先登录");
        }

        // 解析与字段提取都纳入异常转换范围：签名非法、格式错误、过期，
        // 以及 claim 类型不符（JJWT 抛 RequiredTypeException）统一视为无效凭证 → 401。
        Claims claims;
        Number roleType;
        String adminId;
        try {
            claims = DoctorJwtUtil.parseToken(token);
            roleType = claims.get("roleType", Number.class);
            adminId = claims.get("userId", String.class);
        } catch (Exception e) {
            throw AdminAuthException.unauthorized("管理员登录凭证无效");
        }

        // 凭证合法但角色不是管理员 → 403（区别于凭证本身无效的 401）。
        if (roleType == null || roleType.intValue() != 3) {
            throw AdminAuthException.forbidden("仅管理员可访问管理员资料");
        }

        if (adminId == null || adminId.isBlank()) {
            throw AdminAuthException.unauthorized("管理员登录凭证无效");
        }

        return adminId;
    }
}