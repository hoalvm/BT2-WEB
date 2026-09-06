<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${product.productName}" /> | JPA Store</title>
    <c:url var="styleUrl" value="/assets/css/app.css" />
    <link rel="stylesheet" href="${styleUrl}">
</head>
<body>
<c:url var="homeUrl" value="/" />
<c:url var="catalogUrl" value="/product" />
<c:url var="loginUrl" value="/login" />
<c:url var="registerUrl" value="/register" />
<c:url var="logoutUrl" value="/logout" />
<c:url var="adminProductsUrl" value="/admin/products" />
<c:url var="fallbackImageUrl" value="/assets/images/default.svg" />

<header class="site-header">
    <nav class="site-nav" aria-label="Main navigation">
        <a class="brand" href="${homeUrl}"><span class="brand-dot" aria-hidden="true"></span> JPA Store</a>
        <div class="nav-links">
            <a class="nav-link" href="${homeUrl}">Home</a>
            <a class="nav-link current" href="${catalogUrl}" aria-current="page">Products</a>
            <c:choose>
                <c:when test="${not empty sessionScope.currentUser}">
                    <a class="nav-link" href="${adminProductsUrl}">Admin</a>
                    <span class="nav-user">Hi, <c:out value="${sessionScope.currentUser.username}" /></span>
                    <form class="link-button-form" method="post" action="${logoutUrl}">
                        <button class="button small secondary" type="submit">Log out</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <a class="nav-link" href="${loginUrl}">Log in</a>
                    <a class="button small primary" href="${registerUrl}">Create account</a>
                </c:otherwise>
            </c:choose>
        </div>
    </nav>
</header>

<c:set var="storedImageLower" value="${fn:toLowerCase(product.images)}" />
<c:choose>
    <c:when test="${not empty product.images and (fn:startsWith(storedImageLower, 'http://') or fn:startsWith(storedImageLower, 'https://'))}">
        <c:set var="productImageUrl" value="${product.images}" />
    </c:when>
    <c:when test="${not empty product.images and product.images ne 'default.png'}">
        <c:url var="productImageUrl" value="/image">
            <c:param name="fname" value="${product.images}" />
        </c:url>
    </c:when>
    <c:otherwise>
        <c:set var="productImageUrl" value="${fallbackImageUrl}" />
    </c:otherwise>
</c:choose>

<main class="container detail-page">
    <a class="back-link" href="${catalogUrl}"><span aria-hidden="true">←</span> All products</a>
    <article class="card product-detail">
        <div class="product-detail-image-wrap">
            <img class="product-detail-image"
                 src="${fn:escapeXml(productImageUrl)}"
                 alt="${fn:escapeXml(product.productName)}"
                 onerror="this.onerror=null;this.src='${fallbackImageUrl}';">
        </div>
        <div class="product-detail-content">
            <p class="product-category"><c:out value="${product.category.categoryName}" /></p>
            <h1><c:out value="${product.productName}" /></h1>
            <p class="product-detail-price">$<fmt:formatNumber value="${product.price}" pattern="#,##0.00" /></p>
            <span class="badge active">Active</span>
            <div class="detail-divider"></div>
            <h2>Description</h2>
            <c:choose>
                <c:when test="${empty product.description}">
                    <p class="product-description muted">No description is available for this product.</p>
                </c:when>
                <c:otherwise>
                    <p class="product-description"><c:out value="${product.description}" /></p>
                </c:otherwise>
            </c:choose>
        </div>
    </article>
</main>

<footer class="site-footer">
    <p>JPA Web Assignment 02</p>
</footer>
</body>
</html>
