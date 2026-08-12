package com.moonback.pilgrimage.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.moonback.pilgrimage.config.MyBatisConfig;
import com.moonback.pilgrimage.model.dto.MemberDto;
import com.moonback.pilgrimage.model.type.MemberRole;
import com.moonback.pilgrimage.model.type.MemberStatus;
import com.moonback.pilgrimage.support.AbstractMySqlIntegrationTest;

@MybatisTest
@Import(MyBatisConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MemberMapperTest extends AbstractMySqlIntegrationTest {
	
	@Autowired
	private MemberMapper memberMapper;
	
	@Test
    void 회원_저장_후_id로_조회() {
		// given
		MemberDto member = new MemberDto();
		member.setEmail("mapper-test@example.com");
        member.setPassword("encoded-password");
        member.setNickname("매퍼테스트");
        member.setRoleId(MemberRole.USER.getId());
        member.setStatusId(MemberStatus.ACTIVE.getId());
		
		// when
        int result = memberMapper.insertMember(member);
		
		// then
        assertEquals(1, result);
        assertNotNull(member.getMemberId());

        MemberDto savedMember = memberMapper.findById(member.getMemberId());

        assertNotNull(savedMember);
        assertEquals("mapper-test@example.com", savedMember.getEmail());
        assertEquals("매퍼테스트", savedMember.getNickname());
        assertEquals(MemberRole.USER.getId(), savedMember.getRoleId());
        assertEquals(MemberStatus.ACTIVE.getId(), savedMember.getStatusId());
        assertNotNull(savedMember.getCreatedAt());
	}
	
	@Test
    void 이메일로_회원_조회() {
		// given
        MemberDto member = new MemberDto();
        member.setEmail("find-email@example.com");
        member.setPassword("encoded-password");
        member.setNickname("이메일조회");
        member.setRoleId(MemberRole.USER.getId());
        member.setStatusId(MemberStatus.ACTIVE.getId());

        memberMapper.insertMember(member);

        // when
        MemberDto foundMember = memberMapper.findByEmail("find-email@example.com");

        // then
        assertNotNull(foundMember);
        assertEquals(member.getMemberId(), foundMember.getMemberId());
        assertEquals("find-email@example.com", foundMember.getEmail());
        assertEquals("encoded-password", foundMember.getPassword());
        assertEquals("이메일조회", foundMember.getNickname());
        assertEquals(MemberRole.USER.getId(), foundMember.getRoleId());
        assertEquals(MemberStatus.ACTIVE.getId(), foundMember.getStatusId());
        assertNotNull(foundMember.getCreatedAt());
	}
	
	@Test
    void 이메일_개수_조회() {
		// given
        MemberDto member = new MemberDto();
        member.setEmail("count-test@example.com");
        member.setPassword("encoded-password");
        member.setNickname("카운트테스트");
        member.setRoleId(MemberRole.USER.getId());
        member.setStatusId(MemberStatus.ACTIVE.getId());

        memberMapper.insertMember(member);

        // when
        int count = memberMapper.countByEmail("count-test@example.com");

        // then
        assertEquals(1, count);
	}
	
	@Test
	void 회원정보_수정() {
		// given
	    MemberDto member = new MemberDto();
	    member.setEmail("update-" + System.nanoTime() + "@example.com");
	    member.setPassword("old-password");
	    member.setNickname("oldNickname");
	    member.setRoleId(MemberRole.USER.getId());
	    member.setStatusId(MemberStatus.ACTIVE.getId());

	    memberMapper.insertMember(member);

	    int memberId = member.getMemberId();

	    String newEmail = "updated-" + System.nanoTime() + "@example.com";
	    String newNickname = "newNickname";
	    String newPassword = "new-password";

	    // when
	    int count = memberMapper.updateMember(
	            memberId,
	            newEmail,
	            newNickname,
	            newPassword
	    );

	    // then
	    assertEquals(1, count);

	    MemberDto updatedMember = memberMapper.findById(memberId);

	    assertNotNull(updatedMember);
	    assertEquals(newEmail, updatedMember.getEmail());
	    assertEquals(newNickname, updatedMember.getNickname());
	    assertEquals(newPassword, updatedMember.getPassword());
	}
	
	@Test
	void 회원_탈퇴() {
		// given
	    MemberDto member = new MemberDto();
	    member.setEmail("withdraw-" + System.nanoTime() + "@example.com");
	    member.setPassword("encoded-password");
	    member.setNickname("탈퇴테스트");
	    member.setRoleId(MemberRole.USER.getId());
	    member.setStatusId(MemberStatus.ACTIVE.getId());

	    memberMapper.insertMember(member);

	    int memberId = member.getMemberId();

	    // when
	    int result = memberMapper.withdrawById(memberId);

	    // then
	    assertEquals(1, result);

	    MemberDto withdrawnMember = memberMapper.findById(memberId);

	    assertNotNull(withdrawnMember);
	    assertEquals(MemberStatus.WITHDRAWN.getId(), withdrawnMember.getStatusId());
	}
}
