package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.service.DataBoardService;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-service/dashboard")
public class DataBoardController {

    private final DataBoardService dataBoardService;

    public DataBoardController(DataBoardService dataBoardService) {
        this.dataBoardService = dataBoardService;
    }

    @GetMapping("/overview")
    public Result<?> overview(@RequestHeader(value = "token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return Result.error(401, "未登录，请先登录");
        }
        try {
            Integer roleType = DoctorJwtUtil.getRoleType(token);
            if (roleType == null || roleType != 3) {
                return Result.error(403, "仅管理员可访问数据看板");
            }
        } catch (Exception exception) {
            return Result.error(401, "管理员登录凭证无效");
        }
        return Result.ok(dataBoardService.getOverview());
    }
}
