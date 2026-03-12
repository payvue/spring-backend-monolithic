package com.payvue.app.presentation.rest.alarm;

import com.payvue.app.application.alarm.AlarmActionService;
import com.payvue.app.application.alarm.AlarmQueryService;
import com.payvue.app.application.alarm.AlarmReadService;
import com.payvue.app.presentation.rest.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmController {

  private final AlarmQueryService alarmQueryService;
  private final AlarmReadService alarmReadService;
  private final AlarmActionService alarmActionService;

  @GetMapping("/received/{page}")
  public ApiResponse<AlarmNotificationList> received(
      @AuthenticationPrincipal UserDetails user,
      @PathVariable int page
  ) {
    return ApiResponse.success(alarmQueryService.fetchReceived(user.getUsername(), page));
  }

  @PutMapping("/{alarmId}/read")
  public ApiResponse<Void> markAsRead(
      @PathVariable String alarmId,
      @AuthenticationPrincipal UserDetails user
  ) {
    alarmReadService.markAsRead(alarmId, user.getUsername());
    return ApiResponse.success();
  }

  @PutMapping("/received/{alarmId}")
  public ApiResponse<Void> handleReceived(
      @PathVariable String alarmId,
      @RequestParam(name = "action") String action,
      @AuthenticationPrincipal UserDetails user
  ) {
    alarmActionService.handleInvitation(alarmId, action, user.getUsername());
    return ApiResponse.success();
  }
}
