package ru.yandex.practicum.cash.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.cash.client.dto.NotificationRequest;

@FeignClient(name = "notifications", url = "${app.notifications-url}")
public interface NotificationsClient {

    @PostMapping("/api/notifications")
    void send(@RequestBody NotificationRequest request);
}