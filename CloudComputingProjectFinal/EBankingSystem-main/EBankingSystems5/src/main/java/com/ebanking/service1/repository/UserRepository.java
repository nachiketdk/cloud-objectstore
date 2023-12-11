package com.ebanking.service1.repository;

import java.util.List;

import com.ebanking.service1.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;



@Repository
public interface UserRepository extends JpaRepository<Users, Long>{
    public Users findByEmail(String email);
    @Transactional
    @Query(value = "select * from users where role = 'USER' and status = '1'", nativeQuery = true)
    public List<Object[]> findTotalUsers();

//    @Query(value = "select users.*, bankaccount.name from users inner join bankaccount on " +
//            "users.userdetails = bankaccount.id where bankaccount.accountno = :accountNo", nativeQuery = true)
//    public Users findByAccountNo(@RequestParam("accountNo") BigInteger accountNo);

    public Users findByResetPasswordToken(String token);

    public Users findByotp(String otp);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM users WHERE email= :email", nativeQuery = true)
    void deleteUser(String email);
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO users (id,role,status) VALUES (:id,'USER','1')", nativeQuery = true)
    void addNewUser(String id);

}
