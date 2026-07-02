import js from "@eslint/js";
import prettierPlugin from "eslint-plugin-prettier";
import prettierConfig from "eslint-config-prettier";
import globals from "globals";

export default [
    js.configs.recommended, // Use ESLint recommended rules
    {
        files: ["**/*.js"],
        languageOptions: {
            ecmaVersion: "latest",
            sourceType: "module",
            globals: {
                ...globals.node, // Define the execution environment as Node.js (understands process, require, etc.)
            },
        },
        plugins: {
            prettier: prettierPlugin,
        },
        rules: {
            ...prettierConfig.rules, // Disable ESLint rules that conflict with Prettier
            "prettier/prettier": "error", // Report errors if code does not comply with Prettier
            "no-console": "off", // Allow console.log during testing
            "no-unused-vars": "warn", // Warn if variables are declared but unused
        },
    },
];