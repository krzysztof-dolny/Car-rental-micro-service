/*
 * Car mircro service
 * Micro service to book a car
 *
 * OpenAPI spec version: 1.0.0
 * Contact: supportm@bp.org
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package org.bp.carrental.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.OffsetDateTime;
/**
 * Car
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2024-11-30T23:10:03.457+01:00[Europe/Belgrade]")
public class Car {
  @JsonProperty("brand")
  private String brand = null;

  @JsonProperty("model")
  private String model = null;

  @JsonProperty("country")
  private String country = null;

  @JsonProperty("city")
  private String city = null;

  @JsonProperty("checkIn")
  private OffsetDateTime checkIn = null;

  @JsonProperty("checkOut")
  private OffsetDateTime checkOut = null;

  public Car brand(String brand) {
    this.brand = brand;
    return this;
  }

   /**
   * Get brand
   * @return brand
  **/

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public Car model(String model) {
    this.model = model;
    return this;
  }

   /**
   * Get model
   * @return model
  **/

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public Car country(String country) {
    this.country = country;
    return this;
  }

   /**
   * Get country
   * @return country
  **/

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public Car city(String city) {
    this.city = city;
    return this;
  }

   /**
   * Get city
   * @return city
  **/

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public Car checkIn(OffsetDateTime checkIn) {
    this.checkIn = checkIn;
    return this;
  }

   /**
   * Get checkIn
   * @return checkIn
  **/

  public OffsetDateTime getCheckIn() {
    return checkIn;
  }

  public void setCheckIn(OffsetDateTime checkIn) {
    this.checkIn = checkIn;
  }

  public Car checkOut(OffsetDateTime checkOut) {
    this.checkOut = checkOut;
    return this;
  }

   /**
   * Get checkOut
   * @return checkOut
  **/

  public OffsetDateTime getCheckOut() {
    return checkOut;
  }

  public void setCheckOut(OffsetDateTime checkOut) {
    this.checkOut = checkOut;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Car car = (Car) o;
    return Objects.equals(this.brand, car.brand) &&
        Objects.equals(this.model, car.model) &&
        Objects.equals(this.country, car.country) &&
        Objects.equals(this.city, car.city) &&
        Objects.equals(this.checkIn, car.checkIn) &&
        Objects.equals(this.checkOut, car.checkOut);
  }

  @Override
  public int hashCode() {
    return Objects.hash(brand, model, country, city, checkIn, checkOut);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Car {\n");
    
    sb.append("    brand: ").append(toIndentedString(brand)).append("\n");
    sb.append("    model: ").append(toIndentedString(model)).append("\n");
    sb.append("    country: ").append(toIndentedString(country)).append("\n");
    sb.append("    city: ").append(toIndentedString(city)).append("\n");
    sb.append("    checkIn: ").append(toIndentedString(checkIn)).append("\n");
    sb.append("    checkOut: ").append(toIndentedString(checkOut)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
