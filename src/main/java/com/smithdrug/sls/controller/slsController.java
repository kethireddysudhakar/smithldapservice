package com.smithdrug.sls.controller;

import java.util.Arrays;
import java.util.UUID;

import javax.naming.ldap.LdapName;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smithdrug.response.WebServiceResponse;
import com.smithdrug.response.accounts.ForgotPasswordResponseData;
import com.smithdrug.response.accounts.ResponseData;
import com.smithdrug.response.accounts.SmithResponseData;
import com.smithdrug.response.codes.ResponseCodes;
import com.smithdrug.response.error.ErrorData;
import com.smithdrug.response.error.ErrorResponseData;
import com.smithdrug.response.status.SmithStatus;
import com.smithdrug.response.status.SmithStatus.status;
import com.smithdrug.sls.helper.LDAPHelper;
import com.smithdrug.sls.repository.slsRepository;
import com.smithdrug.sls.services.slsServices;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/slsServices")
@Api(value="SLSStore", description="Operations pertaining to Smith LDAP Web Services")
@Validated
/* @RefreshScope */
public class slsController {
	private static final String BASE_DN = "dc=smithportal,dc=com";
	
	@Autowired 
	private slsRepository repository;
	
	@Autowired
	private LDAPHelper ldapHelper;
	@Autowired
	private slsServices slsService;
	
	@Value("${sls.portal.changePasswordURL}")
	private String changePasswordURL;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	@Qualifier("eurekaRestTemplate")
	private RestTemplate eurekaRestTemplate;
	
	private SmithStatus smithStatus = null;
	private ResponseData data =null;
	private ErrorResponseData error =null;
	private WebServiceResponse response = null;
	private String org="egate";
	
	@ApiOperation(value = "Quick check if the service is up and running",response = Iterable.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully tested the service availability"),
            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
            @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
    }
    )
	@GetMapping(path = "/serviceCheck")
	public ResponseEntity<?> quickServiceChecker() {
		SmithStatus smithStatus = new SmithStatus(status.SUCCESS);
		SmithResponseData data = new SmithResponseData("Service up and running");
		ErrorResponseData error = new ErrorResponseData(null);
		WebServiceResponse response = new WebServiceResponse(smithStatus, data, error);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@ApiOperation(value = "Reset request for user password",response = Iterable.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved list"),
            @ApiResponse(code = 400, message = "Data validation failed"),
            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
            @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
    }
    )
	@GetMapping(path = "/v1/changeUserPassword/{accountNumber}/{password}/{passwordToken}/{org}")
	public ResponseEntity<?> changeUserPassword(@PathVariable @NotNull String accountNumber,@PathVariable @NotNull String password,@PathVariable @NotNull String passwordToken,@PathVariable @NotNull String org)
	{
		boolean isValidFPSToken = repository.validatePasswordResetToken(accountNumber, passwordToken,org);
		if(!isValidFPSToken)
		{
			smithStatus = new SmithStatus(status.FAILURE);
			data = new ForgotPasswordResponseData(false, false, true, false,true,null);
			ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.invalidPasswordResettoken);
			error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
			response = new WebServiceResponse(smithStatus, data, error);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		LdapName dn = LdapNameBuilder.newInstance(BASE_DN)
	            .add("OU", "SmithSelect")
	            .add("CN", accountNumber).build();
				boolean isPwdUpdated = updateUserPasswordInPortal(accountNumber, password);
				if(isPwdUpdated)
				{
					String emailResponseString = eurekaRestTemplate.getForObject("http://USERACCOUNT-SERVICE/userAccountServices/v1/getUserInfo/"+accountNumber, String.class);
					JsonObject jsonObject = new JsonParser().parse(emailResponseString).getAsJsonObject();
					int responseCode = jsonObject.get("status").getAsJsonObject().get("code").getAsInt();
					String email = jsonObject.get("data").getAsJsonObject().get("emailAddress").getAsString();// ((UserInformationData)getEmailResponse.getData()).getEmailAddress();
					String emailResponse = eurekaRestTemplate.getForObject("http://MESSAGING-SERVICE/smsServices/v1/sendFPSSuccessEmail/"+email+"/"+accountNumber+"/"+org, String.class);
					WebServiceResponse updatePasswordExpiryResponse = (WebServiceResponse)updatePasswordTokenExpiry(accountNumber,passwordToken).getBody();
					smithStatus = new SmithStatus(status.SUCCESS);
					data = new ForgotPasswordResponseData(false, true, false, false,false,null);
					error = null;
					response = new WebServiceResponse(smithStatus, data, error);
					return new ResponseEntity<>(response, HttpStatus.OK);
				}
				else
				{
						smithStatus = new SmithStatus(status.FAILURE);
						data = new ForgotPasswordResponseData(false, false, false, false,false,null);
						ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.invalidDataErrorCode);
						error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
						response = new WebServiceResponse(smithStatus, data, error);
						return new ResponseEntity<>(response, HttpStatus.OK);
						
				}
	}
	
	@ApiOperation(value = "Reset request for user password",response = Iterable.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved list"),
            @ApiResponse(code = 400, message = "Data validation failed"),
            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
            @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
    }
    )
	@GetMapping(path = "/v1/resetUserPassword/{accountNumber}/{orgId}")
	public ResponseEntity<?> resetUserPassword(@PathVariable @NotNull String accountNumber,@PathVariable @NotNull String orgId) throws JSONException
	{
		String emailResponseString = eurekaRestTemplate.getForObject("http://USERACCOUNT-SERVICE/userAccountServices/v1/getUserInfo/"+accountNumber, String.class);
		JsonObject jsonObject = new JsonParser().parse(emailResponseString).getAsJsonObject();
		int responseCode = jsonObject.get("status").getAsJsonObject().get("code").getAsInt();
		if(responseCode != 200)
		{
			smithStatus = new SmithStatus(status.FAILURE);
			data = new ForgotPasswordResponseData(false, false, false, false,false,null);
			ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.fpsNoEmail);
			error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
			response = new WebServiceResponse(smithStatus, data, error);
			return new ResponseEntity<>(response, HttpStatus.OK);
			
		}
		String email = jsonObject.get("data").getAsJsonObject().get("emailAddress").getAsString();// ((UserInformationData)getEmailResponse.getData()).getEmailAddress();
		String location = jsonObject.get("data").getAsJsonObject().get("location").getAsString();//((UserInformationData)getEmailResponse.getData()).getLocation();
		System.out.println("slsController.resetUserPassword() "+email+location);
		if(location.equals("53"))
		{
			org="integral";
		}
		else
		{
			org="egate";
		}
		if(!org.equalsIgnoreCase(orgId))
		{
			smithStatus = new SmithStatus(status.FAILURE);
			 data = null;
				ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.accountNumberNotFound);
				error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
				response = new WebServiceResponse(smithStatus, data, error);
			 return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
		System.out.println("fpscontroller.sendResetLinkn() org is  "+org);
		UUID responseUUID = null;
		try {
		responseUUID = repository.resetUserPassword(accountNumber,email,org);
		}
		catch(Exception ex)
		{
			smithStatus = new SmithStatus(status.FAILURE);
			 data = new ForgotPasswordResponseData(false, false, false, false,true,email);
				ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.accountNumberNotFound);
				error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
				response = new WebServiceResponse(smithStatus, data, error);
			 return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
		if(null!=responseUUID)
		{
			WebServiceResponse sendEmailResponse = (WebServiceResponse)sendPasswordResetLink(accountNumber, responseUUID.toString(), email,org).getBody();
			if(sendEmailResponse.getStatus().getCode() == 200)
			 {
				smithStatus = new SmithStatus(status.SUCCESS);
				data = new ForgotPasswordResponseData(false, false, false, true,true,email);
				error=null;
				response = new WebServiceResponse(smithStatus, data, error);
				 return new ResponseEntity<>(response, HttpStatus.OK);
			 }
			 else
			 {
				 smithStatus = new SmithStatus(status.FAILURE);
				 data = new ForgotPasswordResponseData(false, false, false, false,true,email);
					ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.fpsEmailNotSent);
					error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
					response = new WebServiceResponse(smithStatus, data, error);
				 return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
			 }
		}
		else
		{
			smithStatus = new SmithStatus(status.FAILURE);
			data = new ForgotPasswordResponseData(false, false, false, false,false,null);
			ErrorData errorMessages = new ErrorData(ResponseCodes.invalidRequestErrorCode, ResponseCodes.fpsInvalidEmail);
			error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
			response = new WebServiceResponse(smithStatus, data, error);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}
		
	}
	
	@ApiOperation(value = "Send email with password reset link",response = Iterable.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved list"),
            @ApiResponse(code = 400, message = "Data validation failed"),
            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
            @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
    }
    )
	@GetMapping(path = "/v1/sendPasswordResetLink/{accountNumber}/{passwordToken}/{email}/{org}")
	public ResponseEntity<?> sendPasswordResetLink(@PathVariable @NotNull String accountNumber,@PathVariable @NotNull String passwordToken,@PathVariable @NotNull String email,@PathVariable @NotNull String org)
	{
		 String emailResponse = eurekaRestTemplate.getForObject("http://MESSAGING-SERVICE/smsServices/v1/sendFPSEmail/"+email+"/"+passwordToken+"/"+accountNumber+"/"+org, String.class);
		 System.out.println("fpscontroller.sendResetLink()" +emailResponse);
		 if(emailResponse.equalsIgnoreCase("Email Sent Sucessfully"))
		 {
				smithStatus = new SmithStatus(status.SUCCESS);
				data = new ForgotPasswordResponseData(false, false, false, true,true,email);
				response = new WebServiceResponse(smithStatus, data, error);
				error = null;
				return new ResponseEntity<>(response, HttpStatus.OK);
		 }
		 else
		 {
		 	smithStatus = new SmithStatus(status.FAILURE);
			data = new ForgotPasswordResponseData(false, false, false, false,true,email);
			ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.fpsEmailNotSent);
			error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
			response = new WebServiceResponse(smithStatus, data, error);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		 }
	}
	
	@ApiOperation(value = "Validation for the password reset token",response = Iterable.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved list"),
            @ApiResponse(code = 400, message = "Data validation failed"),
            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
            @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
    }
    )
	@GetMapping(path = "/v1/validatePasswordResetToken/{accountNumber}/{tokenID}/{org}")
	public ResponseEntity<?> validatePasswordResetToken(@PathVariable @NotNull String accountNumber,@PathVariable @NotNull String tokenID,@PathVariable @NotNull String org)
	{
		boolean isTokenValid = false;
		try {
		isTokenValid = repository.validatePasswordResetToken(accountNumber,tokenID,org);
		}
		catch(Exception ex)
		{
			smithStatus = new SmithStatus(status.FAILURE);
			data = new ForgotPasswordResponseData(false, false, false, false,true,null);
			ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.invalidPasswordResettoken);
			error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
			response = new WebServiceResponse(smithStatus, data, error);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		if(isTokenValid)
		{
			smithStatus = new SmithStatus(status.SUCCESS);
			data = new ForgotPasswordResponseData(true, false, false, false,true,null);
			error = null;
			response = new WebServiceResponse(smithStatus, data, error);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		smithStatus = new SmithStatus(status.FAILURE);
		data = new ForgotPasswordResponseData(false, false, true, false,true,null);
		ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.invalidPasswordResettoken);
		error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
		response = new WebServiceResponse(smithStatus, data, error);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@ApiOperation(value = "Update database about the user click",response = Iterable.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved list"),
            @ApiResponse(code = 400, message = "Data validation failed"),
            @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
            @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
            @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
    }
    )
	@GetMapping(path = "/v1/updatePasswordResetClick/{accountNumber}/{tokenID}")
	public ResponseEntity<?> updateFPSClick(@PathVariable @NotNull String accountNumber,@PathVariable @NotNull String tokenID)
	{
		boolean updateResponse = false;//repository.resetUserPassword(accountNumber,fpsID);
		if(updateResponse)
		{
			smithStatus = new SmithStatus(status.SUCCESS);
			data = new ForgotPasswordResponseData(true, false, false, false,true,null);
			error = null;
			response = new WebServiceResponse(smithStatus, data, error);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		smithStatus = new SmithStatus(status.FAILURE);
		data = new ForgotPasswordResponseData(false, false, true, false,true,null);
		ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.invalidPasswordResettoken);
		error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
		response = new WebServiceResponse(smithStatus, data, error);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	
	
	@GetMapping(path = "/v1/updatePasswordTokenExpiry/{accountNumber}/{tokenID}")
	public ResponseEntity<?> updatePasswordTokenExpiry(@PathVariable @NotNull String accountNumber,
			@PathVariable @NotNull String tokenID) {
		boolean updateResponse = repository.markPasswordTokenExpired(accountNumber, tokenID);
		if(updateResponse)
		{
			smithStatus = new SmithStatus(status.SUCCESS);
			data = new ForgotPasswordResponseData(true, false, true, false,true,null);
			error = null;
			response = new WebServiceResponse(smithStatus, data, error);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		smithStatus = new SmithStatus(status.FAILURE);
		data = new ForgotPasswordResponseData(false, false, true, false,true,null);
		ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.invalidPasswordResettoken);
		error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
		response = new WebServiceResponse(smithStatus, data, error);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	 
	
	@ExceptionHandler(ConstraintViolationException.class)
	  @ResponseStatus(HttpStatus.BAD_REQUEST)
	  ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException e) {
		smithStatus = new SmithStatus(status.FAILURE);
		data = new SmithResponseData("Mandatory parameters missing");
		ErrorData errorMessages = new ErrorData(ResponseCodes.failure, ResponseCodes.mandatoryFieldsMissing);
		error = new ErrorResponseData<ErrorData>(Arrays.asList(errorMessages));
		response = new WebServiceResponse(smithStatus, data, error);
	    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	  }
	
	private boolean updateUserPasswordInPortal(String accountNumber,String password)
	{
		try {
		 Object obj = restTemplate.getForObject(changePasswordURL+"UserID="+accountNumber+"&NewPassword="+password+"&forgotPassword=yes", Object.class);
		 obj.toString();
		  if(obj.toString().contains("Success"))
		  {
			  return true;
		  }
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		  return false;
	}
	
	

}
