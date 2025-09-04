package com.ridesync.mapper;

import com.ridesync.dto.LocationUpdateResponseDto;
import com.ridesync.model.LocationUpdate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    
    LocationMapper INSTANCE = Mappers.getMapper(LocationMapper.class);
    
    @Mapping(target = "userId", source = "user.id")
    LocationUpdateResponseDto toLocationUpdateResponseDto(LocationUpdate locationUpdate);
    
    List<LocationUpdateResponseDto> toLocationUpdateResponseDtoList(List<LocationUpdate> locationUpdates);
}
