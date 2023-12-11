package com.ebanking.service1.service;

import com.ebanking.service1.entities.Users;

import java.util.List;

public interface UserService {
    public Users findByEmail(String email);
//    public Users save(Users theUser);
//    public Users findByAccountNo(BigInteger accountNo);
    public Users findByResetPasswordToken(String token);
//    public void updateResetPasswordToken(String token, String email);
//    public void updatePassword(String password, String token);
//    public Users findByOTP(String otp);
//    public void updateStatus(String otp);
//    void updateOtp(String otp, String email);
    void deleteAccount(String email);

    void addUser(String id);
    List<Object[]> getUsers();
}
