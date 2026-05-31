package com.campus.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exam.dto.ApiResponse;
import com.campus.exam.dto.NotificationRequest;
import com.campus.exam.entity.*;
import com.campus.exam.mapper.*;
import com.campus.exam.service.AuthService;
import com.campus.exam.service.NotificationService;
import com.campus.exam.service.OperationLogService;
import com.campus.exam.service.RoleGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final RoleGuard roleGuard;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final ClassInfoMapper classInfoMapper;
    private final TeacherCourseMapper teacherCourseMapper;
    private final AnnouncementMapper announcementMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final OperationLogMapper operationLogMapper;
    private final BackupRecordMapper backupRecordMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;
    private final AuthService authService;

    @GetMapping("/users")
    public ApiResponse<List<User>> users() {
        roleGuard.require("ADMIN");
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getId));
        users.forEach(u -> u.setPassword(null));
        return ApiResponse.ok(users);
    }

    @PostMapping("/users")
    public ApiResponse<User> saveUser(@RequestBody User user) {
        roleGuard.require("ADMIN");
        if (user.getEnabled() == null) {
            user.setEnabled(1);
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword("123456");
        }
        if (user.getId() == null) {
            if (!authService.isEncoded(user.getPassword())) {
                user.setPassword(authService.encodePassword(user.getPassword()));
            }
            userMapper.insert(user);
        } else {
            if (!user.getPassword().isBlank() && !authService.isEncoded(user.getPassword())) {
                user.setPassword(authService.encodePassword(user.getPassword()));
            } else if (user.getPassword().isBlank()) {
                User old = userMapper.selectById(user.getId());
                user.setPassword(old == null ? authService.encodePassword("123456") : old.getPassword());
            }
            userMapper.updateById(user);
        }
        user.setPassword(null);
        return ApiResponse.ok(user);
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        roleGuard.require("ADMIN");
        userMapper.deleteById(id);
        operationLogService.log("删除用户", "user:" + id, "删除用户");
        return ApiResponse.ok(null);
    }

    @GetMapping("/courses")
    public ApiResponse<List<Course>> courses() {
        roleGuard.require("ADMIN");
        return ApiResponse.ok(courseMapper.selectList(new LambdaQueryWrapper<Course>().orderByAsc(Course::getId)));
    }

    @PostMapping("/courses")
    public ApiResponse<Course> saveCourse(@RequestBody Course course) {
        roleGuard.require("ADMIN");
        if (course.getId() == null) {
            courseMapper.insert(course);
        } else {
            courseMapper.updateById(course);
        }
        operationLogService.log("保存课程", "course:" + course.getId(), course.getName());
        return ApiResponse.ok(course);
    }

    @DeleteMapping("/courses/{id}")
    public ApiResponse<Void> deleteCourse(@PathVariable Long id) {
        roleGuard.require("ADMIN");
        courseMapper.deleteById(id);
        operationLogService.log("删除课程", "course:" + id, "删除课程");
        return ApiResponse.ok(null);
    }

    @GetMapping("/classes")
    public ApiResponse<List<ClassInfo>> classes() {
        roleGuard.require("ADMIN");
        return ApiResponse.ok(classInfoMapper.selectList(new LambdaQueryWrapper<ClassInfo>().orderByAsc(ClassInfo::getId)));
    }

    @PostMapping("/classes")
    public ApiResponse<ClassInfo> saveClass(@RequestBody ClassInfo classInfo) {
        roleGuard.require("ADMIN");
        if (classInfo.getId() == null) {
            classInfoMapper.insert(classInfo);
        } else {
            classInfoMapper.updateById(classInfo);
        }
        operationLogService.log("保存班级", "class:" + classInfo.getId(), classInfo.getName());
        return ApiResponse.ok(classInfo);
    }

    @DeleteMapping("/classes/{id}")
    public ApiResponse<Void> deleteClass(@PathVariable Long id) {
        roleGuard.require("ADMIN");
        classInfoMapper.deleteById(id);
        operationLogService.log("删除班级", "class:" + id, "删除班级");
        return ApiResponse.ok(null);
    }

    @GetMapping("/teacher-courses")
    public ApiResponse<List<TeacherCourse>> teacherCourses() {
        roleGuard.require("ADMIN");
        return ApiResponse.ok(teacherCourseMapper.selectList(new LambdaQueryWrapper<TeacherCourse>().orderByAsc(TeacherCourse::getId)));
    }

    @PostMapping("/teacher-courses")
    public ApiResponse<TeacherCourse> saveTeacherCourse(@RequestBody TeacherCourse entity) {
        roleGuard.require("ADMIN");
        if (entity.getId() == null) {
            teacherCourseMapper.insert(entity);
        } else {
            teacherCourseMapper.updateById(entity);
        }
        operationLogService.log("保存授课安排", "teacherCourse:" + entity.getId(), String.valueOf(entity));
        return ApiResponse.ok(entity);
    }

    @DeleteMapping("/teacher-courses/{id}")
    public ApiResponse<Void> deleteTeacherCourse(@PathVariable Long id) {
        roleGuard.require("ADMIN");
        teacherCourseMapper.deleteById(id);
        operationLogService.log("删除授课安排", "teacherCourse:" + id, "删除授课安排");
        return ApiResponse.ok(null);
    }

    @GetMapping("/announcements")
    public ApiResponse<List<Announcement>> announcements() {
        roleGuard.require("ADMIN");
        return ApiResponse.ok(announcementMapper.selectList(new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getId)));
    }

    @PostMapping("/announcements")
    public ApiResponse<Announcement> saveAnnouncement(@RequestBody Announcement announcement) {
        User admin = roleGuard.require("ADMIN");
        if (announcement.getCreatorId() == null) {
            announcement.setCreatorId(admin.getId());
        }
        if (announcement.getEnabled() == null) {
            announcement.setEnabled(1);
        }
        if (announcement.getCreatedAt() == null) {
            announcement.setCreatedAt(LocalDateTime.now());
        }
        if (announcement.getId() == null) {
            announcementMapper.insert(announcement);
        } else {
            announcementMapper.updateById(announcement);
        }
        operationLogService.log("保存公告", "announcement:" + announcement.getId(), announcement.getTitle());
        return ApiResponse.ok(announcement);
    }

    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        roleGuard.require("ADMIN");
        announcementMapper.deleteById(id);
        operationLogService.log("删除公告", "announcement:" + id, "删除公告");
        return ApiResponse.ok(null);
    }

    @GetMapping("/permissions")
    public ApiResponse<List<Permission>> permissions() {
        roleGuard.require("ADMIN");
        return ApiResponse.ok(permissionMapper.selectList(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSortNo)));
    }

    @GetMapping("/role-permissions/{role}")
    public ApiResponse<List<String>> rolePermissions(@PathVariable String role) {
        roleGuard.require("ADMIN");
        return ApiResponse.ok(rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRole, role))
                .stream()
                .map(RolePermission::getPermissionCode)
                .toList());
    }

    @PostMapping("/role-permissions/{role}")
    public ApiResponse<Void> saveRolePermissions(@PathVariable String role, @RequestBody List<String> codes) {
        roleGuard.require("ADMIN");
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRole, role));
        for (String code : codes) {
            RolePermission entity = new RolePermission();
            entity.setRole(role);
            entity.setPermissionCode(code);
            rolePermissionMapper.insert(entity);
        }
        operationLogService.log("保存角色权限", role, String.join(",", codes));
        return ApiResponse.ok(null);
    }

    @GetMapping("/logs")
    public ApiResponse<List<OperationLog>> logs() {
        roleGuard.require("ADMIN");
        return ApiResponse.ok(operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getId)));
    }

    @GetMapping("/backup-records")
    public ApiResponse<List<BackupRecord>> backupRecords() {
        roleGuard.require("ADMIN");
        return ApiResponse.ok(backupRecordMapper.selectList(new LambdaQueryWrapper<BackupRecord>().orderByDesc(BackupRecord::getId)));
    }

    @PostMapping("/backup-records")
    public ApiResponse<BackupRecord> createBackup(@RequestBody(required = false) Map<String, String> body) {
        roleGuard.require("ADMIN");
        String remark = body == null ? null : body.get("remark");
        BackupRecord record = new BackupRecord();
        record.setName("逻辑备份-" + LocalDateTime.now());
        record.setStatus("CREATED");
        record.setRemark(remark);
        record.setCreatedAt(LocalDateTime.now());
        backupRecordMapper.insert(record);
        operationLogService.log("创建备份记录", "backup:" + record.getId(), remark);
        return ApiResponse.ok(record);
    }

    @PostMapping("/backup-records/{id}/restore")
    public ApiResponse<BackupRecord> restoreBackup(@PathVariable Long id) {
        roleGuard.require("ADMIN");
        BackupRecord record = backupRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("备份记录不存在");
        }
        record.setStatus("RESTORED");
        backupRecordMapper.updateById(record);
        operationLogService.log("恢复备份", "backup:" + id, "恢复逻辑备份记录");
        return ApiResponse.ok(record);
    }

    @PostMapping("/notifications")
    public ApiResponse<Notification> createNotification(@RequestBody NotificationRequest request) {
        roleGuard.require("ADMIN");
        return ApiResponse.ok(notificationService.create(request.userId(), request.role(), request.title(), request.content()));
    }
}
