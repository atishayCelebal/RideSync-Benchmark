package com.ridesync.mapper;

import com.ridesync.dto.GroupResponseDto;
import com.ridesync.model.Group;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    
    GroupMapper INSTANCE = Mappers.getMapper(GroupMapper.class);
    
    @Mapping(target = "members", ignore = true) // We'll handle this separately if needed
    GroupResponseDto toGroupResponseDto(Group group);
    
    List<GroupResponseDto> toGroupResponseDtoList(List<Group> groups);
}
