package com.vsp.backend.Controller;

import com.vsp.backend.Dto.SubscriptionResponse;
import com.vsp.backend.Security.AppUserPrincipal;
import com.vsp.backend.Service.SubscriptionService;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @AuthenticationPrincipal AppUserPrincipal me) {
        Map<String, Object> result = subscriptionService.subscribe(me.id(), id);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @DeleteMapping("/users/{id}/subscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        if (subscriptionService.unsubscribe(me.id(), id)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Unsubscribed"));
        }

        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Subscription not found"));
    }

    @GetMapping("/users/{id}/subscription-requests")
    public ResponseEntity<?> getChannelSubscriptionRequests(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (me == null || !me.id().equals(id)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Owner only"));
        }

        int pageSize = Math.min(Math.max(size, 1), 100);

        return ResponseEntity.ok(Map.of("success", true,
                "requests", subscriptionService.getChannelSubscriptionRequests(id, page, pageSize)));
    }

    @PostMapping("/subscriptions/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Map<String, Object> result = subscriptionService.approve(id, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/subscriptions/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Map<String, Object> result = subscriptionService.reject(id, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Map<String, Object>> removeSubscriber(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Map<String, Object> result = subscriptionService.removeSubscriber(id, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/subscriptions/mine")
    public ResponseEntity<?> getMySubscriptions(@AuthenticationPrincipal AppUserPrincipal me,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        Map<String, Object> result = subscriptionService.getMySubscriptions(me.id(), page, pageSize);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(404).body(result);
    }

    @GetMapping("/users/{id}/subscribers")
    public ResponseEntity<?> getChannelSubscribers(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        Long viewerId = me != null ? me.id() : null;

        Slice<SubscriptionResponse> subscribers = subscriptionService.getChannelSubscribers(id, viewerId, page,
                pageSize);

        if (subscribers == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found or deactivated"));
        }

        return ResponseEntity.ok(Map.of("success", true, "subscribers", subscribers));
    }

    @GetMapping("/users/{id}/subscriber-count")
    public ResponseEntity<?> getSubscriberCount(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Long viewerId = me != null ? me.id() : null;
        Map<String, Object> result = subscriptionService.getSubscriberCount(id, viewerId);

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(404).body(result);
    }

    @GetMapping("/users/{id}/subscription-status")
    public ResponseEntity<?> getMySubscriptionStatus(@PathVariable Long id,
            @AuthenticationPrincipal AppUserPrincipal me) {
        Map<String, Object> result = subscriptionService.getMySubscriptionStatus(id, me.id());

        return result.get("success").equals(true)
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(404).body(result);
    }
}
