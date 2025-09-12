package com.ridesync.mapper;

import com.ridesync.dto.AuthResponseDto;
import com.ridesync.dto.UserDto;
import com.ridesync.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    
    UserDto toUserDto(User user);
    
    @Mapping(target = "token", ignore = true)
    AuthResponseDto toAuthResponseDto(User user);
    
    @Mapping(target = "token", source = "token")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "email", source = "user.email")
    AuthResponseDto toAuthResponseDtoWithToken(User user, String token);
}
