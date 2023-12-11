//package com.example.loadbalancer.service;
//
//import com.example.loadbalancer.repostory.Auth;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.Arrays;
//
//@Service
//public class AutheticationService {
//
//    private Auth auth;
//    public boolean signup(String email, String companyName,String password){
//
//        try{
//            auth.addNewUser(email,password,companyName);
//        }
//        catch (Exception e){
//            System.out.println(e.getMessage());
//            System.out.println(Arrays.toString(e.getStackTrace()));
//            return false;
//        }
//        return true;
//    }
//    public boolean login(String email, String password){
//
//        try{
//            auth.findUser(email,password);
//        }
//        catch (Exception e){
//            System.out.println(Arrays.toString(e.getStackTrace()));
//            return false;
//        }
//        return true;
//    }
//    public boolean delete(String email){
//        try{
//            auth.deleteUser(email);
//        }
//        catch (Exception e){
//            System.out.println(Arrays.toString(e.getStackTrace()));
//            return false;
//        }
//        return true;
//    }
//}
