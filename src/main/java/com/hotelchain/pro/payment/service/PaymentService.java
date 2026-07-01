package com.hotelchain.pro.payment.service;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.booking.entity.Booking;
import com.hotelchain.pro.booking.repository.BookingRepository;
import com.hotelchain.pro.common.enums.PaymentMethod;
import com.hotelchain.pro.common.enums.PaymentStatus;
import com.hotelchain.pro.common.exception.ResourceNotFoundException;
import com.hotelchain.pro.common.exception.PaymentException;
import com.hotelchain.pro.payment.dto.*;
import com.hotelchain.pro.payment.entity.BankConfig;
import com.hotelchain.pro.payment.entity.Payment;
import com.hotelchain.pro.payment.repository.BankConfigRepository;
import com.hotelchain.pro.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BankConfigRepository bankConfigRepository;
    private final VietQRService vietQRService;
    private final PaymentSecurityService paymentSecurityService;

    @Transactional
    public QRPaymentResponse generateQrForBooking(GenerateBookingQrRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId().toString()));

        BankConfig bankConfig = bankConfigRepository.findByPropertyId(booking.getRoom().getProperty().getId())
                .orElseThrow(() -> new PaymentException("PAYMENT_004", "Cấu hình ngân hàng chưa được thiết lập"));

        String content = bankConfig.getTemplateDescription() != null
                ? bankConfig.getTemplateDescription().replace("{BOOKING_CODE}", booking.getBookingCode())
                : "THANH TOAN PHONG " + booking.getBookingCode();

        GenerateQRRequest qrReq = GenerateQRRequest.builder()
                .accountNo(bankConfig.getAccountNumber())
                .accountName(bankConfig.getAccountHolderName())
                .bankBin(bankConfig.getBankBin())
                .transferContent(content)
                .amount(request.getAmount().longValue())
                .bookingCode(booking.getBookingCode())
                .build();

        QRPaymentResponse qrResp = vietQRService.generateDynamicQR(qrReq);

        if (qrResp.isSuccess()) {
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setAmount(request.getAmount());
            payment.setMethod(PaymentMethod.BANK_TRANSFER);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setTransferContent(content);
            payment.setVietQrCode(qrResp.getQrCode());
            payment.setVietQrDataUrl(qrResp.getQrDataUrl());
            paymentRepository.save(payment);
        }

        return qrResp;
    }

    @Transactional
    public Object confirmPayment(ConfirmPaymentRequest request, User user) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId().toString()));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(request.getAmount());
        payment.setMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(request.getTransactionId());
        payment.setNotes(request.getNotes());
        payment.setPaidAt(LocalDateTime.now());
        payment.setConfirmedAt(LocalDateTime.now());
        payment.setConfirmedBy(user);
        paymentRepository.save(payment);

        // Cập nhật remainingAmount của booking
        booking.setRemainingAmount(booking.getRemainingAmount().subtract(request.getAmount()));
        bookingRepository.save(booking);

        return payment;
    }

    @Transactional
    public void processWebhook(String payload) {
        // Mocking webhook parser: Tìm bookingCode và transactionId trong payload
        log.info("Processing bank webhook payload: {}", payload);

        // Giả sử payload có dạng JSON đơn giản chứa bookingCode, amount, transactionId
        // hoặc chúng ta trích xuất được từ chuỗi
        String transactionId = "TXN-" + System.currentTimeMillis();
        BigDecimal amount = BigDecimal.ZERO;
        String bookingCode = null;

        // Đơn giản hóa: Trích xuất bằng regex hoặc string contains
        if (payload.contains("BK-")) {
            int index = payload.indexOf("BK-");
            if (index != -1 && index + 16 <= payload.length()) {
                bookingCode = payload.substring(index, index + 16); // Format: BK-YYYYMMDD-XXXX
            }
        }

        if (bookingCode != null) {
            Booking booking = bookingRepository.findByBookingCode(bookingCode).orElse(null);
            if (booking != null) {
                // Kiểm tra trùng giao dịch
                if (paymentSecurityService.isAlreadyProcessed(transactionId)) {
                    log.warn("Webhook transaction already processed: {}", transactionId);
                    return;
                }

                Payment payment = new Payment();
                payment.setBooking(booking);
                payment.setAmount(booking.getRemainingAmount()); // thanh toán toàn bộ nợ
                payment.setMethod(PaymentMethod.BANK_TRANSFER);
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(transactionId);
                payment.setAutoConfirmed(true);
                payment.setPaidAt(LocalDateTime.now());
                payment.setConfirmedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                // Cập nhật remainingAmount
                booking.setRemainingAmount(BigDecimal.ZERO);
                bookingRepository.save(booking);

                paymentSecurityService.markAsProcessed(transactionId);
                log.info("Webhook payment auto confirmed for booking: {}", bookingCode);
            }
        }
    }

    public List<Object> getPaymentHistory(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId).stream()
                .map(p -> (Object) p)
                .collect(Collectors.toList());
    }

    @Transactional
    public Object refundPayment(RefundRequest request, User user) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.getBookingId().toString()));

        Payment refundPayment = new Payment();
        refundPayment.setBooking(booking);
        refundPayment.setAmount(request.getAmount().negate());
        refundPayment.setMethod(PaymentMethod.BANK_TRANSFER);
        refundPayment.setStatus(PaymentStatus.REFUNDED);
        refundPayment.setRefundAmount(request.getAmount());
        refundPayment.setRefundedAt(LocalDateTime.now());
        refundPayment.setRefundReason(request.getReason());
        refundPayment.setConfirmedBy(user);
        refundPayment.setConfirmedAt(LocalDateTime.now());
        paymentRepository.save(refundPayment);

        // Hoàn tiền làm tăng remainingAmount
        booking.setRemainingAmount(booking.getRemainingAmount().add(request.getAmount()));
        bookingRepository.save(booking);

        return refundPayment;
    }
}
