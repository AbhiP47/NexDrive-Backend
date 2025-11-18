package com.appShala.userGroupService.ServiceImpl;

import com.appShala.userGroupService.Enum.MemberRole;
import com.appShala.userGroupService.Model.Membership;
import com.appShala.userGroupService.Model.UserGroup;
import com.appShala.userGroupService.Payload.MemberDTO;
import com.appShala.userGroupService.Payload.MembershipResponse;
import com.appShala.userGroupService.Repository.MembershipRepository;

import com.appShala.userGroupService.Repository.UserGroupRepository;
import com.appShala.userGroupService.Service.MembershipService;
import com.appShala.userGroupService.client.UserServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MembershipServiceImpl implements MembershipService {

    private final  MembershipRepository membershipRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserServiceClient userServiceClient;

    public MembershipServiceImpl(MembershipRepository membershipRepository, UserGroupRepository userGroupRepository , UserServiceClient userServiceClient)
    {
        this.membershipRepository = membershipRepository;
        this.userGroupRepository = userGroupRepository;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public List<Membership> buildAndSaveInitialMemberships(UserGroup group, UUID adminId, List<UUID> initialMembers) {
        Set<UUID> allMemberIds = new HashSet<>(initialMembers);
        allMemberIds.add(adminId);

        List<Membership> memberships = allMemberIds.stream()
                .map(userId -> Membership.builder()
                        .group(group)
                        .userId(userId)
                        .role(userId.equals(adminId) ? MemberRole.MANAGER : MemberRole.MEMBER)
                        .build()
                ).collect(Collectors.toList());
        memberships.add(Membership.builder()
                        .group(group)
                .userId(adminId)
                .role(MemberRole.MANAGER)
                .build());
        return membershipRepository.saveAll(memberships);
    }

    @Transactional(readOnly = true)
    public List<UUID> getGroupIdsByUserId(UUID userId)
    {
        if(userId == null)
            return Collections.emptyList();

        return membershipRepository.findAllGroupIdsByUserId(userId);
    }

    @Override
    @Transactional
    public MembershipResponse addMembership(List<UUID> userIds, UUID groupId, UUID adminId) {
        if(userIds == null || userIds.isEmpty())
             throw new RuntimeException("no user to add");

        List<UUID> existingMemberIds = membershipRepository.findUserIdsByGroupId(groupId);
        List<UUID> newMemberIds = userIds.stream()
                .filter(id -> !existingMemberIds.contains(id))
                .collect(Collectors.toList());
        if(newMemberIds.isEmpty())
            throw new IllegalArgumentException("All provided users are already present");
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(()-> new IllegalArgumentException("Group not found with ID :"+groupId));
        List<Membership> newMemberships = newMemberIds.stream()
                .map(userId -> Membership.builder()
                        .group(group)
                        .userId(userId)
                        .role(MemberRole.MEMBER)
                        .build())
                .collect(Collectors.toList());
        List<Membership> savedMemberships = membershipRepository.saveAll(newMemberships);

        return  convertToMembershipResponse(savedMemberships , adminId , group.getId());
    }

    @Override
    @Transactional
    public void deleteMembership(List<UUID> userIds, UUID groupId, UUID adminId) {
        membershipRepository.deleteByGroupIdAndUserIdIn(groupId,userIds);
    }

    @Override
    public List<UUID> findMemberUserIdsByGroupId(UUID groupId, MemberRole role) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findMemberUserIdsByGroupId(UUID groupId) {
        List<UUID> userIds = new ArrayList<>();
        try {
            userIds = membershipRepository.findAllUserIdsByGroupIdAndRoleMember(groupId);
            if(userIds.isEmpty())
                return Collections.emptyList();
            return userIds;
        }
        catch (Exception e)
        {
            throw new RuntimeException("could not retrieve the userIds");
        }
    }


    private MembershipResponse convertToMembershipResponse(List<Membership> savedMemberships , UUID adminId , UUID groupId)
    {

List<MemberDTO> memberDetails = savedMemberships.stream().map(membership ->
        MemberDTO.builder()
                .userId(membership.getUserId())
                .role(membership.getRole())
                .build())
        .collect(Collectors.toList());
MembershipResponse response = MembershipResponse.builder()
        .adminId(adminId)
        .groupId(groupId)
        .members(memberDetails)
        .build();
return response;
    }


}
