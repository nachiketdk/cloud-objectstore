//package com.example.loadbalancer.controller;
//
//import com.example.loadbalancer.service.AutheticationService;
//import com.example.loadbalancer.service.User;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Optional;
//
//
//@RestController
//@RequestMapping("/user")
//public class AuthenticationController {
//    @Autowired
//    private AutheticationService autheticationService;
//    @PostMapping("/signup")
//    public ResponseEntity<?> signupUser(@RequestParam String email, @RequestParam String companyName, String password){
//
//        boolean success = autheticationService.signup(email,companyName,password);
//        if(!success){
//            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//        }
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//    @GetMapping("/login")
//    public ResponseEntity<?> loginUser(@RequestParam String email,@RequestParam String password){
//
//        boolean success = autheticationService.login(email,password);
//        if(!success){
//            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//        }
//        return new ResponseEntity<>(HttpStatus.OK);    }
//
//    @PostMapping("/delete")
//    public ResponseEntity<?> loginUser(@RequestParam String email){
//        boolean success = autheticationService.delete(email);
//        if(!success){
//            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//        }
//        return new ResponseEntity<>(HttpStatus.OK);    }
//    @GetMapping("/ping")
//    public ResponseEntity<?> hello(){
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
//
//}
