package com.ridesync.mapper;

import com.ridesync.dto.AlertResponseDto;
import com.ridesync.model.Alert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlertMapper {
    
    AlertMapper INSTANCE = Mappers.getMapper(AlertMapper.class);
    
    @Mapping(source = "ride.id", target = "rideId")
    @Mapping(source = "ride.name", target = "rideName")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "userName")
    @Mapping(source = "device.id", target = "deviceId")
    @Mapping(source = "device.deviceName", target = "deviceName")
    @Mapping(source = "device.deviceType", target = "deviceType")
    AlertResponseDto toAlertResponseDto(Alert alert);
    
    List<AlertResponseDto> toAlertResponseDtoList(List<Alert> alerts);
}
