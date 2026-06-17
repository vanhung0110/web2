package com.hotelchain.pro.utility.service;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.booking.entity.Booking;
import com.hotelchain.pro.booking.repository.BookingRepository;
import com.hotelchain.pro.common.enums.ReadingStatus;
import com.hotelchain.pro.common.exception.ResourceNotFoundException;
import com.hotelchain.pro.common.exception.UtilityException;
import com.hotelchain.pro.property.entity.Property;
import com.hotelchain.pro.property.repository.PropertyRepository;
import com.hotelchain.pro.utility.dto.*;
import com.hotelchain.pro.utility.entity.UtilityPriceConfig;
import com.hotelchain.pro.utility.entity.UtilityReading;
import com.hotelchain.pro.utility.repository.UtilityPriceConfigRepository;
import com.hotelchain.pro.utility.repository.UtilityReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilityService {

    private final UtilityReadingRepository utilityReadingRepository;
    private final UtilityPriceConfigRepository utilityPriceConfigRepository;
    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    public Object recordStartReading(StartReadingRequest request, User user) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId().toString()));

        UtilityReading reading = utilityReadingRepository.findByBookingId(request.getBookingId()).orElse(null);
        if (reading == null) {
            reading = new UtilityReading();
            reading.setBooking(booking);
            reading.setRoom(booking.getRoom());
        }

        reading.setWaterIndexStart(request.getWaterIndexStart());
        reading.setWaterPhotoStartKey(request.getWaterPhotoStartKey());
        reading.setElectricIndexStart(request.getElectricIndexStart());
        reading.setElectricPhotoStartKey(request.getElectricPhotoStartKey());
        reading.setRecordedByStart(user);
        reading.setRecordedAtStart(LocalDateTime.now());
        reading.setStatus(ReadingStatus.PENDING_END);

        return utilityReadingRepository.save(reading);
    }

    @Transactional
    public Object recordEndReading(EndReadingRequest request, User user) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId().toString()));

        UtilityReading reading = utilityReadingRepository.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new UtilityException("UTILITY_005", "Chưa ghi nhận chỉ số đầu kỳ"));

        if (request.getWaterIndexEnd() < reading.getWaterIndexStart()) {
            throw new UtilityException("PAYMENT_003", "Chỉ số nước cuối kỳ không được nhỏ hơn đầu kỳ");
        }
        if (request.getElectricIndexEnd() < reading.getElectricIndexStart()) {
            throw new UtilityException("PAYMENT_003", "Chỉ số điện cuối kỳ không được nhỏ hơn đầu kỳ");
        }

        reading.setWaterIndexEnd(request.getWaterIndexEnd());
        reading.setWaterPhotoEndKey(request.getWaterPhotoEndKey());
        reading.setElectricIndexEnd(request.getElectricIndexEnd());
        reading.setElectricPhotoEndKey(request.getElectricPhotoEndKey());
        reading.setRecordedByEnd(user);
        reading.setRecordedAtEnd(LocalDateTime.now());
        reading.setStatus(ReadingStatus.COMPLETED);

        // Tính tiêu thụ
        reading.setWaterUsage(reading.getWaterIndexEnd() - reading.getWaterIndexStart());
        reading.setElectricUsage(reading.getElectricIndexEnd() - reading.getElectricIndexStart());

        // Lấy config giá
        UtilityPriceConfig config = utilityPriceConfigRepository.findFirstByPropertyIdAndIsActiveTrueOrderByCreatedAtDesc(booking.getRoom().getProperty().getId()).orElse(null);
        BigDecimal waterPrice = config != null ? config.getWaterPricePerUnit() : new BigDecimal("15000");
        BigDecimal electricPrice = config != null && config.getUseFixedElectricPrice() ? config.getFixedElectricPrice() : new BigDecimal("3500");

        reading.setWaterPricePerUnit(waterPrice);
        reading.setWaterTotal(BigDecimal.valueOf(reading.getWaterUsage()).multiply(waterPrice));

        reading.setElectricPricePerUnit(electricPrice);
        reading.setElectricTotal(BigDecimal.valueOf(reading.getElectricUsage()).multiply(electricPrice));

        UtilityReading saved = utilityReadingRepository.save(reading);

        // Cập nhật booking utility cost
        booking.setUtilityCost(saved.getWaterTotal().add(saved.getElectricTotal()));
        booking.setTotalAmount(booking.getTotalRoomFee().add(booking.getUtilityCost()).add(booking.getServiceFee()).subtract(booking.getDiscount()));
        booking.setRemainingAmount(booking.getTotalAmount().subtract(booking.getDepositAmount()));
        bookingRepository.save(booking);

        return saved;
    }

    public Object getReadings(UUID bookingId) {
        return utilityReadingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityReading for Booking", bookingId.toString()));
    }

    @Transactional
    public Object verifyReading(UUID id) {
        UtilityReading reading = utilityReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityReading", id.toString()));

        reading.setWaterManualVerified(true);
        reading.setElectricManualVerified(true);
        return utilityReadingRepository.save(reading);
    }

    @Transactional
    public Object disputeReading(UUID id, DisputeRequest request) {
        UtilityReading reading = utilityReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UtilityReading", id.toString()));

        reading.setStatus(ReadingStatus.DISPUTED);
        reading.setWaterDiscrepancyNote(request.getNotes());
        reading.setElectricDiscrepancyNote(request.getNotes());
        return utilityReadingRepository.save(reading);
    }

    public List<Object> getRoomHistory(UUID roomId) {
        return utilityReadingRepository.findByRoomId(roomId).stream()
                .map(r -> (Object) r)
                .collect(Collectors.toList());
    }

    public Object getPrices(UUID propertyId) {
        return utilityPriceConfigRepository.findFirstByPropertyIdAndIsActiveTrueOrderByCreatedAtDesc(propertyId)
                .orElseGet(() -> {
                    // Tạo một cấu hình mặc định tạm thời nếu chưa có
                    UtilityPriceConfig config = new UtilityPriceConfig();
                    config.setWaterPricePerUnit(new BigDecimal("15000"));
                    config.setUseFixedElectricPrice(true);
                    config.setFixedElectricPrice(new BigDecimal("3500"));
                    return config;
                });
    }

    @Transactional
    public Object updatePrices(UUID propertyId, UpdatePricesRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId.toString()));

        // Tắt các cấu hình cũ của Property này
        utilityPriceConfigRepository.findFirstByPropertyIdAndIsActiveTrueOrderByCreatedAtDesc(propertyId).ifPresent(old -> {
            old.setIsActive(false);
            utilityPriceConfigRepository.save(old);
        });

        UtilityPriceConfig config = new UtilityPriceConfig();
        config.setProperty(property);
        config.setWaterPricePerUnit(request.getWaterPricePerUnit() != null ? request.getWaterPricePerUnit() : new BigDecimal("15000"));
        config.setUseFixedElectricPrice(request.getUseFixedElectricPrice() != null ? request.getUseFixedElectricPrice() : true);
        config.setFixedElectricPrice(request.getFixedElectricPrice() != null ? request.getFixedElectricPrice() : new BigDecimal("3500"));

        config.setElectricTier1Price(request.getElectricTier1Price() != null ? request.getElectricTier1Price() : new BigDecimal("1728"));
        config.setElectricTier2Price(request.getElectricTier2Price() != null ? request.getElectricTier2Price() : new BigDecimal("1786"));
        config.setElectricTier3Price(request.getElectricTier3Price() != null ? request.getElectricTier3Price() : new BigDecimal("2074"));
        config.setElectricTier4Price(request.getElectricTier4Price() != null ? request.getElectricTier4Price() : new BigDecimal("2612"));
        config.setElectricTier5Price(request.getElectricTier5Price() != null ? request.getElectricTier5Price() : new BigDecimal("2919"));
        config.setElectricTier6Price(request.getElectricTier6Price() != null ? request.getElectricTier6Price() : new BigDecimal("3015"));

        config.setIsActive(true);
        config.setEffectiveFrom(LocalDate.now());

        return utilityPriceConfigRepository.save(config);
    }
}
