package com.payvue.app.application.account.setting;

import com.payvue.app.domain.account.CommandType;
import com.payvue.app.domain.account.UserPaymentLimitSetting;
import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.transaction.command.AssetType;
import com.payvue.core.transaction.command.TransactionType;
import com.payvue.core.transaction.query.TransactionHistoryPage;
import com.payvue.core.transaction.query.TransactionHistoryView;
import com.payvue.core.transaction.query.TransactionQueryRepository;
import com.payvue.shared.Period;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentLimitSettingApplicationService implements PaymentLimitSettingUseCase {

  private final UserContextManager userContextManager;
  private final CommunityContextManager communityContextManager;
  private final TransactionQueryRepository transactionQueryRepository;

  @Override
  @Transactional(readOnly = true)
  public PaymentLimitSettingResult get(UserDetails userDetails) {
    User user = userContextManager.findByEmail(userDetails.getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("등록된 사용자를 찾을 수 없습니다."));
    UserPaymentLimitSetting setting = user.getFeaturePreferences()
        .get(UserPaymentLimitSetting.KEY)
        .filter(UserPaymentLimitSetting.class::isInstance)
        .map(UserPaymentLimitSetting.class::cast)
        .orElse(UserPaymentLimitSetting.defaults());

    Community personalCommunity = communityContextManager
        .resolve(user, CommunityCategory.PERSONAL.name())
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("개인 커뮤니티를 찾을 수 없습니다."));

    long dailyUsed = sumUsedAmount(personalCommunity, dailyPeriod());
    long monthlyUsed = sumUsedAmount(personalCommunity, monthlyPeriod());

    return new PaymentLimitSettingResult(
        setting.getDailyLimit(),
        setting.getMonthlyLimit(),
        setting.isEnableDailyLimit(),
        setting.isEnableMonthlyLimit(),
        setting.isEnableNotifications(),
        dailyUsed,
        monthlyUsed
    );
  }

  @Override
  @Transactional
  public void update(UserDetails userDetails, UpdatePaymentLimitSettingCommand command) {
    User user = userContextManager.findByEmail(userDetails.getUsername(), User.class)
        .orElseThrow(() -> new IllegalArgumentException("등록된 사용자를 찾을 수 없습니다."));
    UserPaymentLimitSetting setting = UserPaymentLimitSetting.of(
        command.dailyLimit(),
        command.monthlyLimit(),
        command.enableDailyLimit(),
        command.enableMonthlyLimit(),
        command.enableNotifications()
    );
    user.applyFeaturePreference(setting);
    userContextManager.save(user, CommandType.UPDATE_PROFILE::name);
  }

  private long sumUsedAmount(Community community, Period period) {
    TransactionHistoryPage page = transactionQueryRepository.findCommunity(
        community.getId().getValue().toString(),
        AssetType.ALL.name(),
        TransactionType.ALL,
        null,
        period
    );
    return page.items().stream()
        .map(TransactionHistoryView::core)
        .filter(core -> core != null && core.amount() != null)
        .mapToLong(core -> core.amount() < 0 ? -core.amount() : 0)
        .sum();
  }

  private Period dailyPeriod() {
    ZoneId zone = ZoneId.systemDefault();
    LocalDate today = LocalDate.now(zone);
    Instant from = today.atStartOfDay(zone).toInstant();
    Instant to = today.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();
    return Period.of(from, to);
  }

  private Period monthlyPeriod() {
    ZoneId zone = ZoneId.systemDefault();
    LocalDate today = LocalDate.now(zone);
    LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
    LocalDate startOfNextMonth = startOfMonth.plusMonths(1);
    Instant from = startOfMonth.atStartOfDay(zone).toInstant();
    Instant to = startOfNextMonth.atStartOfDay(zone).minusNanos(1).toInstant();
    return Period.of(from, to);
  }
}
