<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>

<t:default>
  <jsp:body>
        <form:form method="post" action="/signUp" modelAttribute="registerDto" cssClass="form">
          <c:set var="_csrf" value="${_csrf}"/>

          <label for="username" class="label">Email</label>
          <form:input path="email" id="username" name="username" type="text" class="input" placeholder="email@domain.com" />

          <label for="password" class="label">Password</label>
          <form:input path="password" id="password" name="password" type="password" class="input" placeholder="••••••••" />

          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

          <button type="submit" class="btn">Sign Up</button>

          <div class="alerts">
            <c:if test="${param.error != null}">
              <div class="alert alert-danger">Invalid credentials.</div>
            </c:if>
              <spring:bind path="registerDto.email">
                  <div class="alert alert-danger">${status.errorMessage}</div>
              </spring:bind>          </div>
        </form:form>
  </jsp:body>
</t:default>
