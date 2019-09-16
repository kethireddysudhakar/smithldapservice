package com.smithdrug.sls.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class userAccountServiceDBConfig {
	@Bean(name = "db2")
	 @ConfigurationProperties(prefix = "spring.db2datasource")
	 public DataSource dataSource1() {
	  return DataSourceBuilder.create().build();
	 }

	@Primary
	 @Bean(name = "jdbcTemplatedb2")
	 public JdbcTemplate jdbcTemplate1(@Qualifier("db2") DataSource db2DS) {
	  return new JdbcTemplate(db2DS);
	 }
	 
	 @Bean(name = "as400")
	 @ConfigurationProperties(prefix = "spring.as400")
	 public DataSource dataSource2() {
	  return  DataSourceBuilder.create().build();
	 }

	 @Bean(name = "jdbcTemplateas400")
	 public JdbcTemplate jdbcTemplate2(@Qualifier("as400") DataSource as400Ds) {
	  return new JdbcTemplate(as400Ds);
	 }
}
