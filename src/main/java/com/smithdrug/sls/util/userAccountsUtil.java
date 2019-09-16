package com.smithdrug.sls.util;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class userAccountsUtil {
	
	private static final String DATE_FORMAT = "yyyy-MM-dd-HH.mm.ss";
    private static final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
	
	private static String emailHost = "email.smithdrug.com";
	private static String emailPort = "587";
	private static String emailAddr = "alerts@smithdrug.com";
	private static String emailUser = "smithdrug\\alerts";
	private static String emailPwd = "P0rtal8";
	private static String smtpHost = "mail.smtp.host";
	private static String smtpPort = "mail.smtp.port";
	private static String smtpfrom = "mail.smtp.from";
	private static String smtpauth = "mail.smtp.auth";
	private static String starttls = "mail.smtp.starttls.enable";
	private static String sslTrust = "mail.smtp.ssl.trust";
	private static String timeOut = "mail.smtp.connectiontimeout";
	private static String uft8 = "UTF-8";
	private static String charset = "text/html;charset=utf-8";

	// For credit card receipts.
	public static String receiptSDC = "receipt@smithdrug.com";
	public static String receiptIRX = "receipt@integral-rx.com";
	private static String receiptSDCUser = "smithdrug\\receipt";
	private static String receiptIRXUser = "IRXreceipt";
	private static String receiptPWD = "V3Rp.Hh;>x";

	public static boolean sendAlertEmail(String sendTo,String message) {
		boolean alertResponse = false;
		Session session = null;
		String subject = "Error in Trxade item update";
		try {
			
			message = "Error in Trxade item update, please check slack or logs<br>"+message;
			// Set properties with mail server information and sender.
			Properties props = new Properties();
			props.put(smtpfrom, emailAddr);
			props.put(smtpHost, emailHost);
			props.put(smtpPort, emailPort);
			props.put(starttls, "true");
			props.put(smtpauth, "true");
			// props.put("mail.debug", "true");

				session = Session.getInstance(props, new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(emailUser, emailPwd);
					}
				});
		} catch (Exception e) {

		}

		// Create the email message.
		try {
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(emailAddr));
			String[] emails = { "anesiyan@smithdrug.com","mbower@smithdrug.com" };
			InternetAddress dests[] = new InternetAddress[emails.length];
			for (int i = 0; i < emails.length; i++) {
				dests[i] = new InternetAddress(emails[i].trim().toLowerCase());
			}
			msg.setRecipients(Message.RecipientType.TO, dests);
			msg.setSubject(subject, uft8);
			msg.setSentDate(new Date());
			msg.setSubject(subject, uft8);
			Multipart mp = new MimeMultipart();
			MimeBodyPart mbp = new MimeBodyPart();
				/*message = message.replaceAll("&lt;", "<");
				message = message.replaceAll("&gt;", ">");*/
			mbp.setContent(message, charset);
			mp.addBodyPart(mbp);
			//if (sender.equals(receiptIRX)) {
				mbp = new MimeBodyPart();
				DataSource fds = new FileDataSource(
						"/opt/IBM/images/IntegralRx_logo.png");
				mbp.setDataHandler(new DataHandler(fds));
				mbp.setHeader("Content-ID", "<image>");
				mp.addBodyPart(mbp);
			//}
			msg.setContent(mp);
			Transport.send(msg);
		} catch (Exception e) {
		}
		return alertResponse;
	}
	
	public static String getCurrentDateTime()
	{
		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
		Date date = new Date();
		String returnDate = dateFormat.format(date);
		return returnDate;
	}
	
	public static String getNextDateTime(String date)
	{
		// Get current date
        Date currentDate = null;
		try {
			currentDate = new SimpleDateFormat(DATE_FORMAT).parse(date);
		} catch (ParseException e) {
			System.out.println("fpsUtil.getNextDateTime() Date Error ");
			e.printStackTrace();
		}

        // convert date to localdatetime
        LocalDateTime localDateTime = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        localDateTime = localDateTime.plusDays(1);
        // plus one
        // convert LocalDateTime to date
        Date currentDatePlusOneDay = currentDate.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());

        return dateFormat.format(currentDatePlusOneDay);
	}
	
	public static boolean isDateExpired(Timestamp checkDate)
	{
		if(!checkDate.before(new Timestamp(System.currentTimeMillis())))
		{
			return true;
		}
		return false;
	}

}
