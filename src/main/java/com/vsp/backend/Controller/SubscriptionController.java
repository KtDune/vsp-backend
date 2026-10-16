package com.vsp.backend.Controller;

import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vsp.backend.Model.Subscription;
import com.vsp.backend.Model.User;
import com.vsp.backend.Service.SubscriptionService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/users/{id}/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@PathVariable Long id,
            @RequestParam Long subscriberId) {
        Map<String, Object> result = subscriptionService.subscribe(subscriberId, id);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @DeleteMapping("/users/{id}/subscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@PathVariable Long id,
            @RequestParam Long subscriberId) {
        if (subscriptionService.unsubscribe(subscriberId, id)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Unsubscribed"));
        }

        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Subscription not found"));
    }

    @GetMapping("/users/{id}/subscription-requests")
    public ResponseEntity<?> getChannelSubscriptionRequests(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);

        return ResponseEntity.ok(Map.of("success", true,
                "requests", subscriptionService.getChannelSubscriptionRequests(id, page, pageSize)));
    }

    @PostMapping("/subscriptions/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id,
            @RequestParam Long requesterId) {
        Map<String, Object> result = subscriptionService.approve(id, requesterId);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/subscriptions/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id,
            @RequestParam Long requesterId) {
        Map<String, Object> result = subscriptionService.reject(id, requesterId);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Map<String, Object>> removeSubscriber(@PathVariable Long id,
            @RequestParam Long requesterId) {
        Map<String, Object> result = subscriptionService.removeSubscriber(id, requesterId);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/subscriptions/mine")
    public ResponseEntity<?> getMySubscriptions(@RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        Map<String, Object> result = subscriptionService.getMySubscriptions(userId, page, pageSize);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(404).body(result);
    }

    @GetMapping("/users/{id}/subscribers")
    public ResponseEntity<?> getChannelSubscribers(@PathVariable Long id,
            @RequestParam(required = false) Long viewerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);

        Slice<Subscription> subscribers = subscriptionService.getChannelSubscribers(id, viewerId, page, pageSize);

        if (subscribers == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found or deactivated"));
        }

        return ResponseEntity.ok(Map.of("success", true, "subscribers", subscribers));
    }

    @GetMapping("/users/{id}/subscriber-count")
    public ResponseEntity<?> getSubscriberCount(@PathVariable Long id, @RequestParam(required = false) Long viewerId) {
        Map<String, Object> result = subscriptionService.getSubscriberCount(id, viewerId);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(404).body(result);
    }

    @GetMapping("/users/{id}/subscription-status")
    public ResponseEntity<?> getMySubscriptionStatus(@PathVariable Long id,
            @RequestParam(required = false) Long viewerId) {
        Map<String, Object> result = subscriptionService.getMySubscriptionStatus(id, viewerId);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(404).body(result);
    }
}
