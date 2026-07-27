import { foodBeverageService } from "../services/foodBeverageService.js";
import { StatusCodes } from "http-status-codes";

const getMenu = async (req, res, next) => {
    try {
        const { category, showtimeId } = req.query;
        const products = await foodBeverageService.getAllActive(category, showtimeId);
        res.status(StatusCodes.OK).json({ success: true, data: products });
    } catch (error) {
        next(error);
    }
};

const getAdminMenu = async (req, res, next) => {
    try {
        const products = await foodBeverageService.getAllForAdmin();
        res.status(StatusCodes.OK).json({ success: true, data: products });
    } catch (error) {
        next(error);
    }
};

const addProduct = async (req, res, next) => {
    try {
        const product = await foodBeverageService.createProduct(req.body);
        res.status(StatusCodes.CREATED).json({ success: true, data: product });
    } catch (error) {
        next(error);
    }
};

const updateProduct = async (req, res, next) => {
    try {
        const product = await foodBeverageService.updateProduct(req.params.id, req.body);
        res.status(StatusCodes.OK).json({ success: true, data: product });
    } catch (error) {
        next(error);
    }
};

const deleteProduct = async (req, res, next) => {
    try {
        await foodBeverageService.deleteProduct(req.params.id);
        res.status(StatusCodes.OK).json({ success: true, message: "Product deactivated successfully" });
    } catch (error) {
        next(error);
    }
};

export const foodBeverageController = {
    getMenu,
    getAdminMenu,
    addProduct,
    updateProduct,
    deleteProduct
};
