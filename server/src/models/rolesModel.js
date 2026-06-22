import mongoose from "mongoose";

const rolesSchema = new mongoose.Schema({
    ROLE_ID: {type: Number, required: true, unique: true},
    ROLE_NAME: {type: String, required: true},
}, { versionKey: false, collection: 'ROLES' }); 

export const roles = mongoose.model('ROLES', rolesSchema);
