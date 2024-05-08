package com.braintrain.backend.service;

import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OtpService {
    @Value("${twilio.account_sid}")
    private String accountSid;

    @Value("${twilio.auth_token}")
    private String authToken;

    @Value("${twilio.trial_number}")
    private String trialNumber;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
    }

    @Async
    public void sendOtp(String recipientPhone, String body) {
        try {
            Message message = Message
                    .creator(new PhoneNumber(recipientPhone),
                            new PhoneNumber(trialNumber),
                            body)
                    .create();
            log.info("OTP sent");
        } catch (TwilioException e) {
            throw new IllegalStateException("Exception occurred when sending otp to " + recipientPhone + ": " + e);
        }
    }
}
