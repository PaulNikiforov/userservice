package com.innowise.userservice.mapper;

import com.innowise.userservice.model.dto.PaymentCardRequestDTO;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;
import com.innowise.userservice.model.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PaymentCardMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "number", target = "number", qualifiedByName = "maskCardNumber")
    PaymentCardResponseDTO toResponseDTO(PaymentCard card);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    PaymentCard toEntity(PaymentCardRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntityFromDTO(PaymentCardRequestDTO dto, @MappingTarget PaymentCard card);

    @org.mapstruct.Named("maskCardNumber")
    default String maskCardNumber(String number) {
        if (number == null || number.length() < 4) {
            return "****";
        }
        return "**** **** **** " + number.substring(number.length() - 4);
    }
}
