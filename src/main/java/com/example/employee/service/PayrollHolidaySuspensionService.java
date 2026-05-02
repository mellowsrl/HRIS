package com.example.employee.service;

import com.example.employee.model.Holiday;
import com.example.employee.model.OfficialEmployee;
import com.example.employee.model.Suspension;
import com.example.employee.repository.HolidayRepository;
import com.example.employee.repository.SuspensionRepository;
import com.example.employee.util.PayrollDoleMath;
import com.example.employee.util.ShiftTimeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Computes values passed into {@code SP_ProcessRegularPayroll} ({@code payroll.sql}):
 * {@code p_holiday_pay} from the {@code holidays} calendar, and suspension impact as
 * negative {@code p_adjustment} (unpaid time from suspension start through shift end).
 * Rates mirror the SP: monthly {@code admin_pay} → daily (×12/313) / 8 hourly; else {@code hourly_rate}.
 */
@Service
public class PayrollHolidaySuspensionService {

    private static final double MAX_UNPAID_HOURS = 8.0;

    @Autowired
    private HolidayRepository holidayRepository;
    @Autowired
    private SuspensionRepository suspensionRepository;

    public record PeriodContext(List<Holiday> holidays, List<Suspension> suspensions) {}

    public record PayrollExtras(double holidayPay, double adjustmentForSp) {}

    public PeriodContext loadPeriodContext(LocalDate periodStart, LocalDate periodEnd) {
        List<Holiday> holidays = holidayRepository.findByDateBetweenOrderByDateAsc(periodStart, periodEnd);
        List<Suspension> suspensions = suspensionRepository.findByDateBetweenOrderByDateAsc(periodStart, periodEnd);
        return new PeriodContext(holidays, suspensions);
    }

    public PayrollExtras computeForEmployee(OfficialEmployee emp, PeriodContext ctx) {
        if (emp == null || emp.getId() == null || ctx == null) {
            return new PayrollExtras(0.0, 0.0);
        }
        Set<LocalDate> holidayDates = new HashSet<>();
        for (Holiday h : ctx.holidays()) {
            if (h.getDate() != null) {
                holidayDates.add(h.getDate());
            }
        }

        double daily = dailyRate(emp);
        double hourly = hourlyRate(emp);
        if (daily <= 0 && hourly <= 0) {
            return new PayrollExtras(0.0, 0.0);
        }
        if (daily <= 0) {
            daily = hourly * 8.0;
        }
        if (hourly <= 0) {
            hourly = daily / 8.0;
        }

        long distinctHolidayDays = ctx.holidays().stream()
            .map(Holiday::getDate)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        double holidayPay = round2(distinctHolidayDays * daily);

        double suspensionDeduction = 0.0;
        for (Suspension s : ctx.suspensions()) {
            if (s.getDate() == null) {
                continue;
            }
            if (s.getEmployeeId() != null && !s.getEmployeeId().equals(emp.getId())) {
                continue;
            }
            if (holidayDates.contains(s.getDate())) {
                continue;
            }
            suspensionDeduction += suspensionUnpaidPeso(emp, s, hourly);
        }

        suspensionDeduction = round2(suspensionDeduction);
        // Negative adjustment reduces total_earnings in SP (same as payroll.sql p_adjustment).
        double adjustmentForSp = suspensionDeduction > 0 ? -suspensionDeduction : 0.0;
        return new PayrollExtras(holidayPay, adjustmentForSp);
    }

    /**
     * Daily rate aligned with {@code SP_ProcessRegularPayroll}: (admin_pay×12/313) when monthly &gt; 0.
     */
    public double dailyRate(OfficialEmployee emp) {
        Double monthly = emp.getAdminPay();
        if (monthly != null && monthly > 0) {
            return round2(PayrollDoleMath.dailyFromMonthlyBasic(monthly));
        }
        Double hr = emp.getHourlyRate();
        if (hr != null && hr > 0) {
            return round2(hr * 8.0);
        }
        return 0.0;
    }

    public double hourlyRate(OfficialEmployee emp) {
        Double monthly = emp.getAdminPay();
        if (monthly != null && monthly > 0) {
            double daily = PayrollDoleMath.dailyFromMonthlyBasic(monthly);
            return round2(daily / 8.0);
        }
        Double hr = emp.getHourlyRate();
        if (hr != null && hr > 0) {
            return round2(hr);
        }
        return 0.0;
    }

    private double suspensionUnpaidPeso(OfficialEmployee emp, Suspension s, double hourly) {
        if (hourly <= 0) {
            return 0.0;
        }
        LocalTime[] bounds = ShiftTimeParser.parseShiftBounds(emp.getExpectedShift());
        LocalTime shiftStart = bounds[0];
        LocalTime shiftEnd = bounds[1];

        double unpaidHours;
        if (s.getStartTime() == null) {
            unpaidHours = scheduledWorkHours(shiftStart, shiftEnd);
        } else {
            LocalTime susp = s.getStartTime();
            if (!susp.isBefore(shiftStart)) {
                // Unpaid wall time from suspension through shift end (capped).
                long minutes = ChronoUnit.MINUTES.between(susp, shiftEnd);
                if (minutes < 0) {
                    minutes = 0;
                }
                unpaidHours = Math.min(MAX_UNPAID_HOURS, minutes / 60.0);
            } else {
                // Suspension time before shift — treat as full scheduled unpaid block from shift start.
                unpaidHours = scheduledWorkHours(shiftStart, shiftEnd);
            }
        }
        return unpaidHours * hourly;
    }

    /**
     * Nominal paid workday length in hours from shift window (9h span minus 1h lunch when span ≥ 5.5h),
     * matching the rough TCMS import convention in {@code AdmissionService}.
     */
    private static double scheduledWorkHours(LocalTime shiftStart, LocalTime shiftEnd) {
        long spanMin = ChronoUnit.MINUTES.between(shiftStart, shiftEnd);
        if (spanMin <= 0) {
            return MAX_UNPAID_HOURS;
        }
        double spanH = spanMin / 60.0;
        double lunch = spanH >= 5.5 ? 1.0 : 0.0;
        double work = spanH - lunch;
        return Math.min(MAX_UNPAID_HOURS, Math.max(0.0, work));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
