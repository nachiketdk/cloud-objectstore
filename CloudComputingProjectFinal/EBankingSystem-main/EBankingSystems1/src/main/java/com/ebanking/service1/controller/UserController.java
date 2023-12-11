package com.ebanking.service1.controller;

import com.ebanking.service1.entities.Users;
import com.ebanking.service1.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
public class UserController {
//    @Autowired
//    private TransactionService transactionService;
//    @Autowired
//    private BalanceService balanceService;
//    @Autowired
//    private MailService mailService;
    @Autowired
    private UserService userService;

//    PasswordEncoder passwordEncoded;

    @PostMapping("/add-user/{id}")
    public ResponseEntity<?> addNewUser(@PathVariable String id){
        userService.addUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @GetMapping("/hello")
    public String add(){
//        userService.addUser(id);
        return "success";
    }
    @GetMapping("/get-all-users")
    public List<Object[]> getUsersList(){
        return userService.getUsers() ;
    }
//    @GetMapping("/hello")
//    public String currentUserName() {
//        return "Backend Running";
//    }

//
//    @GetMapping("/balance/{accountNo}")
//    public List<Balance> getBalance(@PathVariable BigInteger accountNo) {
//        return balanceService.getBalanceStatus(accountNo);
//    }
//
//    @GetMapping("transaction/{accountNo}")
//    public List<Transactions> getAllTransaction(@PathVariable BigInteger accountNo) {
//        return transactionService.getAllTransaction(accountNo);
//    }

//    @PostMapping("/mail")
//    public ResponseEntity<?> sendMail(@RequestBody Mail theMail) {
//        mailService.send(theMail.from, theMail.to, theMail.subject, theMail.body, theMail.sentDate);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }

//    @PostMapping("/forget-password")
//    public ResponseEntity<?> sendForgetPassword(@RequestBody Users theUser) {
//        if (userService.findByEmail(theUser.getEmail()) == null) {
//            return new ResponseEntity<>(HttpStatus.CONFLICT);
//        }
//        String token = RandomString.make(30);
//        userService.updateResetPasswordToken(token, theUser.getEmail());
//        String resetPasswordLink = "http://localhost:4200/reset-password;token=" + token;
//        mailService.sendMail(theUser.getEmail(), resetPasswordLink);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }

//    @PostMapping("/reset-password/{token}")
//    public ResponseEntity<?> resetPassword(@RequestBody Users theUser, @PathVariable String token) {
//        Users users = userService.findByResetPasswordToken(token);
//        if (users == null) {
//            return new ResponseEntity<>(HttpStatus.CONFLICT);
//        }
//        userService.updatePassword(theUser.getPassword(), token);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }

}
