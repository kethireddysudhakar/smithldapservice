package com.smithdrug.sls.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Repository;

import com.smithdrug.sls.repository.slsRepository;
import com.smithdrug.sls.util.slsUtil;
import com.smithdrug.sls.util.userAccountsUtil;

@Repository
public class slsRepositoryImpl implements slsRepository {

	@Autowired
	@Qualifier("jdbcTemplatedb2")
	private JdbcTemplate jdbcDB2Template;
	
	@Autowired
	@Qualifier("jdbcTemplateas400")
	private JdbcTemplate jdbcAS400Template;
	
	@Value("${sql.fps.getUserInfo}")
	private String getUserInfoSql;
	
	@Value("${sql.fps.insertResetRequest}")
	private String insertFPSRequestSQL;
	
	@Value("${sql.fps.updateResetRequest}")
	private String updateFPSRequestSQL;
	
	@Value("${sql.fps.updateExpiryRequest}")
	private String updateExpiryRequestSQL;
	
	@Value("${sql.fps.deleteResetRequest}")
	private String deleteFPSRequestSQL;
	
	@Autowired
    private LdapTemplate ldapTemplate;
	
	@Value("${sql.fps.validateResetToken}")
	private String validateResetSQL;
	
	@Override
	public void sendPasswordResetLink(String accountNumber) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public UUID resetUserPassword(String accountNumber,String email,String org) {
		
		UUID uuid = UUID.randomUUID();
		if(null == email || "".equals(email))
		{
			return null;
		}
		String currentDateTime = userAccountsUtil.getCurrentDateTime();
		String currentDateTimePlusOne=userAccountsUtil.getNextDateTime(currentDateTime);
		Object[] params = new Object[] {accountNumber,currentDateTime,currentDateTime,currentDateTimePlusOne,"NEW",uuid.toString(),0, email,org };
		int response = jdbcDB2Template.update(insertFPSRequestSQL,params);
		if(response==1)
		{
			return uuid;
		}
		else
		{
			return null;
		}
	}
	
	@Override
	public boolean validatePasswordResetToken(String accountNumber, String resetToken,String org) {
		Object[] params = new Object[] { accountNumber,resetToken,org,"E"};
		boolean isValidToken =false;
		try {
			isValidToken = jdbcDB2Template.query(validateResetSQL, params, new RowMapper<Boolean>() {
				@Override
				public Boolean mapRow(ResultSet result, int rownumber) throws SQLException {
					Timestamp  expirtyDate = result.getTimestamp("EDATE");
					boolean isValidResetLink = userAccountsUtil.isDateExpired(expirtyDate);
					return isValidResetLink;
				}

			}).get(0);
		}
		catch(Exception ex)
		{
			System.out.println("slsRepositoryImpl.validatePasswordResetToken() invalidToken "+resetToken);
		}
		
		return isValidToken;
	}
	@Override
	public boolean markPasswordTokenExpired(String accountNumber, String fpsToken) {
		String currentDateTime = userAccountsUtil.getCurrentDateTime();
		Object[] params = new Object[] {currentDateTime,"E",Integer.parseInt(accountNumber),fpsToken };
		int response = jdbcDB2Template.update(updateExpiryRequestSQL,params);
		if(response==1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean changeUserPasswordLDAP(String accountNumber, String password) {
		// TODO Auto-generated method stub
		return false;
	}

}
