<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>JPA Store</title>
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
            <a class="nav-link current" href="${homeUrl}" aria-current="page">Home</a>
            <a class="nav-link" href="${catalogUrl}">Products</a>
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

<main>
    <section class="store-hero">
        <div class="store-hero-content">
            <p class="eyebrow">Assignment 02</p>
            <h1>Simple products.<br>Clear choices.</h1>
            <p>Browse the latest technology products built on Jakarta Servlet, JSP, and JPA.</p>
            <a class="button primary" href="${catalogUrl}">Browse all products</a>
        </div>
    </section>

    <section class="container store-section" aria-labelledby="newest-products-title">
        <div class="section-heading">
            <div>
                <p class="eyebrow">Just added</p>
                <h2 id="newest-products-title">Newest products</h2>
            </div>
            <a class="text-link" href="${catalogUrl}">View all <span aria-hidden="true">→</span></a>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert error" role="alert"><c:out value="${errorMessage}" /></div>
        </c:if>

        <c:choose>
            <c:when test="${empty products}">
                <div class="card empty-state">No active products are available yet.</div>
            </c:when>
            <c:otherwise>
                <div class="product-grid">
                    <c:forEach items="${products}" var="product">
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
                        <c:url var="detailUrl" value="/product/detail">
                            <c:param name="id" value="${product.productId}" />
                        </c:url>
                        <article class="product-card">
                            <a class="product-card-link" href="${detailUrl}" aria-label="View ${fn:escapeXml(product.productName)}">
                                <div class="product-image-wrap">
                                    <img class="product-image"
                                         src="${fn:escapeXml(productImageUrl)}"
                                         alt="${fn:escapeXml(product.productName)}"
                                         loading="lazy"
                                         onerror="this.onerror=null;this.src='${fallbackImageUrl}';">
                                </div>
                                <div class="product-card-body">
                                    <p class="product-category"><c:out value="${product.category.categoryName}" /></p>
                                    <h3><c:out value="${product.productName}" /></h3>
                                    <p class="product-price">$<fmt:formatNumber value="${product.price}" pattern="#,##0.00" /></p>
                                </div>
                            </a>
                        </article>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</main>

<footer class="site-footer">
    <p>JPA Web Assignment 02</p>
</footer>
</body>
</html>
