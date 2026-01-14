package com.nitin.saas.auth.service;


import com.nitin.saas.auth.entity.EmailVerificationToken;
import com.nitin.saas.auth.entity.PasswordResetToken;
import com.nitin.saas.auth.entity.RefreshToken;
import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.EmailVerificationTokenRepository;
import com.nitin.saas.auth.repository.PasswordResetTokenRepository;
import com.nitin.saas.auth.repository.RefreshTokenRepository;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.common.exception.BadRequestException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.time.LocalDateTime;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;


    public AuthService(UserRepository userRepository, EmailVerificationTokenRepository tokenRepository, PasswordResetTokenRepository passwordResetTokenRepository,  RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder=new BCryptPasswordEncoder();
        this.tokenRepository=tokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public User register(String email, String password){

        if(userRepository.findByEmail(email).isPresent()){
            throw new BadRequestException("Email already exists");
        }
        User user=new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        User savedUser=userRepository.save(user);

        String token=java.util.UUID.randomUUID().toString();

        EmailVerificationToken verificationToken=new EmailVerificationToken(token,savedUser, LocalDateTime.now().plusMinutes(15));

        tokenRepository.save(verificationToken);

        System.out.println(
                "VERIFY EMAIL LINK: http://localhost:3030/api/auth/verify-email?token=" + token
        );
        return savedUser;
    }

    public User login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getAccountLockedUntil() != null &&
                user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Account locked. Try later.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {

            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(15));
            }

            userRepository.save(user);
            throw new BadRequestException("Invalid password");
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        if (!user.isEmailVerified()) {
            throw new BadRequestException("Please verify your email");
        }

        return user;
    }

    public void verifyEmail(String token){
        EmailVerificationToken verificationToken=tokenRepository.findByToken(token).orElseThrow(()->new BadRequestException("Invalid token"));

        if(verificationToken.getExpiryTime().isBefore(LocalDateTime.now())){
            throw new BadRequestException("Token expired");
        }
        User user=verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);

    }

    public void resendEmailVerificationCode(String email) {
        User user=userRepository.findByEmail(email).orElseThrow(()->new BadRequestException("User not found"));
        if(user.isEmailVerified()){ throw new BadRequestException("Email already verified");}
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);
        String token=java.util.UUID.randomUUID().toString();
        EmailVerificationToken verificationToken=new EmailVerificationToken(token,user, LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(verificationToken);
        System.out.println("VERIFY EMAIL LINK: http://localhost:3030/api/auth/verify-email?token=" + token);
    }

    public void forgetPassword(String email){
        User user=userRepository.findByEmail(email).orElseThrow(()->new BadRequestException("User not found"));
        if(!user.isEmailVerified()){ throw new BadRequestException("Please verify your email");}
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);
        String token =java.util.UUID.randomUUID().toString();
        PasswordResetToken resetToken=new PasswordResetToken(token,user,LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.save(resetToken);
        System.out.println("RESET PASSWORD LINK: http://localhost:3030/api/auth/reset-password?token=" + token);
    }

    public void resetPassword(String token,String newPassword){
        PasswordResetToken resetToken=passwordResetTokenRepository.findByToken(token).orElseThrow(()->new BadRequestException("Invalid token"));
        if(resetToken.getExpiryTime().isBefore(LocalDateTime.now())){
            throw new BadRequestException("Token expired");
        }
        User user=resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
    }

    @Transactional
    public String createRefreshToken(User user){
        refreshTokenRepository.findByUser(user).ifPresent(token->{
            refreshTokenRepository.delete(token);
            refreshTokenRepository.flush();
        });
        String token=java.util.UUID.randomUUID().toString();
        RefreshToken refreshToken=new RefreshToken(token,user,LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);
        return token;

    }
}
