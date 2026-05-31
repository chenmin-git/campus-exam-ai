package com.campus.exam.config;

import com.campus.exam.entity.User;

public final class AuthContext {
    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(User user) {
        CURRENT.set(user);
    }

    public static User user() {
        User user = CURRENT.get();
        if (user == null) {
            throw new IllegalStateException("未登录或登录已过期");
        }
        return user;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
