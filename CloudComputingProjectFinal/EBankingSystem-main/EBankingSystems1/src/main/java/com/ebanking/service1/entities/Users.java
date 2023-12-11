package com.ebanking.service1.entities;

import javax.persistence.*;
import javax.persistence.Entity;

import lombok.Data;

import java.math.BigInteger;
import java.sql.Date;

@Entity
@Data
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public int id;



    @Column(name = "username")
    public String username;

    @Column(name = "accountno")
    public BigInteger accountNo;

    @Column(name = "email")
    public String email;

    @Column(name = "password")
    public String password;

    @Enumerated(EnumType.STRING)
    @Column(name="role")
    public Role role;

    @Column(name = "status")
    public int status;

    @Column(name = "createdate")
    public Date createdDate;

    @Column(name = "resetpasswordtoken")
    public String resetPasswordToken;

    @Column(name = "otp")
    public String otp;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "userdetails")
    public BankAccount userDetails;

    @Transient
    public String token;

}
