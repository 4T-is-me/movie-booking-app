import mongoose from "mongoose";

const cinemaSchema = new mongoose.Schema({
    cinemaName: {type : String, required: true },
    address: {type: String, required: true },
    city: {type: String, required: true },
    hotline: {type: String, required: true },
    image: {type: String},
    status: {type: String, default: 'active'} // active, inactive
}, {versionKey: false, collection: 'cinemas'});

export const Cinema = mongoose.model('Cinema', cinemaSchema);