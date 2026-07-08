package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.service.DataBoardService;
import com.cloudbrainmed.admin.support.AdminAuthHelper;
import com.cloudbrainmed.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin-service/dashboard")
public class DataBoardController {

    private final DataBoardService dataBoardService;
    private final AdminAuthHelper adminAuthHelper;

    public DataBoardController(
            DataBoardService dataBoardService,
            AdminAuthHelper adminAuthHelper) {
        this.dataBoardService = dataBoardService;
        this.adminAuthHelper = adminAuthHelper;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(
            @RequestHeader(value = "token", required = false) String token) {
        adminAuthHelper.requireAdmin(token);
        return Result.ok(dataBoardService.overview());
    }
}
