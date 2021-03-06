/*
 * Onyx Persistence API
 * Access your database via Web Services
 *
 * OpenAPI spec version: 2.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.swagger.client.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.client.model.QueryCriteria;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * QueryCriteria
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2017-12-27T19:03:42.962Z")
public class QueryCriteria {
  @SerializedName("attribute")
  private String attribute = null;

  /**
   * Query criteria operator e.x. EQUAL, NOT_EQUAL, etc...
   */
  @JsonAdapter(OperatorEnum.Adapter.class)
  public enum OperatorEnum {
    EQUAL("EQUAL"),
    
    NOT_EQUAL("NOT_EQUAL"),
    
    NOT_STARTS_WITH("NOT_STARTS_WITH"),
    
    NOT_NULL("NOT_NULL"),
    
    IS_NULL("IS_NULL"),
    
    STARTS_WITH("STARTS_WITH"),
    
    CONTAINS("CONTAINS"),
    
    NOT_CONTAINS("NOT_CONTAINS"),
    
    LIKE("LIKE"),
    
    NOT_LIKE("NOT_LIKE"),
    
    MATCHES("MATCHES"),
    
    NOT_MATCHES("NOT_MATCHES"),
    
    LESS_THAN("LESS_THAN"),
    
    GREATER_THAN("GREATER_THAN"),
    
    LESS_THAN_EQUAL("LESS_THAN_EQUAL"),
    
    GREATER_THAN_EQUAL("GREATER_THAN_EQUAL"),
    
    IN("IN"),
    
    NOT_IN("NOT_IN");

    private String value;

    OperatorEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static OperatorEnum fromValue(String text) {
      for (OperatorEnum b : OperatorEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }

    public static class Adapter extends TypeAdapter<OperatorEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final OperatorEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public OperatorEnum read(final JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        return OperatorEnum.fromValue(String.valueOf(value));
      }
    }
  }

  @SerializedName("operator")
  private OperatorEnum operator = null;

  @SerializedName("value")
  private Object value = null;

  @SerializedName("subCriteria")
  private List<QueryCriteria> subCriteria = null;

  @SerializedName("isAnd")
  private Boolean isAnd = null;

  @SerializedName("isOr")
  private Boolean isOr = null;

  @SerializedName("isNot")
  private Boolean isNot = null;

  public QueryCriteria attribute(String attribute) {
    this.attribute = attribute;
    return this;
  }

   /**
   * Attribute to check criteria for
   * @return attribute
  **/
  @ApiModelProperty(value = "Attribute to check criteria for")
  public String getAttribute() {
    return attribute;
  }

  public void setAttribute(String attribute) {
    this.attribute = attribute;
  }

  public QueryCriteria operator(OperatorEnum operator) {
    this.operator = operator;
    return this;
  }

   /**
   * Query criteria operator e.x. EQUAL, NOT_EQUAL, etc...
   * @return operator
  **/
  @ApiModelProperty(value = "Query criteria operator e.x. EQUAL, NOT_EQUAL, etc...")
  public OperatorEnum getOperator() {
    return operator;
  }

  public void setOperator(OperatorEnum operator) {
    this.operator = operator;
  }

  public QueryCriteria value(Object value) {
    this.value = value;
    return this;
  }

   /**
   * Criteria value to compare
   * @return value
  **/
  @ApiModelProperty(value = "Criteria value to compare")
  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public QueryCriteria subCriteria(List<QueryCriteria> subCriteria) {
    this.subCriteria = subCriteria;
    return this;
  }

  public QueryCriteria addSubCriteriaItem(QueryCriteria subCriteriaItem) {
    if (this.subCriteria == null) {
      this.subCriteria = new ArrayList<QueryCriteria>();
    }
    this.subCriteria.add(subCriteriaItem);
    return this;
  }

   /**
   * Sub Criteria contains and / or criteria
   * @return subCriteria
  **/
  @ApiModelProperty(value = "Sub Criteria contains and / or criteria")
  public List<QueryCriteria> getSubCriteria() {
    return subCriteria;
  }

  public void setSubCriteria(List<QueryCriteria> subCriteria) {
    this.subCriteria = subCriteria;
  }

  public QueryCriteria isAnd(Boolean isAnd) {
    this.isAnd = isAnd;
    return this;
  }

   /**
   * Is an And criteria
   * @return isAnd
  **/
  @ApiModelProperty(value = "Is an And criteria")
  public Boolean getIsAnd() {
    return isAnd;
  }

  public void setIsAnd(Boolean isAnd) {
    this.isAnd = isAnd;
  }

  public QueryCriteria isOr(Boolean isOr) {
    this.isOr = isOr;
    return this;
  }

   /**
   * Is an Or criteria
   * @return isOr
  **/
  @ApiModelProperty(value = "Is an Or criteria")
  public Boolean getIsOr() {
    return isOr;
  }

  public void setIsOr(Boolean isOr) {
    this.isOr = isOr;
  }

  public QueryCriteria isNot(Boolean isNot) {
    this.isNot = isNot;
    return this;
  }

   /**
   * Inverse of defined criteria
   * @return isNot
  **/
  @ApiModelProperty(value = "Inverse of defined criteria")
  public Boolean getIsNot() {
    return isNot;
  }

  public void setIsNot(Boolean isNot) {
    this.isNot = isNot;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QueryCriteria queryCriteria = (QueryCriteria) o;
    return Objects.equals(this.attribute, queryCriteria.attribute) &&
        Objects.equals(this.operator, queryCriteria.operator) &&
        Objects.equals(this.value, queryCriteria.value) &&
        Objects.equals(this.subCriteria, queryCriteria.subCriteria) &&
        Objects.equals(this.isAnd, queryCriteria.isAnd) &&
        Objects.equals(this.isOr, queryCriteria.isOr) &&
        Objects.equals(this.isNot, queryCriteria.isNot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attribute, operator, value, subCriteria, isAnd, isOr, isNot);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QueryCriteria {\n");
    
    sb.append("    attribute: ").append(toIndentedString(attribute)).append("\n");
    sb.append("    operator: ").append(toIndentedString(operator)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    subCriteria: ").append(toIndentedString(subCriteria)).append("\n");
    sb.append("    isAnd: ").append(toIndentedString(isAnd)).append("\n");
    sb.append("    isOr: ").append(toIndentedString(isOr)).append("\n");
    sb.append("    isNot: ").append(toIndentedString(isNot)).append("\n");
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

