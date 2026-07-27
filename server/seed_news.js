import mongoose from "mongoose";
import dotenv from "dotenv";
import { News } from "./src/models/newsModel.js";
import { Movie } from "./src/models/movieModel.js";
dotenv.config();

const run = async () => {
    try {
        await mongoose.connect(process.env.MONGODB_URI || process.env.MONGO_URI);
        console.log("Connected to MongoDB!");

        // Fetch some movies
        const movies = await Movie.find({ isDeleted: false }).limit(3);
        if (movies.length === 0) {
            console.log("No movies found to tag in news. Please make sure movies exist first.");
            return;
        }

        // Clean existing news
        await News.deleteMany({});
        console.log("Cleared existing news articles.");

        const seedArticles = [
            {
                title: "Avatar 3 release date announced",
                content: "Director James Cameron has officially announced that Avatar 3 is on schedule for its worldwide release. The production has wrapped up the principal photography in New Zealand. The film will introduce the 'Ash People', a new clan of Na'vi who represent a more volcanic and aggressive side of Pandora.",
                image: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500",
                movieId: movies[0]._id,
                publishedAt: new Date()
            },
            {
                title: "Behind the scenes: The cinematography secrets",
                content: "In this exclusive behind-the-scenes breakdown, we look at the groundbreaking camera work and lighting setups used to capture the visual aesthetic of our featured movies. The crew discusses the challenges of shooting in high frame rates and implementing real-time virtual production tools.",
                image: "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=500",
                movieId: movies[1] ? movies[1]._id : movies[0]._id,
                publishedAt: new Date(Date.now() - 24 * 60 * 60 * 1000)
            },
            {
                title: "Why IMAX is the ultimate way to watch blockbusters",
                content: "IMAX screen formats offer 26% more picture than standard theaters. With custom-built digital speakers and lasers projection systems, IMAX delivers a movie-going experience like no other. Discover the list of upcoming titles releasing in the IMAX format this season.",
                image: "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=500",
                movieId: movies[2] ? movies[2]._id : movies[0]._id,
                publishedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000)
            }
        ];

        await News.insertMany(seedArticles);
        console.log("Successfully seeded 3 movie news articles!");

    } catch (e) {
        console.error(e);
    } finally {
        await mongoose.disconnect();
        console.log("Disconnected.");
    }
}
run();
