package vn.hcmute.jpaweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "videos")
@NamedQuery(name = "Video.findAll", query = "SELECT v FROM Video v ORDER BY v.videoId ASC")
public class Video implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @NotBlank(message = "Mã video không được để trống")
    @Size(max = 100, message = "Mã video không được vượt quá 100 ký tự")
    @Column(name = "video_id", length = 100)
    private String videoId;

    @Min(value = 0, message = "Active chỉ nhận giá trị 0 hoặc 1")
    @Max(value = 1, message = "Active chỉ nhận giá trị 0 hoặc 1")
    @Column(name = "active", nullable = false)
    private int active;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(max = 500, message = "Đường dẫn poster không được vượt quá 500 ký tự")
    @Column(name = "poster", length = 500)
    private String poster;

    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    @Column(name = "title", length = 255)
    private String title;

    @Min(value = 0, message = "Lượt xem không được là số âm")
    @Column(name = "views", nullable = false)
    private int views;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    public Video() {
    }

    public Video(String videoId, String title, Category category) {
        this.videoId = videoId;
        this.title = title;
        this.category = category;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
