package com.chapter.bookstore.checkout;

import jakarta.validation.constraints.*;

public class DeliveryForm {
  @NotBlank
  @Size(max = 100)
  public String recipientName = "";

  @NotBlank
  @Pattern(regexp = "0[0-9]{9}")
  public String phone = "";

  @NotBlank
  @Size(max = 255)
  public String street = "";

  @NotBlank
  @Size(max = 100)
  public String ward = "";

  @NotBlank
  @Size(max = 100)
  public String district = "";

  @NotBlank
  @Size(max = 100)
  public String province = "";
}
