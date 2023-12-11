//package com.example.loadbalancer.repostory;
//
//import com.example.loadbalancer.entity.User;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//import java.util.List;
//import javax.transaction.Transactional;
//import org.springframework.data.jpa.repository.Modifying;
//
//@Repository
//public interface Auth extends JpaRepository<User, Long> {
//
////    public Users findByEmail(String email);
//    @Transactional
//    @Query(value = "select email from admin where email= :email and password= :password LIMIT 1", nativeQuery = true)
//    public Object findUser(String email,String password);
//
//    @Transactional
//    @Modifying
//    @Query(value = "DELETE FROM admin WHERE email= :email", nativeQuery = true)
//    void deleteUser(String email);
//    @Transactional
//    @Modifying
//    @Query(value = "INSERT INTO admin (email,password,comapnyname) VALUES (:email,:password,:companyName)", nativeQuery = true)
//    void addNewUser(String email,String password,String companyName);
//
//}
