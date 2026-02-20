package com.strataguard.core.util;

import com.strataguard.core.dto.amenity.BookingResponse;
import com.strataguard.core.dto.amenity.WaitlistResponse;
import com.strataguard.core.entity.AmenityBooking;
import com.strataguard.core.entity.BookingWaitlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "amenityName", ignore = true)
    @Mapping(target = "residentName", ignore = true)
    BookingResponse toResponse(AmenityBooking booking);

    WaitlistResponse toResponse(BookingWaitlist waitlist);
}
