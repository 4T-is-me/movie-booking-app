/**
 * Security Guardrails Module - Lớp bảo vệ thông tin nhạy cảm cho Chatbot AI
 * Ngăn chặn tuyệt đối việc tiết lộ thông tin hệ thống, tài khoản quản trị, mật khẩu, 
 * báo cáo tài chính, thông tin cá nhân và dữ liệu nhạy cảm khác.
 * Bổ sung PII Masking (Tự động che giấu Số điện thoại & Số thẻ tín dụng).
 */

// Danh sách từ khóa bị cấm liên quan đến tài khoản, mật khẩu, mã hóa
const credentialKeywords = [
  'mat khau',
  'password',
  'passwd',
  'pass',
  'hash',
  'token',
  'secret',
  'jwt',
  'api key',
  'auth key',
  'bearer',
  'otp',
  'ma otp'
];

// Danh sách từ khóa liên quan đến hạ tầng & CSDL / Tiêm lệnh độc hại
const infrastructureKeywords = [
  'database',
  'mongodb',
  'nosql',
  'mongo uri',
  'connection string',
  'process env',
  'env file',
  'backend code',
  'source code',
  'system log',
  'drop()',
  'drop table',
  'drop',
  'delete',
  'truncate',
  'or 1=1',
  'or 1 1',
  "or '1'='1'",
  "or '1'='1",
  "admin' or",
  "'; drop",
  '--',
  '$ne',
  '$gt',
  '$gte',
  '$lt',
  '$lte',
  '$in',
  '$nin',
  '$where',
  '$regex',
  '{"$ne"',
  '{"$gt"',
  '{"$where"',
  '"$ne":',
  '"$gt":',
  '"$where":'
];

// Danh sách từ khóa liên quan đến quản trị & dữ liệu nội bộ/tài chính
const adminDataKeywords = [
  'doanh thu',
  'doanh so',
  'bao cao tai chinh',
  'tai khoan admin',
  'list admin',
  'danh sach user',
  'danh sach khach hang',
  'danh sach nhan vien',
  'email nhan vien',
  'luong nhan vien',
  'bang luong',
  'payroll',
  'so the',
  'mat khau admin'
];

/**
 * Kiểm tra xem tin nhắn người dùng có vi phạm chính sách bảo mật thông tin hay không
 * @param {string} normalizedText - Tin nhắn người dùng đã được chuẩn hóa (không dấu, lowercase)
 * @param {string} rawText - Tin nhắn thô ban đầu từ người dùng
 * @returns {boolean} - true nếu vi phạm bảo mật, false nếu an toàn
 */
export const detectSecurityViolation = (normalizedText = '', rawText = '') => {
  const combined = (normalizedText + ' ' + (rawText || '')).toLowerCase();

  // 1. Kiểm tra từ khóa tài khoản/mật khẩu
  const hasCredentialViolation = credentialKeywords.some(kw => combined.includes(kw));
  if (hasCredentialViolation) return true;

  // 2. Kiểm tra từ khóa hạ tầng CSDL & NoSQL Injection
  const hasInfraViolation = infrastructureKeywords.some(kw => combined.includes(kw));
  if (hasInfraViolation) return true;

  // 3. Kiểm tra từ khóa quản trị & doanh thu
  const hasAdminViolation = adminDataKeywords.some(kw => combined.includes(kw));
  if (hasAdminViolation) return true;

  return false;
};

/**
 * Che (masking) các thông tin cá nhân nhạy cảm (PII) như số điện thoại và số thẻ tín dụng
 * @param {string} rawText - Tin nhắn thô ban đầu
 * @returns {string} - Tin nhắn đã được ẩn các số nhạy cảm (ví dụ: 0901234567 -> 090****567)
 */
export const maskPIIData = (rawText = '') => {
  if (!rawText || typeof rawText !== 'string') return '';

  let masked = rawText;

  // 1. Che số thẻ Visa / Mastercard (13 - 19 chữ số liên tiếp hoặc phân cách bằng space/gạch ngang)
  masked = masked.replace(/\b(\d{4})[\s\-]?(\d{4})[\s\-]?(\d{4})[\s\-]?(\d{4,7})\b/g, '$1-****-****-$4');

  // 2. Che số điện thoại Việt Nam (10 - 11 chữ số bắt đầu bằng 0)
  masked = masked.replace(/\b(0\d{2})[\s\.\-]?(\d{3,4})[\s\.\-]?(\d{3,4})\b/g, (match, p1, p2, p3) => {
    return `${p1}****${p3.substring(p3.length - 3)}`;
  });

  return masked;
};
