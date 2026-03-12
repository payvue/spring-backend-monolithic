package com.payvue.app.application.summary;

import com.payvue.app.application.summary.category.balance.SummaryAccumulateFlow;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.app.domain.summary.BalancesByCategory;
import com.payvue.app.domain.summary.SummaryAggregationStrategy;
import com.payvue.app.domain.summary.SummaryService;
import com.payvue.core.asset.AssetClassification;
import java.util.Set;

import com.payvue.supporting.user.UserContextManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SummaryApplicationService {

  private final UserContextManager userContextManager;
  private final CommunityContextManager communityContextManager;
  private final SummaryService<BalancesByCategory> summaryService;
  private final SummaryAggregationStrategy<BalancesByCategory> aggregationStrategy;

  public BalancesByCategory accumulate(UserDetails userDetails, Set<AssetClassification> categories) {
    return SummaryAccumulateFlow.start(userDetails, categories)
        .user(step -> step.loadUser(userContextManager))
        .community(step -> step.loadPersonal(communityContextManager).loadShared(communityContextManager))
        .summary(step -> step.summarize(summaryService, aggregationStrategy))
        .end();
  }
}
