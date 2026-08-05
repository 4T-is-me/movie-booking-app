import { FoodBeverage } from "../models/foodBeverageModel.js";
import { Inventory } from "../models/inventoryModel.js";
import { Showtime } from "../models/showTimeModel.js";
import { Cinema } from "../models/cinemaModel.js";
import { StatusCodes } from "http-status-codes";
import ApiError from "../utils/ApiError.js";

const getAllActive = async (category, showtimeId) => {
    const query = { isActive: true };
    if (category) {
        query.category = category;
    }

    const products = await FoodBeverage.find(query)
        .select("-createdAt -updatedAt")
        .sort({ category: 1, price: 1 })
        .lean();

    if (showtimeId) {
        const showtime = await Showtime.findById(showtimeId);
        if (showtime) {
            const cinemaId = showtime.cinemaId;
            const inventory = await Inventory.find({ cinemaId }).lean();
            const inventoryMap = {};
            inventory.forEach(inv => {
                inventoryMap[inv.itemCode] = inv.quantity;
            });

            products.forEach(product => {
                let maxPossible = 999;
                if (product.inventoryDeductions && product.inventoryDeductions.length > 0) {
                    product.inventoryDeductions.forEach(deduction => {
                        const stock = inventoryMap[deduction.itemCode] || 0;
                        const possible = Math.floor(stock / deduction.quantity);
                        if (possible < maxPossible) maxPossible = possible;
                    });
                }
                product.stock = maxPossible;
                delete product.inventoryDeductions;
            });
        }
    } else {
        products.forEach(product => {
            delete product.inventoryDeductions;
            product.stock = 999;
        });
    }

    return products;
};

const getAllForAdmin = async () => {
    return await FoodBeverage.find({}).sort({ category: 1, price: 1 });
};

const createProduct = async (data) => {
    const product = await FoodBeverage.create(data);

    if (product.type === 'single') {
        const cinemas = await Cinema.find({});

        let itemCode = (product.inventoryDeductions && product.inventoryDeductions.length > 0)
            ? product.inventoryDeductions[0].itemCode
            : product.name.toUpperCase().replace(/\s+/g, '_');

        if (!product.inventoryDeductions || product.inventoryDeductions.length === 0) {
            product.inventoryDeductions = [{ itemCode, quantity: 1 }];
            await product.save();
        }

        for (const cinema of cinemas) {
            await Inventory.findOneAndUpdate(
                { cinemaId: cinema._id, itemCode: itemCode },
                {
                    $set: { itemName: product.name },
                    $setOnInsert: {
                        quantity: 0,
                        lowStockAlert: 15
                    }
                },
                { upsert: true }
            );
        }
    }
    return product;
};

const updateProduct = async (id, data) => {
    const updatedProduct = await FoodBeverage.findByIdAndUpdate(
        id,
        { $set: data },
        { returnDocument: 'after', runValidators: true }
    );

    if (!updatedProduct) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Product not found");
    }

    if (updatedProduct.type === 'single' && updatedProduct.isActive) {
        const cinemas = await Cinema.find({});
        const itemCode = (updatedProduct.inventoryDeductions && updatedProduct.inventoryDeductions.length > 0)
            ? updatedProduct.inventoryDeductions[0].itemCode
            : updatedProduct.name.toUpperCase().replace(/\s+/g, '_');

        for (const cinema of cinemas) {
            await Inventory.findOneAndUpdate(
                { cinemaId: cinema._id, itemCode: itemCode },
                {
                    $set: { itemName: updatedProduct.name },
                    $setOnInsert: {
                        quantity: 0,
                        lowStockAlert: 15
                    }
                },
                { upsert: true }
            );
        }
    }

    return updatedProduct;
};

const deleteProduct = async (id) => {
    const product = await FoodBeverage.findByIdAndUpdate(
        id,
        { $set: { isActive: false } },
        { returnDocument: 'after' }
    );
    if (!product) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Product not found");
    }
    return product;
};

export const foodBeverageService = {
    getAllActive,
    getAllForAdmin,
    createProduct,
    updateProduct,
    deleteProduct
};
