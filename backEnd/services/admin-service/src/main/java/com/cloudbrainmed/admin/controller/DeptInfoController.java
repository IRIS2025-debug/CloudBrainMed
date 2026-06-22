package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.entity.DeptInfo;
import com.cloudbrainmed.admin.mapper.DeptInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudbrainmed.common.result.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-service/dept")
public class DeptInfoController {

    @Resource
    private DeptInfoMapper deptInfoMapper;

    /**
     * 获取所有启用的科室列表
     */
    @GetMapping("/list")
    public Result<List<DeptInfo>> list() {
        LambdaQueryWrapper<DeptInfo> qw = new LambdaQueryWrapper<>();
        qw.eq(DeptInfo::getStatus, 1);
        qw.orderByAsc(DeptInfo::getCreateTime);
        return Result.ok(deptInfoMapper.selectList(qw));
    }
}