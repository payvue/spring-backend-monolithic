package com.payvue.app.application.summary.category.balance;

import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.app.domain.summary.BalancesByCategory;
import com.payvue.app.domain.summary.SummaryAggregationStrategy;
import com.payvue.app.domain.summary.SummaryService;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.asset.AssetClassification;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.security.core.userdetails.UserDetails;

public final class SummaryAccumulateFlow {
  private final SummaryAccumulate context;

  private SummaryAccumulateFlow(SummaryAccumulate context) {
    this.context = context;
  }

  public static SummaryAccumulateFlow start(UserDetails userDetails, Set<AssetClassification> categories) {
    return new SummaryAccumulateFlow(new SummaryAccumulate(userDetails, categories));
  }

  public SummaryAccumulateFlow user(Consumer<SummaryAccumulateFlow> step) {
    step.accept(this);
    return this;
  }

  public SummaryAccumulateFlow community(Consumer<SummaryAccumulateFlow> step) {
    step.accept(this);
    return this;
  }

  public SummaryAccumulateFlow summary(Consumer<SummaryAccumulateFlow> step) {
    step.accept(this);
    return this;
  }

  public SummaryAccumulateFlow loadUser(UserContextManager userContextManager) {
    User user = userContextManager.findByEmail(context.userDetails().getUsername(), User.class)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    context.user(user);
    return this;
  }

  public SummaryAccumulateFlow loadPersonal(CommunityContextManager communityContextManager) {
    context.communities().put(
        CommunityCategory.PERSONAL,
        communityContextManager.resolve(context.user(), CommunityCategory.PERSONAL.name())
    );
    return this;
  }

  public SummaryAccumulateFlow loadShared(CommunityContextManager communityContextManager) {
    context.communities().put(
        CommunityCategory.COOPERATIVE,
        communityContextManager.resolve(context.user(), CommunityCategory.COOPERATIVE.name())
    );
    return this;
  }

  public SummaryAccumulateFlow summarize(
      SummaryService<BalancesByCategory> summaryService,
      SummaryAggregationStrategy<BalancesByCategory> aggregationStrategy
  ) {
    Map<CommunityCategory, Set<Community>> communities = context.communities();
    Map<CommunityCategory, BalancesByCategory> summaries = context.summaries();
    Map<CommunityCategory, BalancesByCategory> values = new EnumMap<>(CommunityCategory.class);
    for (Map.Entry<CommunityCategory, Set<Community>> entry : communities.entrySet()) {
      BalancesByCategory summary = summaryService.summarize(entry.getValue(), context.categories());
      summaries.put(entry.getKey(), summary);
      values.put(entry.getKey(), summary);
    }
    context.result(aggregationStrategy.aggregate(List.copyOf(values.values())));
    return this;
  }

  public BalancesByCategory end() {
    return context.result();
  }
}
