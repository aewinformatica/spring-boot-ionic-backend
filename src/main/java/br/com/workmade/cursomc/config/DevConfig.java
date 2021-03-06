package br.com.workmade.cursomc.config;

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import br.com.workmade.cursomc.service.DBService;
import br.com.workmade.cursomc.service.EmailService;
import br.com.workmade.cursomc.serviceImpl.SmtpEmailServiceImpl;

@Configuration
@Profile("dev")
public class DevConfig {
	
	private Logger LOGGER = LoggerFactory.getLogger(DevConfig.class);

	@Autowired
	private DBService dBService;
	
	@Value("${spring.jpa.hibernate.ddl-auto}")
	private String strategy;
	
	@Bean
	public boolean instantiateDataBase() throws ParseException {
		if(!strategy.equals("update")) {
			LOGGER.info("Modo "+strategy+" ativo");
			dBService.instantiateDataBase();
		}
		return false;
	}
	@Bean
	public EmailService emailService() { 
		return new SmtpEmailServiceImpl();
	}
}
