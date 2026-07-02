import mongoose from 'mongoose';

const movieSchema = new mongoose.Schema({
    movieName: { type: String, required: true, trim: true },
    duration: { type: Number, required: true }, // Thời lượng (phút)
    releaseDate: { type: Date, required: true }, // Ngày công chiếu
    endDate: { type: Date },                      // Ngày kết thúc chiếu
    movieGenre: { type: [String], required: true }, // Mảng các thể loại phim
    language: { type: String, required: true },
    censorship: { type: String, required: true }, // Độ tuổi (ví dụ: 13T)
    description: { type: String }, // Mô tả nội dung phim
    director: { type: [String], required: true }, // Mảng tên các đạo diễn
    actors: { type: [String], required: true }, // Mảng tên các diễn viên
    country: { type: String, required: true },
    status: { type: String, default: 'coming_soon' }, // Trạng thái phim (ended, showing,...)
    poster: { type: String }, // Link ảnh poster phim
    rating: { type: Number, default: 0 },
    totalReviews: { type: Number, default: 0 },
    totalStarts: { type: Number, default: 0 },
    updatedAt: { type: Date, default: Date.now },
    isDeleted: { type: Boolean, default: false },
    deletedAt: { type: Date, default: null }
}, {
    versionKey: false,
    collection: 'movies', // Khớp với tên collection trong lỗi cũ của bạn
    timestamps: true // Tự động quản lý createdAt và updatedAt dưới dạng ISODate
});

// Index tối ưu truy vấn lọc phim theo khoảng ngày chiếu (now_showing / coming_soon)
movieSchema.index({ releaseDate: 1, endDate: 1 });
// Index để lọc phim chưa xóa và theo ngày công chiếu
movieSchema.index({isDeleted: 1, releaseDate: 1}); 
export const Movie = mongoose.model('Movie', movieSchema);
