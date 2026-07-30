import natural from 'natural';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { trainingData } from '../data/trainingData.js';
import { loadMLClassifierModel } from '../services/intentClassifier.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const MODEL_PATH = path.resolve(__dirname, '../model.json');

const trainModel = async () => {
  console.log(`🚀 Bắt đầu huấn luyện AI với ${trainingData.length} mẫu dữ liệu...`);

  const classifier = new natural.BayesClassifier();
  trainingData.forEach(sample => {
    classifier.addDocument(sample.text, sample.intent);
  });
  
  classifier.train();

  await new Promise((resolve, reject) => {
    classifier.save(MODEL_PATH, err => {
      if (err) return reject(err);
      resolve();
    });
  });

  // Nạp thử Model mới vào service để kiểm tra độ toàn vẹn
  await loadMLClassifierModel();

  console.log(`✅ Huấn luyện hoàn tất! Mô hình mới đã được ghi đè thành công vào model.json.`);
};

trainModel().catch(err => {
  console.error("❌ Lỗi trong quá trình huấn luyện:", err);
  process.exit(1);
});
