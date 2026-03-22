package com.nitin.saas.auth.event;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.entity.EmailVerificationToken;

public class UserRegisteredEvent {

    private final User user;
    private final EmailVerificationToken token;

    public UserRegisteredEvent(User user, EmailVerificationToken token) {
        this.user = user;
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public EmailVerificationToken getToken() {
        return token;
    }
}