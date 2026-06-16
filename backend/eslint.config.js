import js from "@eslint/js";
import prettierPlugin from "eslint-plugin-prettier";
import prettierConfig from "eslint-config-prettier";
import globals from "globals";

export default [
    js.configs.recommended, // Sử dụng các rule recommended của ESLint
    {
        files: ["**/*.js"],
        languageOptions: {
            ecmaVersion: "latest",
            sourceType: "module",
            globals: {
                ...globals.node, // Định nghĩa môi trường chạy là Node.js (hiểu process, require, v.v.)
            },
        },
        plugins: {
            prettier: prettierPlugin,
        },
        rules: {
            ...prettierConfig.rules, // Tắt các rule của ESLint xung đột với Prettier
            "prettier/prettier": "error", // Báo lỗi đỏ nếu code không tuân thủ Prettier
            "no-console": "off", // Cho phép dùng console.log thoải mái khi test
            "no-unused-vars": "warn", // Cảnh báo nếu tạo biến mà không dùng
        },
    },
];