package com.nitin.saas.staff.repository;

import com.nitin.saas.staff.entity.StaffSalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffSalaryPaymentRepository extends JpaRepository<StaffSalaryPayment, Long> {

    Optional<StaffSalaryPayment> findByStaffIdAndSalaryMonth(Long staffId, LocalDate salaryMonth);

    @Query("SELECT sp FROM StaffSalaryPayment sp JOIN Staff s ON s.id = sp.staffId " +
            "WHERE s.businessId = :businessId AND s.deletedAt IS NULL AND sp.salaryMonth = :salaryMonth " +
            "ORDER BY s.createdAt DESC")
    List<StaffSalaryPayment> findByBusinessIdAndSalaryMonth(@Param("businessId") Long businessId,
                                                            @Param("salaryMonth") LocalDate salaryMonth);

    @Query("SELECT sp FROM StaffSalaryPayment sp JOIN Staff s ON s.id = sp.staffId " +
            "WHERE s.businessId = :businessId AND s.deletedAt IS NULL " +
            "AND sp.paymentStatus = com.nitin.saas.staff.entity.StaffSalaryPayment$PaymentStatus.UNPAID " +
            "AND (:salaryMonth IS NULL OR sp.salaryMonth = :salaryMonth) " +
            "ORDER BY sp.pendingAmount DESC")
    List<StaffSalaryPayment> findUnpaidByBusiness(@Param("businessId") Long businessId,
                                                  @Param("salaryMonth") LocalDate salaryMonth);

    @Query("SELECT sp FROM StaffSalaryPayment sp JOIN Staff s ON s.id = sp.staffId " +
            "WHERE s.businessId = :businessId AND s.deletedAt IS NULL ORDER BY sp.salaryMonth DESC")
    List<StaffSalaryPayment> findByBusinessId(@Param("businessId") Long businessId);
}
