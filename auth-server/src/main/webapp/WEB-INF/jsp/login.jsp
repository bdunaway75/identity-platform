<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>

<t:default>
    <jsp:body>
                <form:form method="post" action="/login" cssClass="form">
                    <c:set var="_csrf" value="${_csrf}"/>

                    <label for="username" class="label">Email / Username</label>
                    <input id="username" name="username" type="text" class="input" placeholder="email@domain.com" />

                    <label for="password" class="label">Password</label>
                    <input id="password" name="password" type="password" class="input" placeholder="••••••••" />

                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

                    <button type="submit" class="btn">Login</button>

                    <div class="muted text-center" style="margin-top:.4rem;">
                        Don't have an account? <a href="/signUp" style="color:var(--brand2); text-decoration:none;">Sign Up</a> &nbsp;•&nbsp;
                        Forgot password? <a href="#" style="color:var(--brand2); text-decoration:none;">Reset</a>
                    </div>

                    <div class="alerts">
                        <c:if test="${param.error != null}">
                            <div class="alert alert-danger">Invalid credentials.</div>
                        </c:if>
                        <c:if test="${param.logout != null}">
                            <div class="alert alert-success">You have been logged out.</div>
                        </c:if>
                    </div>
                </form:form>
    </jsp:body>
</t:default>
