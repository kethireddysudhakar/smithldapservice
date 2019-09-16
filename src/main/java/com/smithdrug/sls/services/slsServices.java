package com.smithdrug.sls.services;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import com.smithdrug.sls.services.intf.slsServiceIntf;

@Service
public class slsServices implements slsServiceIntf {
	
	@Autowired
    private LdapTemplate ldapTemplate;
	
	private static final String BASE_DN = "dc=smithportal,dc=com";
	
	@Override
	public boolean changeUserPasswordLDAP(String accountNumber,String password) {
		try {
		LdapName dn = LdapNameBuilder.newInstance(BASE_DN)
	            .add("OU", "SmithSelect")
	            .add("CN", accountNumber).build();
	        DirContextAdapter context = new DirContextAdapter(dn);

	        context.setAttributeValues(
	           "objectclass", 
	           new String[] 
	           { "top", 
	             "person","organizationalPerson","user"
	             });
	        context.setAttributeValue("title", "Mr");
	        context.setAttributeValue("userPassword",password);
	        
			/*
			 * String quotedPassword = "\"" + password + "\""; char unicodePwd[] =
			 * quotedPassword.toCharArray(); byte pwdArray[] = new byte[unicodePwd.length *
			 * 2]; for (int i=0; i<unicodePwd.length; i++) { pwdArray[i*2 + 1] = (byte)
			 * (unicodePwd[i] >>> 8); pwdArray[i*2 + 0] = (byte) (unicodePwd[i] & 0xff); }
			 * ModificationItem[] mods = new ModificationItem[1]; mods[0] = new
			 * ModificationItem(DirContext.REPLACE_ATTRIBUTE, new
			 * BasicAttribute("UnicodePwd", pwdArray));
			 */
	        
	        ModificationItem[] mods = new ModificationItem[2];
	    	 
			//Replace the "unicdodePwd" attribute with a new value
			//Password must be both Unicode and a quoted string
			String newQuotedPassword = "\"" + password + "\"";
			
			//String newQuotedPassword =newPassword;
			byte[] newUnicodePassword = null;
				newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE");

			mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", newUnicodePassword));
			mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("pwdLastSet", "-1"));
	        
context.setUpdateMode(true);
	        ldapTemplate.modifyAttributes(dn, mods);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return false;
	}

}
