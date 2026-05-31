package com.campus.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exam.entity.Notification;
import com.campus.exam.entity.User;
import com.campus.exam.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationMapper notificationMapper;

    public List<Notification> list(User user) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .and(w -> w.eq(Notification::getUserId, user.getId()).or().eq(Notification::getRole, user.getRole()))
                .orderByDesc(Notification::getId));
    }

    public Notification create(Long userId, String role, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setRole(role);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
        return notification;
    }

    public void markRead(Long id, User user) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            return;
        }
        boolean own = user.getId().equals(notification.getUserId()) || user.getRole().equals(notification.getRole());
        if (own) {
            notification.setReadFlag(1);
            notificationMapper.updateById(notification);
        }
    }
}
