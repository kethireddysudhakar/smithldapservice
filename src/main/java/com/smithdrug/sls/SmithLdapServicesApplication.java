package com.smithdrug.sls;

import static springfox.documentation.builders.PathSelectors.regex;

import java.util.ArrayList;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@EnableSwagger2
@ComponentScan
@SpringBootApplication
@EnableDiscoveryClient
@EnableAutoConfiguration(exclude = {JndiConnectionFactoryAutoConfiguration.class,DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,JpaRepositoriesAutoConfiguration.class,DataSourceTransactionManagerAutoConfiguration.class})
@PropertySource({"classpath:sls.properties","classpath:sls_${spring.profiles.active}.properties"})
public class SmithLdapServicesApplication extends SpringBootServletInitializer{
	
	@Value("${sls.portal.username}")
	private String getPortalUsername;
	
	@Value("${sls.portal.password}")
	private String getPortalPassword;
	
	@Value("${swagger.server.host}")
	private String swaggerHost;
	
	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate(getClientHttpRequestFactory());
	}
	
	@Bean("eurekaRestTemplate")
	@LoadBalanced
	RestTemplate restTemplates() {
		return new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication.run(SmithLdapServicesApplication.class, args);
	}
	
	@Bean
    public Docket productApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.smithdrug.sls"))
                .paths(regex("/slsServices.*"))
                .build().host(swaggerHost)
                .apiInfo(metaData());
    }
	private ApiInfo metaData() {
        ApiInfo apiInfo = new ApiInfo(
                "Smith LDAP services REST API",
                "REST API for Smith LDAP Web Service",
                "1.0",
                "Terms of service",
                new Contact("Alex Jeyasingh", "https://github.com/neethualex", "alexjeyasingh@gmail.com"),
               "Apache License Version 2.0",
                "https://www.apache.org/licenses/LICENSE-2.0",new ArrayList<VendorExtension>());
        return apiInfo;
    }
    
    private HttpComponentsClientHttpRequestFactory getClientHttpRequestFactory()
    {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory
                          = new HttpComponentsClientHttpRequestFactory();
         
        clientHttpRequestFactory.setHttpClient(httpClient());
              
        return clientHttpRequestFactory;
    }
    
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SmithLdapServicesApplication.class);
    }

     
    private HttpClient httpClient()
    {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
 
        credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(getPortalUsername, getPortalPassword));
 
        HttpClient client = HttpClientBuilder
                                .create()
                                .setDefaultCredentialsProvider(credentialsProvider)
                                .build();
        return client;
    }


}
