package com.aiu.registration;

import java.util.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Keep SQL calendar dates and local teaching times independent of JSON UTC conversion. */
@RestControllerAdvice
public class ApiDateAdvice implements ResponseBodyAdvice<Object> {
  @Override
  public boolean supports(
      MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter method,
      MediaType mediaType,
      Class<? extends HttpMessageConverter<?>> converter,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    return normalize(body);
  }

  private Object normalize(Object value) {
    if (value instanceof java.sql.Date date) return date.toLocalDate().toString();
    if (value instanceof java.sql.Time time) return time.toLocalTime().toString();
    if (value instanceof java.sql.Timestamp time) return time.toLocalDateTime().toString();
    if (value instanceof Map<?, ?> map) {
      var result = new LinkedHashMap<Object, Object>();
      map.forEach((key, item) -> result.put(key, normalize(item)));
      return result;
    }
    if (value instanceof Collection<?> list) return list.stream().map(this::normalize).toList();
    return value;
  }
}
