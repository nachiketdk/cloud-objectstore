package com.ebanking.service1.service;

import javax.transaction.Transactional;

import com.ebanking.service1.entities.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ebanking.service1.repository.UserRepository;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

//    @Autowired
//    PasswordEncoder passwordEncoded;

    @Override
    public Users findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

//    @Override
//    public Users save(Users theUser) {
//        theUser.setPassword(passwordEncoded.encode(theUser.getPassword()));
//        return userRepository.save(theUser);
//    }

//    @Override
//    public Users findByAccountNo(BigInteger accountNo) {
//        return userRepository.findByAccountNo(accountNo);
//    }

    @Override
    public Users findByResetPasswordToken(String token) {
        return userRepository.findByResetPasswordToken(token);
    }

//    @Override
//    public void updateResetPasswordToken(String token, String email) {
//        Users theUser = userRepository.findByEmail(email);
//        theUser.setResetPasswordToken(token);
//        userRepository.save(theUser);
//    }

//    @Override
//    public void updatePassword(String password, String token) {
//        Users theUser = userRepository.findByResetPasswordToken(token);
//        theUser.setPassword(passwordEncoded.encode(password));
//        theUser.setResetPasswordToken(null);
//        userRepository.save(theUser);
//    }

//    @Override
//    public Users findByOTP(String otp) {
//        return userRepository.findByotp(otp);
//    }

//    @Override
//    public void updateStatus(String otp) {
//        Users theUser = userRepository.findByotp(otp);
//        theUser.setStatus(1);
//        theUser.setOtp(null);
//        userRepository.save(theUser);
//    }

//    @Override
//    public void updateOtp(String otp, String email) {
//        Users theUser = userRepository.findByEmail(email);
//        theUser.setOtp(otp);
//        userRepository.save(theUser);
//    }

    @Override
    public void deleteAccount(String email) {
        userRepository.deleteUser(email);
    }

    @Override
    public void addUser(String id) {
        userRepository.addNewUser(id);
    }

    @Override
    public List<Object[]> getUsers() {
        return userRepository.findTotalUsers();
    }
}
