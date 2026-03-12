package com.payvue.core.community;

import static org.assertj.core.api.Assertions.assertThat;

import com.payvue.supporting.user.command.User;
import com.payvue.supporting.user.command.UserAccountView;
import com.payvue.supporting.user.command.UserId;
import com.payvue.supporting.user.command.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberShipTranslatorTest {

  @Test
  @DisplayName("사용자 멤버십은 커뮤니티 ID 집합으로 변환된다")
  void translateUserToCommunityIds() {
    User user = newUser();
    user.activate();
    String communityId = UUID.randomUUID().toString();
    user.joinCommunity(communityId);

    MemberShipTranslator translator = new MemberShipTranslator(new InMemoryUserRepository());
    Set<CommunityId> ids = translator.translate(user);

    assertThat(ids).contains(CommunityId.of(UUID.fromString(communityId)));
  }

  @Test
  @DisplayName("커뮤니티 참여자 정보는 UserAccountView 맵으로 변환된다")
  void translateCommunityToUserViews() {
    User user = newUser();
    UserId userId = user.getId();
    InMemoryUserRepository repository = new InMemoryUserRepository();
    repository.store = user;

    Community community = PersonalTestCommunity.create(userId.getValue());
    MemberShipTranslator translator = new MemberShipTranslator(repository);

    Map<UserId, UserAccountView> views = translator.translate(community);

    assertThat(views).containsKey(userId);
    assertThat(views.get(userId).userId()).isEqualTo(userId);
  }

  private User newUser() {
    return User.create(
        "login",
        "a@b.com",
        "010",
        "nick",
        "real",
        LocalDate.now(),
        "sido",
        "sigungu",
        "road",
        "detail",
        "12345"
    );
  }

  static final class InMemoryUserRepository implements UserRepository {
    User store;

    @Override
    public User save(User user) {
      store = user;
      return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
      return Optional.ofNullable(store);
    }

    @Override
    public Optional<User> findByLoginId(String loginId) {
      return Optional.ofNullable(store);
    }

    @Override
    public Optional<User> findByEmail(String email) {
      return Optional.ofNullable(store);
    }

    @Override
    public Set<UUID> participantedIn(String email) {
      return Set.of();
    }
  }

  static final class PersonalTestCommunity extends Community {
    private PersonalTestCommunity(UUID ownerId) {
      super("name", "desc", ownerId, new TestRole());
    }

    static PersonalTestCommunity create(UUID ownerId) {
      return new PersonalTestCommunity(ownerId);
    }

    @Override
    public String getCommunityType() {
      return "PERSONAL";
    }
  }

  static final class TestRole implements RoleDescriptor {
    @Override
    public String getName() {
      return "ADMIN";
    }

    @Override
    public boolean isAdmin() {
      return true;
    }
  }
}
