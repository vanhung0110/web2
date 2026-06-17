package com.hotelchain.pro.staff.service;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.exception.ResourceNotFoundException;
import com.hotelchain.pro.common.exception.HotelChainException;
import com.hotelchain.pro.property.entity.Property;
import com.hotelchain.pro.property.repository.PropertyRepository;
import com.hotelchain.pro.staff.dto.*;
import com.hotelchain.pro.staff.entity.Shift;
import com.hotelchain.pro.staff.entity.Staff;
import com.hotelchain.pro.staff.repository.ShiftRepository;
import com.hotelchain.pro.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final StaffRepository staffRepository;
    private final PropertyRepository propertyRepository;

    public List<Object> listShifts(UUID propertyId, String from, String to) {
        LocalDateTime start = from != null ? LocalDateTime.parse(from) : LocalDateTime.now().minusDays(7);
        LocalDateTime end = to != null ? LocalDateTime.parse(to) : LocalDateTime.now().plusDays(7);

        List<Shift> shifts;
        if (propertyId != null) {
            shifts = shiftRepository.findByPropertyIdAndScheduledStartBetween(propertyId, start, end);
        } else {
            shifts = shiftRepository.findByScheduledStartBetween(start, end);
        }

        return shifts.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public Object createShift(CreateShiftRequest request) {
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff", request.getStaffId().toString()));

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", request.getPropertyId().toString()));

        Shift shift = new Shift();
        shift.setStaff(staff);
        shift.setProperty(property);
        shift.setScheduledStart(request.getScheduledStart());
        shift.setScheduledEnd(request.getScheduledEnd());
        shift.setType(request.getType());
        shift.setNotes(request.getNotes());
        shift.setIsOvertime(request.getIsOvertime() != null ? request.getIsOvertime() : false);

        Shift saved = shiftRepository.save(shift);
        return mapToDto(saved);
    }

    @Transactional
    public Object clockIn(UUID id, ClockInRequest request, User user) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", id.toString()));

        // Kiểm tra tài khoản khớp với nhân viên của ca làm
        if (shift.getStaff().getUser() == null || !shift.getStaff().getUser().getId().equals(user.getId())) {
            throw new HotelChainException("FORBIDDEN", "Bạn không được phân công cho ca làm việc này");
        }

        shift.setActualStart(LocalDateTime.now());
        if (request != null) {
            shift.setClockInLatitude(request.getLatitude());
            shift.setClockInLongitude(request.getLongitude());
        }

        return mapToDto(shiftRepository.save(shift));
    }

    @Transactional
    public Object clockOut(UUID id, ClockOutRequest request, User user) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", id.toString()));

        if (shift.getStaff().getUser() == null || !shift.getStaff().getUser().getId().equals(user.getId())) {
            throw new HotelChainException("FORBIDDEN", "Bạn không được phân công cho ca làm việc này");
        }

        shift.setActualEnd(LocalDateTime.now());
        if (request != null) {
            shift.setClockOutLatitude(request.getLatitude());
            shift.setClockOutLongitude(request.getLongitude());
        }

        return mapToDto(shiftRepository.save(shift));
    }

    public List<Object> getAttendanceReport(UUID propertyId, String from, String to) {
        LocalDateTime start = from != null ? LocalDateTime.parse(from) : LocalDateTime.now().minusDays(30);
        LocalDateTime end = to != null ? LocalDateTime.parse(to) : LocalDateTime.now();

        List<Shift> shifts;
        if (propertyId != null) {
            shifts = shiftRepository.findByPropertyIdAndScheduledStartBetween(propertyId, start, end);
        } else {
            shifts = shiftRepository.findByScheduledStartBetween(start, end);
        }

        // Nhóm theo nhân viên và tính toán
        Map<Staff, List<Shift>> group = shifts.stream()
                .filter(s -> s.getActualStart() != null)
                .collect(Collectors.groupingBy(Shift::getStaff));

        List<Object> reports = new ArrayList<>();
        for (Map.Entry<Staff, List<Shift>> entry : group.entrySet()) {
            Staff staff = entry.getKey();
            List<Shift> staffShifts = entry.getValue();

            double scheduledHours = 0.0;
            double actualHours = 0.0;
            long lateMinutes = 0;
            double overtimeHours = 0.0;
            int daysWorked = staffShifts.size();

            for (Shift s : staffShifts) {
                double sch = Duration.between(s.getScheduledStart(), s.getScheduledEnd()).toMinutes() / 60.0;
                scheduledHours += sch;

                if (s.getActualEnd() != null) {
                    double act = Duration.between(s.getActualStart(), s.getActualEnd()).toMinutes() / 60.0;
                    actualHours += act;

                    if (s.getIsOvertime()) {
                        overtimeHours += act;
                    }
                }

                // Đi muộn (so sánh actualStart và scheduledStart)
                if (s.getActualStart().isAfter(s.getScheduledStart())) {
                    lateMinutes += Duration.between(s.getScheduledStart(), s.getActualStart()).toMinutes();
                }
            }

            reports.add(AttendanceDto.builder()
                    .staffId(staff.getId())
                    .staffName(staff.getFullName())
                    .scheduledHours(scheduledHours)
                    .actualHours(actualHours)
                    .lateMinutes(lateMinutes)
                    .overtimeHours(overtimeHours)
                    .daysWorked(daysWorked)
                    .build());
        }

        return reports;
    }

    private Object mapToDto(Shift shift) {
        return ShiftDto.builder()
                .id(shift.getId())
                .staffId(shift.getStaff().getId())
                .staffName(shift.getStaff().getFullName())
                .propertyId(shift.getProperty().getId())
                .scheduledStart(shift.getScheduledStart())
                .scheduledEnd(shift.getScheduledEnd())
                .actualStart(shift.getActualStart())
                .actualEnd(shift.getActualEnd())
                .type(shift.getType())
                .notes(shift.getNotes())
                .isOvertime(shift.getIsOvertime())
                .clockInLatitude(shift.getClockInLatitude())
                .clockInLongitude(shift.getClockInLongitude())
                .clockOutLatitude(shift.getClockOutLatitude())
                .clockOutLongitude(shift.getClockOutLongitude())
                .build();
    }
}
