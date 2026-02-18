package com.strataguard.service.notification;

import com.strataguard.core.entity.Notification;
import com.strataguard.core.enums.NotificationChannel;

public interface NotificationSender {

    void send(Notification notification);

    NotificationChannel getChannel();
}
