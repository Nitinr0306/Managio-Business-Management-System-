package com.managio.modules.customers.controller;

import com.managio.modules.customers.service.CustomerCsv;
import com.managio.modules.customers.service.CustomerService;
import com.managio.modules.customers.domain.Customer.Details;
import com.managio.modules.customers.service.CustomerCsv.ImportResult;
import com.managio.modules.customers.repository.CustomerQueries.Counts;
import com.managio.modules.customers.repository.CustomerQueries.CustomerRow;
import com.managio.modules.customers.repository.CustomerQueries.Sort;
import com.managio.modules.customers.repository.CustomerQueries.View;
import com.managio.modules.customers.service.CustomerService.CustomerResponse;
import com.managio.modules.customers.service.CustomerService.Input;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import com.managio.platform.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/customers")
@RequiredArgsConstructor
class CustomerController {

    private final CustomerService customers;
    private final CustomerCsv csv;

    record CustomerRequest(
            @NotBlank(message = "Enter a name") @Size(max = 150, message = "Use 150 characters or fewer") String fullName,
            @Size(max = 20, message = "Use 20 characters or fewer") String phone,
            @Email(message = "Enter a valid email") @Size(max = 255) String email,
            @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
            @Size(max = 10) String gender,
            @Size(max = 300) String address,
            @Size(max = 150) String guardianName,
            @Size(max = 20) String guardianPhone,
            @Size(max = 4000) String notes,
            LocalDate joinedOn,
            boolean confirmDuplicate) {

        Input toInput() {
            return new Input(new Details(fullName, phone, email, dateOfBirth, gender, address, guardianName,
                    guardianPhone, notes), joinedOn, confirmDuplicate);
        }
    }

    @GetMapping
    @RequirePermission(Permission.CUSTOMERS_VIEW)
    PageResponse<CustomerRow> list(BusinessContext ctx,
                                   @RequestParam(defaultValue = "ACTIVE") View view,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) Long groupId,
                                   @RequestParam(defaultValue = "NAME") Sort sort,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "25") int size) {
        int safeSize = Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE);
        return customers.list(ctx, view, q, groupId, sort, Math.max(page, 0), safeSize);
    }

    @GetMapping("/counts")
    @RequirePermission(Permission.CUSTOMERS_VIEW)
    Counts counts(BusinessContext ctx) {
        return customers.counts(ctx);
    }

    @GetMapping("/{customerId}")
    @RequirePermission(Permission.CUSTOMERS_VIEW)
    CustomerResponse get(BusinessContext ctx, @PathVariable long customerId) {
        return customers.get(ctx, customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.CUSTOMERS_MANAGE)
    CustomerResponse create(BusinessContext ctx, @Valid @RequestBody CustomerRequest request) {
        return customers.create(ctx, request.toInput());
    }

    @PatchMapping("/{customerId}")
    @RequirePermission(Permission.CUSTOMERS_MANAGE)
    CustomerResponse update(BusinessContext ctx, @PathVariable long customerId, @Valid @RequestBody CustomerRequest request) {
        return customers.update(ctx, customerId, request.toInput());
    }

    @PostMapping("/{customerId}/archive")
    @RequirePermission(Permission.CUSTOMERS_ARCHIVE)
    CustomerResponse archive(BusinessContext ctx, @PathVariable long customerId) {
        return customers.archive(ctx, customerId);
    }

    @PostMapping("/{customerId}/restore")
    @RequirePermission(Permission.CUSTOMERS_ARCHIVE)
    CustomerResponse restore(BusinessContext ctx, @PathVariable long customerId) {
        return customers.restore(ctx, customerId);
    }

    @GetMapping("/export")
    @RequirePermission(Permission.CUSTOMERS_IMPORT)
    ResponseEntity<byte[]> export(BusinessContext ctx) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("customers.csv").build().toString())
                .body(csv.export(ctx.businessId()));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(Permission.CUSTOMERS_IMPORT)
    ImportResult importCsv(BusinessContext ctx, @RequestParam("file") MultipartFile file) {
        return csv.importFile(ctx, file);
    }
}
