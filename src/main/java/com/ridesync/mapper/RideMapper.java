package com.ridesync.mapper;

import com.ridesync.dto.RideResponseDto;
import com.ridesync.model.Ride;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RideMapper {
    
    RideMapper INSTANCE = Mappers.getMapper(RideMapper.class);
    
    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    @Mapping(source = "createdBy.id", target = "createdBy")
    @Mapping(source = "createdBy.username", target = "createdByName")
    RideResponseDto toRideResponseDto(Ride ride);
    
    List<RideResponseDto> toRideResponseDtoList(List<Ride> rides);
}
