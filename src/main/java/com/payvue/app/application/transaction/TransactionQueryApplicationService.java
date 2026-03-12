package com.payvue.app.application.transaction;

import com.payvue.app.domain.community.CommunityCategory;
import com.payvue.core.community.Community;
import com.payvue.core.community.CommunityContextManager;
import com.payvue.core.community.CommunityId;
import com.payvue.core.transaction.command.TransactionType;
import com.payvue.core.transaction.query.TransactionHistoryPage;
import com.payvue.core.transaction.query.TransactionQueryRepository;
import com.payvue.shared.Period;
import com.payvue.supporting.user.UserContextManager;
import com.payvue.supporting.user.command.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionQueryApplicationService implements TransactionQueryUseCase {

  private final UserContextManager userContextManager;
  private final TransactionQueryRepository queryRepository;
  private final CommunityContextManager communityContextManager;

  @Override
  public TransactionHistoryResult queryPersonal(
      UserDetails user,
      String assetType,
      TransactionType transactionType,
      Pageable pageable,
      Period period
  ) {
    User actor = userContextManager.findByEmail(user.getUsername(), User.class)
            .orElseThrow(() -> new IllegalArgumentException("등록된 사용자를 찾을 수 없습니다."));
    Community personalCommunity = communityContextManager.resolve(actor, CommunityCategory.PERSONAL.name())
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("개인 커뮤니티를 찾을 수 없습니다."));

    TransactionHistoryPage page = queryRepository.findCommunity(
        personalCommunity.getId().toString(),
        assetType,
        transactionType,
        pageable,
        period
    );
    TransactionHistoryResult.Pagination pagination = new TransactionHistoryResult.Pagination(
        pageable == null ? 1 : pageable.getPageNumber() + 1,
        pageable == null ? 0 : pageable.getPageSize(),
        page.total()
    );
    return TransactionHistoryResult.fromViews(page.items(), pagination);
  }

  @Override
  public TransactionHistoryResult queryCommunity(
      CommunityId communityId,
      String assetType,
      TransactionType transactionType,
      Pageable pageable,
      Period period
  ) {
    TransactionHistoryPage page = queryRepository.findCommunity(
        communityId.toString(),
        assetType,
        transactionType,
        pageable,
        period
    );
    TransactionHistoryResult.Pagination pagination = new TransactionHistoryResult.Pagination(
        pageable == null ? 1 : pageable.getPageNumber() + 1,
        pageable == null ? 0 : pageable.getPageSize(),
        page.total()
    );
    return TransactionHistoryResult.fromViews(page.items(), pagination);
  }
}
