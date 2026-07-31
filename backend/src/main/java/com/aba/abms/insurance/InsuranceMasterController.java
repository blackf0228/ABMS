package com.aba.abms.insurance;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insurance")
@CrossOrigin(originPatterns = {
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "http://localhost:4300",
    "http://127.0.0.1:4300",
    "http://localhost:4400",
    "http://127.0.0.1:4400",
    "http://192.168.*:*"
})
public class InsuranceMasterController {

  private final EmploymentInsuranceRateRepository employmentInsuranceRateRepository;
  private final SocialInsurancePremiumAmountRepository socialInsurancePremiumAmountRepository;

  public InsuranceMasterController(
      EmploymentInsuranceRateRepository employmentInsuranceRateRepository,
      SocialInsurancePremiumAmountRepository socialInsurancePremiumAmountRepository) {
    this.employmentInsuranceRateRepository = employmentInsuranceRateRepository;
    this.socialInsurancePremiumAmountRepository = socialInsurancePremiumAmountRepository;
  }

  @GetMapping("/employment-rates")
  public List<EmploymentInsuranceRate> employmentRates() {
    return employmentInsuranceRateRepository.findAll(Sort.by("no"));
  }

  @GetMapping("/social-premiums")
  public List<SocialInsurancePremiumAmount> socialPremiums() {
    return socialInsurancePremiumAmountRepository.findAll(Sort.by("grade"));
  }
}
