package com.aba.abms.billing;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing-requests")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class BillingRequestController {

  private final BillingRequestRepository repository;

  public BillingRequestController(BillingRequestRepository repository) {
    this.repository = repository;
  }

  @PostMapping
  public BillingRequestDto create(@Valid @RequestBody BillingRequestDto request) {
    return BillingRequestDto.fromEntity(repository.save(request.toEntity()));
  }

  @GetMapping
  public List<BillingRequestDto> search(
      @RequestParam(required = false) String requestSource,
      @RequestParam(required = false) String requestDestination,
      @RequestParam(required = false) LocalDate requestMonth,
      @RequestParam(required = false) String engineer,
      @RequestParam(required = false) BigDecimal amount,
      @RequestParam(required = false) String remarks) {
    Specification<BillingRequest> spec = Specification.allOf(
        contains("requestSource", requestSource),
        contains("requestDestination", requestDestination),
        equalsValue("requestMonth", requestMonth),
        contains("engineer", engineer),
        equalsValue("amount", amount),
        contains("remarks", remarks));

    return repository.findAll(spec, Sort.by(Sort.Direction.ASC, "no"))
        .stream()
        .map(BillingRequestDto::fromEntity)
        .toList();
  }

  private static Specification<BillingRequest> contains(String field, String value) {
    return (root, query, builder) -> {
      if (value == null || value.isBlank()) {
        return builder.conjunction();
      }
      return builder.like(
          builder.lower(root.get(field)),
          "%" + value.trim().toLowerCase() + "%");
    };
  }

  private static <T> Specification<BillingRequest> equalsValue(String field, T value) {
    return (root, query, builder) -> {
      if (value == null) {
        return builder.conjunction();
      }
      return builder.equal(root.get(field), value);
    };
  }
}
