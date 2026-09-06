<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý danh mục</title>
    <c:url var="styleUrl" value="/assets/css/app.css" />
    <link rel="stylesheet" href="${styleUrl}">
</head>
<body>
<c:url var="homeNavUrl" value="/" />
<c:url var="productNavUrl" value="/product" />
<c:url var="categoryNavUrl" value="/admin/categories" />
<c:url var="productAdminNavUrl" value="/admin/products" />
<c:url var="logoutNavUrl" value="/logout" />
<header class="site-header">
    <nav class="site-nav" aria-label="Admin navigation">
        <a class="brand" href="${homeNavUrl}"><span class="brand-dot" aria-hidden="true"></span> JPA Store</a>
        <div class="nav-links">
            <a class="nav-link" href="${homeNavUrl}">Home</a>
            <a class="nav-link" href="${productNavUrl}">Products</a>
            <a class="nav-link current" href="${categoryNavUrl}" aria-current="page">Categories</a>
            <a class="nav-link" href="${productAdminNavUrl}">Product Admin</a>
            <c:if test="${not empty sessionScope.currentUser}">
                <span class="nav-user"><c:out value="${sessionScope.currentUser.username}" /></span>
                <form class="link-button-form" method="post" action="${logoutNavUrl}">
                    <button class="button small secondary" type="submit">Log out</button>
                </form>
            </c:if>
        </div>
    </nav>
</header>
<main class="container">
    <div class="page-heading">
        <div>
            <p class="eyebrow">JPA Web</p>
            <h1>Danh mục</h1>
        </div>
        <c:url var="addUrl" value="/admin/category/add" />
        <a class="button primary" href="${addUrl}"><span aria-hidden="true">+</span> Thêm mới</a>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert success" role="status"><c:out value="${successMessage}" /></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert error" role="alert"><c:out value="${errorMessage}" /></div>
    </c:if>

    <section class="card toolbar" aria-label="Tìm kiếm danh mục">
        <c:url var="listUrl" value="/admin/categories" />
        <form class="search-form" method="get" action="${listUrl}">
            <label class="sr-only" for="keyword">Tên danh mục</label>
            <input id="keyword" name="keyword" type="search" maxlength="255"
                   value="${fn:escapeXml(keyword)}" placeholder="Tìm danh mục">
            <button class="button primary" type="submit">Tìm</button>
            <c:if test="${not empty keyword}">
                <a class="button secondary" href="${listUrl}">Xóa</a>
            </c:if>
        </form>
        <p class="result-count">
            <strong><c:out value="${totalItems}" /></strong> danh mục
            <c:if test="${not empty keyword}">
                · “<c:out value="${keyword}" />”
            </c:if>
        </p>
    </section>

    <section class="card table-card">
        <div class="table-responsive">
            <table>
                <thead>
                <tr>
                    <th scope="col">#</th>
                    <th scope="col">Ảnh</th>
                    <th scope="col">Tên</th>
                    <th scope="col">Trạng thái</th>
                    <th scope="col"><span class="sr-only">Thao tác</span></th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${empty categories}">
                        <tr class="empty-row">
                            <td colspan="5" class="empty-state">Không có danh mục phù hợp.</td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach items="${categories}" var="category" varStatus="loop">
                            <c:set var="storedImageLower" value="${fn:toLowerCase(category.images)}" />
                            <c:choose>
                                <c:when test="${not empty category.images and (fn:startsWith(storedImageLower, 'http://') or fn:startsWith(storedImageLower, 'https://'))}">
                                    <c:set var="categoryImageUrl" value="${category.images}" />
                                </c:when>
                                <c:when test="${not empty category.images and category.images ne 'default.png'}">
                                    <c:url var="categoryImageUrl" value="/image">
                                        <c:param name="fname" value="${category.images}" />
                                    </c:url>
                                </c:when>
                                <c:otherwise>
                                    <c:url var="categoryImageUrl" value="/assets/images/default.svg" />
                                </c:otherwise>
                            </c:choose>
                            <c:url var="fallbackImageUrl" value="/assets/images/default.svg" />
                            <c:url var="editUrl" value="/admin/category/edit">
                                <c:param name="id" value="${category.categoryId}" />
                            </c:url>
                            <c:url var="deleteUrl" value="/admin/category/delete">
                                <c:param name="id" value="${category.categoryId}" />
                            </c:url>
                            <tr>
                                <td class="index-cell"><c:out value="${(currentPage - 1) * pageSize + loop.count}" /></td>
                                <td>
                                    <img class="category-thumbnail"
                                         src="${fn:escapeXml(categoryImageUrl)}"
                                         alt="${fn:escapeXml(category.categoryName)}"
                                         loading="lazy"
                                         onerror="this.onerror=null;this.src='${fallbackImageUrl}';">
                                </td>
                                <td class="category-name"><c:out value="${category.categoryName}" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${category.status == 1}">
                                            <span class="badge active">Hoạt động</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge locked">Khóa</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <div class="actions">
                                        <a class="button small secondary" href="${editUrl}">Sửa</a>
                                        <a class="button small danger" href="${deleteUrl}"
                                           onclick="return confirm('Bạn có chắc muốn xóa danh mục này?');">Xóa</a>
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

    <c:if test="${totalPages > 0}">
        <nav class="pagination" aria-label="Phân trang danh mục">
            <c:url var="previousUrl" value="/admin/categories">
                <c:param name="page" value="${currentPage - 1}" />
                <c:param name="keyword" value="${keyword}" />
            </c:url>
            <c:choose>
                <c:when test="${currentPage > 1}">
                    <a class="page-link" href="${previousUrl}" aria-label="Trang trước" title="Trang trước">←</a>
                </c:when>
                <c:otherwise>
                    <span class="page-link disabled" aria-disabled="true" aria-label="Trang trước">←</span>
                </c:otherwise>
            </c:choose>

            <c:forEach begin="1" end="${totalPages}" var="pageNumber">
                <c:url var="pageUrl" value="/admin/categories">
                    <c:param name="page" value="${pageNumber}" />
                    <c:param name="keyword" value="${keyword}" />
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

            <c:url var="nextUrl" value="/admin/categories">
                <c:param name="page" value="${currentPage + 1}" />
                <c:param name="keyword" value="${keyword}" />
            </c:url>
            <c:choose>
                <c:when test="${currentPage < totalPages}">
                    <a class="page-link" href="${nextUrl}" aria-label="Trang sau" title="Trang sau">→</a>
                </c:when>
                <c:otherwise>
                    <span class="page-link disabled" aria-disabled="true" aria-label="Trang sau">→</span>
                </c:otherwise>
            </c:choose>
        </nav>
    </c:if>
</main>
</body>
</html>
