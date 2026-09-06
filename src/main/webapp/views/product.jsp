<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Products | JPA Store</title>
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

<main class="container catalog-page">
    <div class="page-heading catalog-heading">
        <div>
            <p class="eyebrow">Catalog</p>
            <h1>All products</h1>
            <p class="muted"><c:out value="${totalItems}" /> active products</p>
        </div>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert error" role="alert"><c:out value="${errorMessage}" /></div>
    </c:if>

    <c:choose>
        <c:when test="${empty products}">
            <div class="card empty-state">No active products are available yet.</div>
        </c:when>
        <c:otherwise>
            <div class="product-grid product-grid-catalog">
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
                                <h2><c:out value="${product.productName}" /></h2>
                                <p class="product-price">$<fmt:formatNumber value="${product.price}" pattern="#,##0.00" /></p>
                            </div>
                        </a>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${totalPages > 0}">
        <nav class="pagination" aria-label="Product pages">
            <c:url var="previousUrl" value="/product">
                <c:param name="page" value="${currentPage - 1}" />
            </c:url>
            <c:choose>
                <c:when test="${currentPage > 1}">
                    <a class="page-link page-link-wide" href="${previousUrl}">Previous</a>
                </c:when>
                <c:otherwise>
                    <span class="page-link page-link-wide disabled" aria-disabled="true">Previous</span>
                </c:otherwise>
            </c:choose>

            <c:forEach begin="1" end="${totalPages}" var="pageNumber">
                <c:url var="pageUrl" value="/product">
                    <c:param name="page" value="${pageNumber}" />
                </c:url>
                <c:choose>
                    <c:when test="${pageNumber == currentPage}">
                        <span class="page-link current" aria-current="page"><c:out value="${pageNumber}" /></span>
                    </c:when>
                    <c:otherwise>
                        <a class="page-link" href="${pageUrl}"><c:out value="${pageNumber}" /></a>
                    </c:otherwise>
                </c:choose>
            </c:forEach>

            <c:url var="nextUrl" value="/product">
                <c:param name="page" value="${currentPage + 1}" />
            </c:url>
            <c:choose>
                <c:when test="${currentPage < totalPages}">
                    <a class="page-link page-link-wide" href="${nextUrl}">Next</a>
                </c:when>
                <c:otherwise>
                    <span class="page-link page-link-wide disabled" aria-disabled="true">Next</span>
                </c:otherwise>
            </c:choose>
        </nav>
    </c:if>
</main>

<footer class="site-footer">
    <p>JPA Web Assignment 02</p>
</footer>
</body>
</html>
