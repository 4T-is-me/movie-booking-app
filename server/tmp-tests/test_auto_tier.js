import mongoose from 'mongoose';
import dotenv from 'dotenv';
import { Movie } from '../src/models/movieModel.js';
import { CleanUpJobs } from '../src/jobs/cleanUpJobs.js';

dotenv.config({ path: '../.env' });

const testAutoTier = async () => {
  try {
    console.log("Connecting to MongoDB...");
    await mongoose.connect(process.env.MONGO_URI);
    console.log("Connected.");

    const now = new Date();
    
    // Movie 1: Released 5 days ago (Should be Tier 1)
    const movie1Date = new Date(now);
    movie1Date.setDate(now.getDate() - 5);
    
    // Movie 2: Released 20 days ago (Should be Tier 2)
    const movie2Date = new Date(now);
    movie2Date.setDate(now.getDate() - 20);

    // Movie 3: Released 40 days ago (Should be Tier 3)
    const movie3Date = new Date(now);
    movie3Date.setDate(now.getDate() - 40);

    // Create mock movies
    const movies = await Movie.insertMany([
      {
        movieName: "Test Movie 1 (Tier 1)",
        duration: 120,
        releaseDate: movie1Date,
        movieGenre: ["Action"],
        language: "English",
        censorship: "13+",
        director: ["Director 1"],
        actors: ["Actor 1"],
        country: "USA",
        status: "showing",
        isAutoTier: true,
        tier: 5 // Start with a random tier to ensure it updates
      },
      {
        movieName: "Test Movie 2 (Tier 2)",
        duration: 120,
        releaseDate: movie2Date,
        movieGenre: ["Comedy"],
        language: "English",
        censorship: "13+",
        director: ["Director 2"],
        actors: ["Actor 2"],
        country: "USA",
        status: "showing",
        isAutoTier: true,
        tier: 1 // Start with wrong tier
      },
      {
        movieName: "Test Movie 3 (Tier 3)",
        duration: 120,
        releaseDate: movie3Date,
        movieGenre: ["Drama"],
        language: "English",
        censorship: "13+",
        director: ["Director 3"],
        actors: ["Actor 3"],
        country: "USA",
        status: "showing",
        isAutoTier: true,
        tier: 1 // Start with wrong tier
      },
      {
        movieName: "Test Movie 4 (No Auto Tier)",
        duration: 120,
        releaseDate: movie3Date, // 40 days ago
        movieGenre: ["Drama"],
        language: "English",
        censorship: "13+",
        director: ["Director 3"],
        actors: ["Actor 3"],
        country: "USA",
        status: "showing",
        isAutoTier: false, // Auto tier disabled
        tier: 1 // Should stay 1
      }
    ]);

    const movieIds = movies.map(m => m._id);
    console.log(`Created ${movies.length} test movies.`);

    // Run the job
    console.log("Running autoUpdateMovieTiers...");
    await CleanUpJobs.autoUpdateMovieTiers();
    console.log("Job completed.");

    // Verify results
    const updatedMovies = await Movie.find({ _id: { $in: movieIds } }).sort({ movieName: 1 });
    
    console.log("--- RESULTS ---");
    updatedMovies.forEach(m => {
      console.log(`Movie: ${m.movieName} | Tier: ${m.tier} | isAutoTier: ${m.isAutoTier}`);
    });

    // Cleanup
    console.log("Cleaning up test movies...");
    await Movie.deleteMany({ _id: { $in: movieIds } });
    console.log("Done.");

    process.exit(0);
  } catch (error) {
    console.error("Error during test:", error);
    process.exit(1);
  }
};

testAutoTier();
