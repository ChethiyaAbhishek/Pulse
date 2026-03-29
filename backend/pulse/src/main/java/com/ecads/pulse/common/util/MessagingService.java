package com.ecads.pulse.common.util;

import com.ecads.pulse.authentication.entity.User;
import com.ecads.pulse.authentication.repository.UserRepository;
import com.ecads.pulse.common.exception.ShowErrorException;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingService {

    private final UserRepository userRepository;

    public Boolean sendPushNotification(String patientUid, String title, String body) {

        if (patientUid == null || patientUid.isEmpty()) {
            return false;
        }

        User user = userRepository.getByUid(patientUid);

        if (user != null) {

            if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                String patientToken = user.getFcmToken();

                Message message = Message.builder()
                        .setToken(patientToken)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
                        .build();

                try {
                    String response = FirebaseMessaging.getInstance().send(message);
                    log.info("Successfully sent message: {}", response);
                    return true;

                } catch (FirebaseMessagingException e) {
                    log.error("Error sending FCM message: {}", e.getMessage());
                    return false;
                }
            } else {
                log.error("No FCM token found for the user: {}", patientUid);
                throw new ShowErrorException("No FCM token found");
            }
        } else {
            log.error("No user found");
            return false;
        }
    }

}
