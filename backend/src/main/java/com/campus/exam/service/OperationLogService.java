package com.campus.exam.service;

import com.campus.exam.config.AuthContext;
import com.campus.exam.entity.OperationLog;
import com.campus.exam.entity.User;
import com.campus.exam.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogMapper operationLogMapper;

    public void log(String action, String target, String detail) {
        OperationLog log = new OperationLog();
        try {
            User current = AuthContext.user();
            log.setUserId(current.getId());
            log.setUsername(current.getUsername());
            log.setRole(current.getRole());
        } catch (Exception ignored) {
            log.setUsername("SYSTEM");
            log.setRole("SYSTEM");
        }
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
