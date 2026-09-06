<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Products | JPA Store</title>
    <c:url var="styleUrl" value="/assets/css/app.css" />
    <link rel="stylesheet" href="${styleUrl}">
</head>
<body>
<c:url var="homeUrl" value="/" />
<c:url var="catalogUrl" value="/product" />
<c:url var="productsUrl" value="/admin/products" />
<c:url var="categoriesUrl" value="/admin/categories" />
<c:url var="addUrl" value="/admin/product/add" />
<c:url var="logoutUrl" value="/logout" />
<c:url var="fallbackImageUrl" value="/assets/images/default.svg" />

<header class="site-header">
    <nav class="site-nav" aria-label="Admin navigation">
        <a class="brand" href="${homeUrl}"><span class="brand-dot" aria-hidden="true"></span> JPA Store</a>
        <div class="nav-links">
            <a class="nav-link" href="${catalogUrl}">Store</a>
            <a class="nav-link current" href="${productsUrl}" aria-current="page">Products</a>
            <a class="nav-link" href="${categoriesUrl}">Categories</a>
            <c:if test="${not empty sessionScope.currentUser}">
                <span class="nav-user"><c:out value="${sessionScope.currentUser.username}" /></span>
                <form class="link-button-form" method="post" action="${logoutUrl}">
                    <button class="button small secondary" type="submit">Log out</button>
                </form>
            </c:if>
        </div>
    </nav>
</header>

<main class="container admin-page">
    <div class="page-heading">
        <div>
            <p class="eyebrow">Administration</p>
            <h1>Products</h1>
            <p class="muted"><c:out value="${totalItems}" /> products in the catalog</p>
        </div>
        <a class="button primary" href="${addUrl}"><span aria-hidden="true">+</span> Add product</a>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert success" role="status"><c:out value="${successMessage}" /></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert error" role="alert"><c:out value="${errorMessage}" /></div>
    </c:if>

    <section class="card table-card product-table-card">
        <div class="table-responsive">
            <table class="product-table">
                <thead>
                <tr>
                    <th scope="col">#</th>
                    <th scope="col">Image</th>
                    <th scope="col">Product</th>
                    <th scope="col">Category</th>
                    <th scope="col">Price</th>
                    <th scope="col">Status</th>
                    <th scope="col"><span class="sr-only">Actions</span></th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${empty products}">
                        <tr class="empty-row">
                            <td colspan="7" class="empty-state">No products have been added yet.</td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach items="${products}" var="product" varStatus="loop">
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
                            <c:url var="editUrl" value="/admin/product/edit">
                                <c:param name="id" value="${product.productId}" />
                            </c:url>
                            <c:url var="deleteUrl" value="/admin/product/delete" />
                            <tr>
                                <td class="index-cell"><c:out value="${loop.count}" /></td>
                                <td>
                                    <img class="product-thumbnail"
                                         src="${fn:escapeXml(productImageUrl)}"
                                         alt="${fn:escapeXml(product.productName)}"
                                         loading="lazy"
                                         onerror="this.onerror=null;this.src='${fallbackImageUrl}';">
                                </td>
                                <td class="product-name"><c:out value="${product.productName}" /></td>
                                <td><c:out value="${product.category.categoryName}" /></td>
                                <td class="price-cell">$<fmt:formatNumber value="${product.price}" pattern="#,##0.00" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${product.status == 1}">
                                            <span class="badge active">Active</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge locked">Hidden</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <div class="actions actions-nowrap">
                                        <a class="button small secondary" href="${editUrl}">Edit</a>
                                        <form class="inline-form" method="post" action="${deleteUrl}"
                                              onsubmit="return confirm('Delete this product?');">
                                            <input type="hidden" name="id" value="${fn:escapeXml(product.productId)}">
                                            <button class="button small danger" type="submit">Delete</button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
                </tbody>
            </table>
        </div>
    </section>
</main>
</body>
</html>
