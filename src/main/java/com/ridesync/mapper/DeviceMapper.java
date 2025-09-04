package com.ridesync.mapper;

import com.ridesync.dto.DeviceResponseDto;
import com.ridesync.model.Device;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeviceMapper {
    
    DeviceMapper INSTANCE = Mappers.getMapper(DeviceMapper.class);
    
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "userName")
    DeviceResponseDto toDeviceResponseDto(Device device);
    
    List<DeviceResponseDto> toDeviceResponseDtoList(List<Device> devices);
}
