package com.hotelchain.pro.booking.service;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.booking.dto.*;
import com.hotelchain.pro.booking.entity.Booking;
import com.hotelchain.pro.booking.entity.Guest;
import com.hotelchain.pro.booking.repository.BookingRepository;
import com.hotelchain.pro.booking.repository.GuestRepository;
import com.hotelchain.pro.common.enums.*;
import com.hotelchain.pro.common.exception.ResourceNotFoundException;
import com.hotelchain.pro.common.exception.BookingException;
import com.hotelchain.pro.common.exception.HotelChainException;
import com.hotelchain.pro.payment.entity.BankConfig;
import com.hotelchain.pro.payment.entity.Payment;
import com.hotelchain.pro.payment.repository.BankConfigRepository;
import com.hotelchain.pro.payment.repository.PaymentRepository;
import com.hotelchain.pro.payment.service.VietQRService;
import com.hotelchain.pro.payment.dto.GenerateQRRequest;
import com.hotelchain.pro.payment.dto.QRPaymentResponse;
import com.hotelchain.pro.room.entity.Room;
import com.hotelchain.pro.room.repository.RoomRepository;
import com.hotelchain.pro.utility.entity.UtilityPriceConfig;
import com.hotelchain.pro.utility.entity.UtilityReading;
import com.hotelchain.pro.utility.repository.UtilityPriceConfigRepository;
import com.hotelchain.pro.utility.repository.UtilityReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final PaymentRepository paymentRepository;
    private final UtilityReadingRepository utilityReadingRepository;
    private final UtilityPriceConfigRepository utilityPriceConfigRepository;
    private final BankConfigRepository bankConfigRepository;
    private final VietQRService vietQRService;

    public Page<BookingDto> listBookings(BookingFilter filter, Pageable pageable, User user) {
        BookingStatus status = null;
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            status = BookingStatus.valueOf(filter.getStatus().toUpperCase());
        }

        LocalDateTime fromTime = null;
        if (filter.getFrom() != null && !filter.getFrom().isBlank()) {
            fromTime = LocalDateTime.parse(filter.getFrom());
        }

        LocalDateTime toTime = null;
        if (filter.getTo() != null && !filter.getTo().isBlank()) {
            toTime = LocalDateTime.parse(filter.getTo());
        }

        UUID propertyId = filter.getPropertyId();
        if (propertyId == null && user.getRole() != Role.SUPER_ADMIN && !user.getAssignedPropertyIds().isEmpty()) {
            propertyId = user.getAssignedPropertyIds().iterator().next();
        }

        Page<Booking> page = bookingRepository.findByFilters(
                propertyId,
                status,
                filter.getSearchQuery(),
                fromTime,
                toTime,
                pageable
        );

        return page.map(this::mapToDto);
    }

    @Transactional
    public BookingDto createBooking(CreateBookingRequest request, User user) {
        // Kiểm tra xung đột phòng
        long conflicts = bookingRepository.countConflictingBookings(
                request.getRoomId(),
                request.getCheckInPlan(),
                request.getCheckOutPlan(),
                null
        );
        if (conflicts > 0) {
            throw new BookingException("BOOKING_001", "Phòng đã được đặt trong thời gian này");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", request.getRoomId().toString()));

        // Tìm hoặc tạo khách hàng
        Guest guest;
        if (request.getGuestId() != null) {
            guest = guestRepository.findById(request.getGuestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Guest", request.getGuestId().toString()));
        } else {
            guest = guestRepository.findByPhone(request.getPhone()).orElse(null);
            if (guest == null) {
                guest = new Guest();
                guest.setFullName(request.getFullName());
                guest.setPhone(request.getPhone());
                guest.setEmail(request.getEmail());
                guest.setIdNumber(request.getIdNumber());
                guest.setIdType(request.getIdType());
                guest = guestRepository.save(guest);
            }
        }

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setGuest(guest);
        booking.setCreatedByUser(user);
        booking.setPropertyId(room.getProperty().getId());
        booking.setCheckInPlan(request.getCheckInPlan());
        booking.setCheckOutPlan(request.getCheckOutPlan());
        booking.setStatus(BookingStatus.PENDING);
        booking.setSource(request.getSource() != null ? BookingSource.valueOf(request.getSource().toUpperCase()) : BookingSource.WALK_IN);
        booking.setAdultsCount(request.getAdultsCount());
        booking.setChildrenCount(request.getChildrenCount());
        booking.setRoomRatePerNight(request.getRoomRatePerNight());

        long nights = Math.max(1, ChronoUnit.DAYS.between(request.getCheckInPlan().toLocalDate(), request.getCheckOutPlan().toLocalDate()));
        BigDecimal totalRoomFee = request.getRoomRatePerNight().multiply(BigDecimal.valueOf(nights));
        booking.setTotalRoomFee(totalRoomFee);
        booking.setTotalAmount(totalRoomFee);
        booking.setDepositAmount(request.getDepositAmount() != null ? request.getDepositAmount() : BigDecimal.ZERO);
        booking.setRemainingAmount(totalRoomFee.subtract(booking.getDepositAmount()));
        booking.setSpecialRequests(request.getSpecialRequests());

        // Sinh mã đặt phòng tự động
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        booking.setBookingCode("BK-" + dateStr + "-" + randomSuffix);

        Booking saved = bookingRepository.save(booking);
        return mapToDto(saved);
    }

    public BookingDto getBooking(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id.toString()));
        return mapToDto(booking);
    }

    @Transactional
    public BookingDto confirmBooking(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id.toString()));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingException("BOOKING_014", "Không thể xác nhận đặt phòng có trạng thái hiện tại là: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return mapToDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto checkIn(UUID id, CheckInRequest request, User user) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id.toString()));

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setActualCheckIn(request.getActualCheckIn() != null ? request.getActualCheckIn() : LocalDateTime.now());
        if (request.getNotes() != null) {
            booking.setInternalNote(request.getNotes());
        }

        // Cập nhật thông tin giấy tờ khách
        Guest guest = booking.getGuest();
        if (request.getGuestIdNumber() != null) {
            guest.setIdNumber(request.getGuestIdNumber());
        }
        if (request.getGuestIdType() != null) {
            guest.setIdType(request.getGuestIdType());
        }
        if (request.getGuestIdImageFrontKey() != null) {
            guest.setIdImageFrontKey(request.getGuestIdImageFrontKey());
        }
        if (request.getGuestIdImageBackKey() != null) {
            guest.setIdImageBackKey(request.getGuestIdImageBackKey());
        }
        guestRepository.save(guest);

        // Đánh dấu phòng là OCCUPIED
        Room room = booking.getRoom();
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        // Tạo chỉ số đồng hồ nước/điện bắt đầu
        UtilityReading reading = new UtilityReading();
        reading.setBooking(booking);
        reading.setRoom(room);
        reading.setWaterIndexStart(request.getWaterIndexStart() != null ? request.getWaterIndexStart() : 0.0);
        reading.setWaterPhotoStartKey(request.getWaterPhotoStartKey());
        reading.setElectricIndexStart(request.getElectricIndexStart() != null ? request.getElectricIndexStart() : 0.0);
        reading.setElectricPhotoStartKey(request.getElectricPhotoStartKey());
        reading.setRecordedByStart(user);
        reading.setRecordedAtStart(LocalDateTime.now());
        reading.setStatus(ReadingStatus.PENDING_END);
        utilityReadingRepository.save(reading);

        return mapToDto(bookingRepository.save(booking));
    }

    @Transactional
    public CheckOutResponse checkOut(UUID id, CheckOutRequest request, User user) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id.toString()));

        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setActualCheckOut(request.getActualCheckOut() != null ? request.getActualCheckOut() : LocalDateTime.now());

        // Đánh dấu phòng là CLEANING
        Room room = booking.getRoom();
        room.setStatus(RoomStatus.CLEANING);
        roomRepository.save(room);

        // Cập nhật chỉ số đồng hồ cuối kỳ
        UtilityReading reading = utilityReadingRepository.findByBookingId(id)
                .orElseGet(() -> {
                    UtilityReading r = new UtilityReading();
                    r.setBooking(booking);
                    r.setRoom(room);
                    r.setWaterIndexStart(0.0);
                    r.setElectricIndexStart(0.0);
                    r.setRecordedByStart(user);
                    r.setRecordedAtStart(LocalDateTime.now());
                    return r;
                });

        reading.setWaterIndexEnd(request.getWaterIndexEnd());
        reading.setWaterPhotoEndKey(request.getWaterPhotoEndKey());
        reading.setElectricIndexEnd(request.getElectricIndexEnd());
        reading.setElectricPhotoEndKey(request.getElectricPhotoEndKey());
        reading.setRecordedByEnd(user);
        reading.setRecordedAtEnd(LocalDateTime.now());
        reading.setStatus(ReadingStatus.COMPLETED);

        // Tính lượng tiêu thụ
        double waterUsage = Math.max(0.0, reading.getWaterIndexEnd() - reading.getWaterIndexStart());
        double electricUsage = Math.max(0.0, reading.getElectricIndexEnd() - reading.getElectricIndexStart());

        reading.setWaterUsage(waterUsage);
        reading.setElectricUsage(electricUsage);

        // Lấy cấu hình giá tiện ích
        UtilityPriceConfig priceConfig = utilityPriceConfigRepository.findFirstByPropertyIdAndIsActiveTrueOrderByCreatedAtDesc(room.getProperty().getId())
                .orElse(null);

        BigDecimal waterPrice = new BigDecimal("15000");
        BigDecimal electricPrice = new BigDecimal("3500");
        boolean useFixedElectric = true;

        if (priceConfig != null) {
            waterPrice = priceConfig.getWaterPricePerUnit();
            useFixedElectric = priceConfig.getUseFixedElectricPrice();
            if (useFixedElectric) {
                electricPrice = priceConfig.getFixedElectricPrice();
            }
        }

        BigDecimal waterTotal = BigDecimal.valueOf(waterUsage).multiply(waterPrice);
        BigDecimal electricTotal;

        if (useFixedElectric) {
            electricTotal = BigDecimal.valueOf(electricUsage).multiply(electricPrice);
        } else {
            // Giá điện bậc thang EVN
            electricTotal = calculateElectricTieredTotal(electricUsage, priceConfig);
            if (electricUsage > 0) {
                electricPrice = electricTotal.divide(BigDecimal.valueOf(electricUsage), 2, RoundingMode.HALF_UP);
            }
        }

        reading.setWaterPricePerUnit(waterPrice);
        reading.setWaterTotal(waterTotal);
        reading.setElectricPricePerUnit(electricPrice);
        reading.setElectricTotal(electricTotal);
        utilityReadingRepository.save(reading);

        // Cập nhật chi phí đặt phòng
        BigDecimal utilityCost = waterTotal.add(electricTotal);
        booking.setUtilityCost(utilityCost);
        booking.setTotalAmount(booking.getTotalRoomFee().add(utilityCost).add(booking.getServiceFee()).subtract(booking.getDiscount()));
        booking.setRemainingAmount(booking.getTotalAmount().subtract(booking.getDepositAmount()));
        bookingRepository.save(booking);

        // Sinh QR VietQR thanh toán phần còn lại nếu còn nợ
        String qrUrl = null;
        if (booking.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
            BankConfig bankConfig = bankConfigRepository.findByPropertyId(room.getProperty().getId()).orElse(null);
            if (bankConfig != null && bankConfig.getIsActive()) {
                try {
                    String content = bankConfig.getTemplateDescription() != null
                            ? bankConfig.getTemplateDescription().replace("{BOOKING_CODE}", booking.getBookingCode())
                            : "THANH TOAN BK " + booking.getBookingCode();

                    GenerateQRRequest qrReq = GenerateQRRequest.builder()
                            .accountNo(bankConfig.getAccountNumber())
                            .accountName(bankConfig.getAccountHolderName())
                            .bankBin(bankConfig.getBankBin())
                            .transferContent(content)
                            .amount(booking.getRemainingAmount().longValue())
                            .bookingCode(booking.getBookingCode())
                            .build();

                    QRPaymentResponse qrResp = vietQRService.generateDynamicQR(qrReq);
                    if (qrResp.isSuccess()) {
                        qrUrl = qrResp.getQrDataUrl();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return CheckOutResponse.builder()
                .bookingCode(booking.getBookingCode())
                .roomNumber(room.getRoomNumber())
                .roomFee(booking.getTotalRoomFee())
                .waterUsage(waterUsage)
                .waterCost(waterTotal)
                .electricUsage(electricUsage)
                .electricCost(electricTotal)
                .serviceFee(booking.getServiceFee())
                .discount(booking.getDiscount())
                .totalAmount(booking.getTotalAmount())
                .depositAmount(booking.getDepositAmount())
                .remainingAmount(booking.getRemainingAmount())
                .qrCodeUrl(qrUrl)
                .paymentStatus(booking.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0 ? "PAID" : "PENDING")
                .build();
    }

    private BigDecimal calculateElectricTieredTotal(double usage, UtilityPriceConfig config) {
        if (usage <= 0) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal tier1 = config != null ? config.getElectricTier1Price() : new BigDecimal("1728");
        BigDecimal tier2 = config != null ? config.getElectricTier2Price() : new BigDecimal("1786");
        BigDecimal tier3 = config != null ? config.getElectricTier3Price() : new BigDecimal("2074");
        BigDecimal tier4 = config != null ? config.getElectricTier4Price() : new BigDecimal("2612");
        BigDecimal tier5 = config != null ? config.getElectricTier5Price() : new BigDecimal("2919");
        BigDecimal tier6 = config != null ? config.getElectricTier6Price() : new BigDecimal("3015");

        if (usage <= 50) {
            total = total.add(BigDecimal.valueOf(usage).multiply(tier1));
        } else if (usage <= 100) {
            total = total.add(BigDecimal.valueOf(50).multiply(tier1));
            total = total.add(BigDecimal.valueOf(usage - 50).multiply(tier2));
        } else if (usage <= 200) {
            total = total.add(BigDecimal.valueOf(50).multiply(tier1));
            total = total.add(BigDecimal.valueOf(50).multiply(tier2));
            total = total.add(BigDecimal.valueOf(usage - 100).multiply(tier3));
        } else if (usage <= 300) {
            total = total.add(BigDecimal.valueOf(50).multiply(tier1));
            total = total.add(BigDecimal.valueOf(50).multiply(tier2));
            total = total.add(BigDecimal.valueOf(100).multiply(tier3));
            total = total.add(BigDecimal.valueOf(usage - 200).multiply(tier4));
        } else if (usage <= 400) {
            total = total.add(BigDecimal.valueOf(50).multiply(tier1));
            total = total.add(BigDecimal.valueOf(50).multiply(tier2));
            total = total.add(BigDecimal.valueOf(100).multiply(tier3));
            total = total.add(BigDecimal.valueOf(100).multiply(tier4));
            total = total.add(BigDecimal.valueOf(usage - 300).multiply(tier5));
        } else {
            total = total.add(BigDecimal.valueOf(50).multiply(tier1));
            total = total.add(BigDecimal.valueOf(50).multiply(tier2));
            total = total.add(BigDecimal.valueOf(100).multiply(tier3));
            total = total.add(BigDecimal.valueOf(100).multiply(tier4));
            total = total.add(BigDecimal.valueOf(100).multiply(tier5));
            total = total.add(BigDecimal.valueOf(usage - 400).multiply(tier6));
        }
        return total;
    }

    @Transactional
    public BookingDto cancelBooking(UUID id, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id.toString()));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setInternalNote((booking.getInternalNote() != null ? booking.getInternalNote() + "; " : "") + "Hủy do: " + reason);

        // Trả trạng thái phòng về AVAILABLE nếu phòng đang ở OCCUPIED/CHECKED_IN
        Room room = booking.getRoom();
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        return mapToDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto extendBooking(UUID id, ExtendBookingRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id.toString()));

        booking.setCheckOutPlan(request.getNewCheckOutPlan());

        // Tính lại tiền phòng
        long nights = Math.max(1, ChronoUnit.DAYS.between(booking.getCheckInPlan().toLocalDate(), request.getNewCheckOutPlan().toLocalDate()));
        BigDecimal totalRoomFee = booking.getRoomRatePerNight().multiply(BigDecimal.valueOf(nights));
        booking.setTotalRoomFee(totalRoomFee);
        booking.setTotalAmount(totalRoomFee.add(booking.getUtilityCost()).add(booking.getServiceFee()).subtract(booking.getDiscount()));
        booking.setRemainingAmount(booking.getTotalAmount().subtract(booking.getDepositAmount()));

        return mapToDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto changeRoom(UUID id, ChangeRoomRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id.toString()));

        Room oldRoom = booking.getRoom();
        Room newRoom = roomRepository.findById(request.getNewRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", request.getNewRoomId().toString()));

        // Check conflicts for new room
        long conflicts = bookingRepository.countConflictingBookings(
                newRoom.getId(),
                booking.getCheckInPlan(),
                booking.getCheckOutPlan(),
                booking.getId()
        );
        if (conflicts > 0) {
            throw new BookingException("BOOKING_001", "Phòng mới đã có khách đặt trong khoảng thời gian này");
        }

        // Đổi trạng thái phòng
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            oldRoom.setStatus(RoomStatus.CLEANING);
            newRoom.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(oldRoom);
            roomRepository.save(newRoom);

            // Chuyển UtilityReading sang phòng mới (cần ghi nhận chỉ số bắt đầu của phòng mới)
            UtilityReading reading = utilityReadingRepository.findByBookingId(id).orElse(null);
            if (reading != null) {
                reading.setRoom(newRoom);
                reading.setWaterIndexStart(newRoom.getInitialWaterIndex());
                reading.setElectricIndexStart(newRoom.getInitialElectricIndex());
                utilityReadingRepository.save(reading);
            }
        }

        booking.setRoom(newRoom);
        return mapToDto(bookingRepository.save(booking));
    }

    public Object getInvoice(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id.toString()));

        UtilityReading reading = utilityReadingRepository.findByBookingId(id).orElse(null);

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("bookingCode", booking.getBookingCode());
        map.put("guestName", booking.getGuest().getFullName());
        map.put("roomNumber", booking.getRoom().getRoomNumber());
        map.put("roomFee", booking.getTotalRoomFee());
        map.put("waterUsage", reading != null && reading.getWaterUsage() != null ? reading.getWaterUsage() : 0.0);
        map.put("waterCost", reading != null && reading.getWaterTotal() != null ? reading.getWaterTotal() : BigDecimal.ZERO);
        map.put("electricUsage", reading != null && reading.getElectricUsage() != null ? reading.getElectricUsage() : 0.0);
        map.put("electricCost", reading != null && reading.getElectricTotal() != null ? reading.getElectricTotal() : BigDecimal.ZERO);
        map.put("serviceFee", booking.getServiceFee());
        map.put("discount", booking.getDiscount());
        map.put("totalAmount", booking.getTotalAmount());
        map.put("depositAmount", booking.getDepositAmount());
        map.put("remainingAmount", booking.getRemainingAmount());
        return map;
    }

    public byte[] generateInvoicePdf(UUID id) {
        // Trả về mock PDF bytes đơn giản
        String pdfMock = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] >>\nendobj\nxref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n0000000060 00000 n\n0000000121 00000 n\ntrailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n200\n%%EOF";
        return pdfMock.getBytes();
    }

    private BookingDto mapToDto(Booking booking) {
        return BookingDto.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .roomId(booking.getRoom().getId())
                .roomNumber(booking.getRoom().getRoomNumber())
                .guestId(booking.getGuest().getId())
                .guestName(booking.getGuest().getFullName())
                .createdByUserId(booking.getCreatedByUser() != null ? booking.getCreatedByUser().getId() : null)
                .propertyId(booking.getPropertyId())
                .checkInPlan(booking.getCheckInPlan())
                .checkOutPlan(booking.getCheckOutPlan())
                .actualCheckIn(booking.getActualCheckIn())
                .actualCheckOut(booking.getActualCheckOut())
                .status(booking.getStatus())
                .source(booking.getSource())
                .adultsCount(booking.getAdultsCount())
                .childrenCount(booking.getChildrenCount())
                .roomRatePerNight(booking.getRoomRatePerNight())
                .totalRoomFee(booking.getTotalRoomFee())
                .utilityCost(booking.getUtilityCost())
                .serviceFee(booking.getServiceFee())
                .discount(booking.getDiscount())
                .totalAmount(booking.getTotalAmount())
                .depositAmount(booking.getDepositAmount())
                .remainingAmount(booking.getRemainingAmount())
                .specialRequests(booking.getSpecialRequests())
                .internalNote(booking.getInternalNote())
                .build();
    }
}
