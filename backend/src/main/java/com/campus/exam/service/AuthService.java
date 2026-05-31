package com.campus.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exam.dto.LoginRequest;
import com.campus.exam.dto.LoginResponse;
import com.campus.exam.entity.RolePermission;
import com.campus.exam.entity.User;
import com.campus.exam.mapper.RolePermissionMapper;
import com.campus.exam.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.auth.token-hours:12}")
    private long tokenHours;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username())
                .eq(User::getEnabled, 1));
        if (user == null || !matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(user.getId(), Instant.now().plusSeconds(tokenHours * 3600)));
        List<String> permissions = permissions(user.getRole());
        user.setPassword(null);
        return new LoginResponse(token, user, permissions);
    }

    public List<String> permissions(String role) {
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRole, role))
                .stream()
                .map(RolePermission::getPermissionCode)
                .toList();
    }

    public User requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("请先登录");
        }
        Session session = sessions.get(token);
        if (session == null || session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            throw new IllegalArgumentException("登录已过期");
        }
        User user = userMapper.selectById(session.userId());
        if (user == null || user.getEnabled() == null || user.getEnabled() != 1) {
            throw new IllegalArgumentException("用户不可用");
        }
        user.setPassword(null);
        return user;
    }

    public String encodePassword(String raw) {
        return passwordEncoder.encode(raw);
    }

    public boolean isEncoded(String value) {
        return value != null && value.startsWith("$2");
    }

    public boolean matchesPassword(String raw, String stored) {
        return matches(raw, stored);
    }

    private boolean matches(String raw, String stored) {
        return stored != null && (passwordEncoder.matches(raw, stored) || stored.equals(raw));
    }

    private record Session(Long userId, Instant expiresAt) {
    }
}
