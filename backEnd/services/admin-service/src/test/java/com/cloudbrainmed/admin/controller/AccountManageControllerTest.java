package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.exception.AdminAuthException;
import com.cloudbrainmed.admin.service.AccountService;
import com.cloudbrainmed.admin.vo.AdminProfileVo;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 校验管理员资料接口的鉴权入口 {@code extractAdminId}：
 * 管理员身份只能来自合法且 roleType=3 的 JWT，任何非法 token 一律拒绝，
 * 且拒绝时绝不调用 AccountService。四个资料接口共用同一鉴权入口。
 *
 * 返回码语义：token 缺失/非法/篡改/过期/伪造/缺 userId → 401；
 * 合法 token 但角色非管理员（roleType != 3）→ 403。
 */
class AccountManageControllerTest {

    private AccountService accountService;
    private AccountManageController controller;

    @BeforeEach
    void setUp() throws Exception {
        accountService = mock(AccountService.class);
        controller = new AccountManageController();
        Field field = AccountManageController.class.getDeclaredField("accountService");
        field.setAccessible(true);
        field.set(controller, accountService);
    }

    private String adminToken(String adminId) {
        return DoctorJwtUtil.createToken(adminId, "13800000000", 3);
    }

    // ---- 合法管理员 token：放行，使用 JWT 中的 userId ----

    @Test
    void infoUsesUserIdFromValidAdminToken() {
        AdminProfileVo vo = new AdminProfileVo();
        when(accountService.getAdminInfo("A001")).thenReturn(vo);

        controller.info(adminToken("A001"));

        verify(accountService).getAdminInfo("A001");
    }

    @Test
    void updateUsesUserIdFromValidAdminToken() {
        controller.update(adminToken("A001"), Map.of("email", "a@b.com", "phone", "13800000000"));

        verify(accountService).updateAdminProfile("A001", "a@b.com", "13800000000");
    }

    @Test
    void changePasswordUsesUserIdFromValidAdminToken() {
        controller.changePassword(adminToken("A001"), Map.of("oldPassword", "old", "newPassword", "new"));

        verify(accountService).changePassword("A001", "old", "new");
    }

    @Test
    void avatarUploadUsesUserIdFromValidAdminToken() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(file.getOriginalFilename()).thenReturn("a.png");
        when(accountService.uploadAvatar("A001", new byte[]{1, 2, 3}, "a.png"))
                .thenReturn("http://cdn/a.png");

        controller.avatarUpload(adminToken("A001"), file);

        verify(accountService).uploadAvatar("A001", new byte[]{1, 2, 3}, "a.png");
    }

    // ---- 医生 token（roleType=2）：拒绝，403 无权限 ----

    @Test
    void rejectsDoctorTokenWith403() {
        String doctorToken = DoctorJwtUtil.createToken("D001", "13800000000", 2);

        assertThatThrownBy(() -> controller.info(doctorToken))
                .isInstanceOf(AdminAuthException.class)
                .hasMessageContaining("仅管理员");
        assertThat(((AdminAuthException) catchThrowable(() -> controller.info(doctorToken))).getCode())
                .isEqualTo(403);
        verifyNoInteractions(accountService);
    }

    // ---- 缺少 roleType 或 userId：拒绝 ----

    @Test
    void rejectsTokenMissingRoleType() {
        String token = io.jsonwebtoken.Jwts.builder()
                .claim("userId", "A001")
                .claim("phone", "13800000000")
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "medical-cloud-jwt-secret-2026-very-long-key-32bytes".getBytes()))
                .compact();

        assertThatThrownBy(() -> controller.info(token))
                .isInstanceOf(AdminAuthException.class);
        verifyNoInteractions(accountService);
    }

    @Test
    void rejectsTokenMissingUserIdWith401() {
        String token = io.jsonwebtoken.Jwts.builder()
                .claim("roleType", 3)
                .claim("phone", "13800000000")
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "medical-cloud-jwt-secret-2026-very-long-key-32bytes".getBytes()))
                .compact();

        assertThatThrownBy(() -> controller.info(token))
                .isInstanceOf(AdminAuthException.class)
                .satisfies(e -> assertThat(((AdminAuthException) e).getCode()).isEqualTo(401));
        verifyNoInteractions(accountService);
    }

    // ---- 合法签名但 claim 类型不符（JJWT RequiredTypeException）：拒绝，401 ----

    @Test
    void rejectsTokenWithStringRoleTypeWith401() {
        String token = io.jsonwebtoken.Jwts.builder()
                .claim("userId", "A001")
                .claim("roleType", "3") // 字符串而非数字
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "medical-cloud-jwt-secret-2026-very-long-key-32bytes".getBytes()))
                .compact();

        assertThatThrownBy(() -> controller.info(token))
                .isInstanceOf(AdminAuthException.class)
                .satisfies(e -> assertThat(((AdminAuthException) e).getCode()).isEqualTo(401));
        verifyNoInteractions(accountService);
    }

    @Test
    void rejectsTokenWithNumericUserIdWith401() {
        String token = io.jsonwebtoken.Jwts.builder()
                .claim("userId", 12345) // 数字而非字符串
                .claim("roleType", 3)
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "medical-cloud-jwt-secret-2026-very-long-key-32bytes".getBytes()))
                .compact();

        assertThatThrownBy(() -> controller.info(token))
                .isInstanceOf(AdminAuthException.class)
                .satisfies(e -> assertThat(((AdminAuthException) e).getCode()).isEqualTo(401));
        verifyNoInteractions(accountService);
    }

    // ---- 伪造 / 篡改 / 过期 / 普通字符串 token：拒绝，401 未认证 ----

    @Test
    void rejectsPlainStringTokenWith401() {
        assertThatThrownBy(() -> controller.info("A001"))
                .isInstanceOf(AdminAuthException.class)
                .hasMessageContaining("无效")
                .satisfies(e -> assertThat(((AdminAuthException) e).getCode()).isEqualTo(401));
        verifyNoInteractions(accountService);
    }

    @Test
    void rejectsTamperedToken() {
        String token = adminToken("A001");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> controller.info(tampered))
                .isInstanceOf(AdminAuthException.class);
        verifyNoInteractions(accountService);
    }

    @Test
    void rejectsTokenSignedWithWrongKey() {
        String forged = io.jsonwebtoken.Jwts.builder()
                .claim("userId", "A001")
                .claim("roleType", 3)
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "another-totally-different-secret-key-32bytes-long".getBytes()))
                .compact();

        assertThatThrownBy(() -> controller.info(forged))
                .isInstanceOf(AdminAuthException.class);
        verifyNoInteractions(accountService);
    }

    @Test
    void rejectsExpiredToken() {
        String expired = io.jsonwebtoken.Jwts.builder()
                .claim("userId", "A001")
                .claim("roleType", 3)
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 60000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "medical-cloud-jwt-secret-2026-very-long-key-32bytes".getBytes()))
                .compact();

        assertThatThrownBy(() -> controller.info(expired))
                .isInstanceOf(AdminAuthException.class);
        verifyNoInteractions(accountService);
    }

    // ---- 空 token：拒绝，401 未认证 ----

    @Test
    void rejectsNullTokenWith401() {
        assertThatThrownBy(() -> controller.info(null))
                .isInstanceOf(AdminAuthException.class)
                .hasMessageContaining("未登录")
                .satisfies(e -> assertThat(((AdminAuthException) e).getCode()).isEqualTo(401));
        verifyNoInteractions(accountService);
    }

    @Test
    void rejectsBlankToken() {
        assertThatThrownBy(() -> controller.info("   "))
                .isInstanceOf(AdminAuthException.class)
                .hasMessageContaining("未登录");
        verifyNoInteractions(accountService);
    }

    // ---- 四个资料接口均经过同一鉴权入口：非法 token 全部拒绝且不触达 service ----

    @Test
    void allFourProfileEndpointsRejectInvalidTokenBeforeCallingService() throws Exception {
        MultipartFile file = mock(MultipartFile.class);

        assertThatThrownBy(() -> controller.info("bad"))
                .isInstanceOf(AdminAuthException.class);
        assertThatThrownBy(() -> controller.update("bad", Map.of()))
                .isInstanceOf(AdminAuthException.class);
        assertThatThrownBy(() -> controller.changePassword("bad", Map.of()))
                .isInstanceOf(AdminAuthException.class);
        assertThatThrownBy(() -> controller.avatarUpload("bad", file))
                .isInstanceOf(AdminAuthException.class);

        verify(accountService, never()).getAdminInfo(org.mockito.ArgumentMatchers.anyString());
        verify(accountService, never()).updateAdminProfile(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(accountService, never()).changePassword(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(accountService, never()).uploadAvatar(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        // 头像接口在鉴权阶段就被拒，未读取文件内容
        verify(file, never()).getBytes();
        verifyNoInteractions(accountService);
    }

    private static Throwable catchThrowable(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            return t;
        }
        return null;
    }
}
