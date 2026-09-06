package vn.hcmute.jpaweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@NamedQuery(
        name = "Category.findAll",
        query = "SELECT c FROM Category c ORDER BY c.categoryId ASC"
)
public class Category implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 255, message = "Tên danh mục không được vượt quá 255 ký tự")
    @Column(name = "category_name", nullable = false, unique = true, length = 255)
    private String categoryName;

    @Size(max = 500, message = "Đường dẫn ảnh không được vượt quá 500 ký tự")
    @Column(name = "images", length = 500)
    private String images;

    @Min(value = 0, message = "Trạng thái chỉ nhận giá trị 0 hoặc 1")
    @Max(value = 1, message = "Trạng thái chỉ nhận giá trị 0 hoặc 1")
    @Column(name = "status", nullable = false)
    private int status;

    @OneToMany(mappedBy = "category")
    private List<Video> videos = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    private List<Product> products = new ArrayList<>();

    public Category() {
    }

    public Category(String categoryName, String images, int status) {
        this.categoryName = categoryName;
        this.images = images;
        this.status = status;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<Video> getVideos() {
        return videos;
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos == null ? new ArrayList<>() : videos;
    }

    public Video addVideo(Video video) {
        if (video != null && !videos.contains(video)) {
            videos.add(video);
            video.setCategory(this);
        }
        return video;
    }

    public Video removeVideo(Video video) {
        if (video != null && videos.remove(video)) {
            video.setCategory(null);
        }
        return video;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products == null ? new ArrayList<>() : products;
    }

    public Product addProduct(Product product) {
        if (product != null && !products.contains(product)) {
            products.add(product);
            product.setCategory(this);
        }
        return product;
    }

    public Product removeProduct(Product product) {
        if (product != null && products.remove(product)) {
            product.setCategory(null);
        }
        return product;
    }
}
