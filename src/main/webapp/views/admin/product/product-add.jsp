<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Add Product | JPA Store</title>
    <c:url var="styleUrl" value="/assets/css/app.css" />
    <link rel="stylesheet" href="${styleUrl}">
</head>
<body>
<c:url var="homeUrl" value="/" />
<c:url var="catalogUrl" value="/product" />
<c:url var="productsUrl" value="/admin/products" />
<c:url var="categoriesUrl" value="/admin/categories" />
<c:url var="insertUrl" value="/admin/product/insert" />
<c:url var="logoutUrl" value="/logout" />

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

<main class="container narrow admin-page">
    <div class="page-heading">
        <div>
            <a class="back-link" href="${productsUrl}"><span aria-hidden="true">←</span> Products</a>
            <h1>Add product</h1>
        </div>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert error" role="alert"><c:out value="${errorMessage}" /></div>
    </c:if>

    <section class="card form-card">
        <form method="post" action="${insertUrl}" enctype="multipart/form-data">
            <div class="form-group">
                <label for="productName">Product name <span aria-hidden="true">*</span></label>
                <input id="productName" name="productName" type="text" maxlength="255" required
                       value="${fn:escapeXml(product.productName)}"
                       placeholder="Example: Wireless Headphones" autofocus>
            </div>

            <div class="field-row">
                <div class="form-group">
                    <label for="price">Price (USD) <span aria-hidden="true">*</span></label>
                    <input id="price" name="price" type="number" min="0" step="0.01" required
                           value="${fn:escapeXml(product.price)}" placeholder="99.99">
                </div>
                <div class="form-group">
                    <label for="categoryId">Category <span aria-hidden="true">*</span></label>
                    <select id="categoryId" name="categoryId" required>
                        <option value="">Select a category</option>
                        <c:forEach items="${categories}" var="category">
                            <option value="${fn:escapeXml(category.categoryId)}"
                                    <c:if test="${category.categoryId == categoryId}">selected</c:if>>
                                <c:out value="${category.categoryName}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>
            </div>

            <div class="form-group">
                <label for="description">Description</label>
                <textarea id="description" name="description" rows="5"
                          placeholder="Describe the product"><c:out value="${product.description}" /></textarea>
            </div>

            <div class="form-group">
                <label for="imageUrl">Image URL</label>
                <input id="imageUrl" name="imageUrl" type="url" maxlength="500"
                       value="${fn:escapeXml(imageUrl)}" placeholder="https://example.com/product.jpg">
                <small>Optional. A selected file takes priority over this URL.</small>
            </div>

            <div class="form-group">
                <label for="imagesFile">Upload image</label>
                <input id="imagesFile" name="imagesFile" type="file"
                       accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp">
                <small>JPG, PNG, or WEBP. Maximum 5 MB.</small>
            </div>

            <div class="form-group">
                <label for="status">Status <span aria-hidden="true">*</span></label>
                <select id="status" name="status" required>
                    <option value="1" <c:if test="${product.status == 1}">selected</c:if>>Active</option>
                    <option value="0" <c:if test="${product.status == 0}">selected</c:if>>Hidden</option>
                </select>
            </div>

            <div class="form-actions">
                <button class="button primary" type="submit">Add product</button>
                <a class="button secondary" href="${productsUrl}">Cancel</a>
            </div>
        </form>
    </section>
</main>
</body>
</html>
