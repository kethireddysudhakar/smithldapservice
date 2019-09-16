package com.smithdrug.sls.repository;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface slsRepository {
	
	public UUID resetUserPassword(String accountNumber,String email,String org);
	public boolean changeUserPasswordLDAP(String accountNumber,String password);
	public boolean validatePasswordResetToken(String accountNumber,String uniqueId,String org);
	public boolean markPasswordTokenExpired(String accountNumber, String tokenID);
	public void sendPasswordResetLink(String accountNumber);

}
