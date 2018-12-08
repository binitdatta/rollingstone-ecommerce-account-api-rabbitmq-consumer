package com.rollingstone.rabbitmq.consumer;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.rollingstone.config.ApplicationConstant;
import com.rollingstone.model.AccountDTO;

@Service
public class AccountRabbitMQMessageConsumerListener {

	private static final Logger log = LoggerFactory.getLogger(AccountRabbitMQMessageConsumerListener.class);

	 @Autowired
	 private JavaMailSender sender;
	 
	 @Value("${spring.mail.username}")
	 String emailReceiver;
	 
	/**
	 * Message listener for account
	 * 
	 * @param AccountDTO a user defined object used for unmarshalling of message
	 */
	@RabbitListener(queues = "${account.queue.name}")
	public void receiveMessageForApp1(final AccountDTO accountDTO) {
		log.info("Received account dto message: {} from account queue.", accountDTO);

		try {
			log.info("Ta Think");
			// TODO: Code to send email to the Account Holder
			sendMail(accountDTO);
			log.info("<< Email Sent.");
		} catch (HttpClientErrorException ex) {

			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.info("Delay...");
				try {
					Thread.sleep(ApplicationConstant.MESSAGE_RETRY_DELAY);
				} catch (InterruptedException e) {
				}

				log.info("Throwing exception so that message will be requed in the queue.");
				// Note: Typically Application specific exception should be thrown below
				throw new RuntimeException();
			} else {
				throw new AmqpRejectAndDontRequeueException(ex);
			}

		} catch (Exception e) {
			log.error("Internal server error occurred in API call. Bypassing message requeue {}", e);
			throw new AmqpRejectAndDontRequeueException(e);
		}
	}
	
	 public String sendMail(AccountDTO accountDTO) {
	        MimeMessage message = sender.createMimeMessage();
	        MimeMessageHelper helper = new MimeMessageHelper(message);

	        try {
	            helper.setTo(emailReceiver);
	            helper.setText("Dear :"+accountDTO.getAccountName() + "Your Account has now been Created "
	            + "With the AccountNumber : "+ accountDTO.getAccountNumber() +" )");
	            helper.setSubject("Account Created!");
	        } catch (MessagingException e) {
	            e.printStackTrace();
	            return "Error while sending mail ..";
	        }
	        sender.send(message);
	        return "Mail Sent Success!";
	    }
}
