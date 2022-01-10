package net.hirana.context;

import net.hirana.utils.RedisService;
import net.hirana.utils.push.FCMService;
import net.hirana.utils.push.PushNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

public enum FCMContext {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(FCMContext.class);
    private HashMap<String, Date> lastNotificationSended = new HashMap<>();

    public void saveTokenForUser(String user, String token) {
        log.info(String.format("Setting new token for %s - %s", user, token));
        RedisService.INSTANCE.setValue(String.format("FCM-%s", user), token);
    }

    public void sendMessageToUser(String user, String title, String content) {
        String key = String.format("FCM-%s", user);
        Date now = new Date();
        if(lastNotificationSended.get(user).getTime() + 20000 > now.getTime()) { // pasaron 10 secs desde la ultima notificacion?
            return;
        }
        Optional<String> fcmToken = Optional.ofNullable(RedisService.INSTANCE.getValue(key));
        if(fcmToken.isPresent()) {
            PushNotificationRequest request = new PushNotificationRequest();
            request.setMessage(content);
            request.setToken(fcmToken.get());
            request.setTitle(title);
            log.debug(String.format("Sending notification of %s to %s", title, user));
            try {
                FCMService.INSTANCE.sendMessageToToken(request);
            } catch (Exception e) {
                log.error(String.format("Can't send notification %s", fcmToken), e);
            }
            lastNotificationSended.put(user, now);
        }
    }

}
