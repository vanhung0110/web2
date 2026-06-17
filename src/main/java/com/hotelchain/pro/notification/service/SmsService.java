package com.hotelchain.pro.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * SMS Service — hỗ trợ Esms.vn, Twilio, Viettel SMS.
 */
@Slf4j
@Service
public class SmsService {

    @Value("${notification.sms.provider:ESMS}")
    private String provider;

    @Value("${notification.sms.api-key:}")
    private String apiKey;

    @Value("${notification.sms.brand-name:HotelChain}")
    private String brandName;

    @Async
    public void send(String phoneNumber, String message) {
        try {
            switch (provider.toUpperCase()) {
                case "ESMS" -> sendViaEsms(phoneNumber, message);
                case "TWILIO" -> sendViaTwilio(phoneNumber, message);
                default -> log.warn("Unknown SMS provider: {}", provider);
            }
        } catch (Exception e) {
            log.error("SMS send failed to {}: {}", phoneNumber, e.getMessage());
        }
    }

    private void sendViaEsms(String phone, String message) {
        // Esms.vn API integration
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_get_json";

        String requestUrl = url + "?ApiKey=" + apiKey
                + "&Content=" + message
                + "&Phone=" + phone
                + "&Brandname=" + brandName
                + "&SmsType=2";

        try {
            String response = restTemplate.getForObject(requestUrl, String.class);
            log.info("Esms response for {}: {}", phone, response);
        } catch (Exception e) {
            log.error("Esms API error: {}", e.getMessage());
        }
    }

    private void sendViaTwilio(String phone, String message) {
        // Twilio SMS — sử dụng Twilio SDK
        log.info("[Twilio] SMS to {}: {}", phone, message);
        // com.twilio.Twilio.init(accountSid, authToken);
        // com.twilio.rest.api.v2010.account.Message.creator(
        //     new PhoneNumber(phone), new PhoneNumber(twilioPhone), message).create();
    }
}
