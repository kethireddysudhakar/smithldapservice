package com.smithdrug.sls.helper;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.springframework.stereotype.Service;

@Service
public class LDAPHelper {

	//private String ldapHost = "ldap://portaldca.smithportal.com:636/";
	private String ldapHost = "ldap://portaldca.smithportal.com:636/";
	private String bindUser = "cn=bind4sdc,cn=users,dc=smithportal,dc=com";
	private String bindPassword = "B1nd45dc";
	private static String resetText = "Your password must be changed after reset";
	
	
	public boolean updateUserPassword(String userDN, String password) throws NamingException, UnsupportedEncodingException{
		changePassword(userDN,password,ldapHost,bindUser,bindPassword);
		return true;
	}
	
	private void changePassword(String userDN, String password , String ldapHost, String adminUser, String adminPwd) throws NamingException, UnsupportedEncodingException {
		LdapContext ctx1 = null;
			// Create the initial directory context
			ctx1 = connectLDAP(adminUser,adminPwd,true, ldapHost);
			//change the password
			changePasswordLdap(ctx1, userDN, password);
	}
	
	private void changePasswordLdap(DirContext ctx1, String userDN, String newPassword) throws NamingException, UnsupportedEncodingException {
			//set password is a ldap modfy operation
			ModificationItem[] mods = new ModificationItem[2];
	
			//Replace the "unicdodePwd" attribute with a new value
			//Password must be both Unicode and a quoted string
			String newQuotedPassword = "\"" + newPassword + "\"";
			
			//String newQuotedPassword =newPassword;
			byte[] newUnicodePassword = null;
				newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE");

			mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", newUnicodePassword));
			mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("pwdLastSet", "-1"));
			// Perform the update
				ctx1.modifyAttributes(userDN, mods);
	}
	
	private LdapContext connectLDAP(String user, String password, boolean secureConnection, String ldapHost) throws NamingException {
		 LdapContext ctx = null;
		// -----------------------------------------------
		// Set up the environment for creating the initial
		// context.
		// -----------------------------------------------
			Properties props = new Properties();
			props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		
			props.setProperty(Context.PROVIDER_URL,ldapHost);
			props.setProperty(Context.URL_PKG_PREFIXES, "com.sun.jndi.url");
			props.setProperty(Context.REFERRAL, "ignore");
			//props.setProperty(Context.REFERRAL, "follow");
			
			//ADDED FOR SSL - not needed - import certs into portal admin console
			props.setProperty(Context.SECURITY_PROTOCOL, "ssl");
			props.setProperty("java.naming.ldap.deleteRDN", "true");
			if (secureConnection) {
				props.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
				
				// --------------------------------------------------
				// specify the root username
				// --------------------------------------------------
				// props.setProperty(Context.SECURITY_PRINCIPAL,
				System.out.println("LDAPHelper.connectLDAP( ) user "+user);
				props.setProperty(Context.SECURITY_PRINCIPAL, user);
				// --------------------------------------------------
				// specify the root password
				// --------------------------------------------------
				props.setProperty(Context.SECURITY_CREDENTIALS, password);
				// --------------------------------------------------

			}
			// search does not need root user id and password.
			else {
				props.setProperty(Context.SECURITY_AUTHENTICATION, "simple");

			}
			// -----------------------------------------------
			// Get the environment properties (props) for
			// creating initial context and specifying LDAP
			// service provider parameters.
			// -----------------------------------------------
			props.setProperty("java.naming.ldap.attributes.binary","objectGUID");
				ctx = new InitialLdapContext(props,null);
		return ctx;
	}
	
	
}

