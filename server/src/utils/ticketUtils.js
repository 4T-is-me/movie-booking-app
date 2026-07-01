import QRCode from 'qrcode';
import  ApiError  from '../utils/ApiError.js';
import { StatusCodes } from 'http-status-codes';
const generateTicketCode = async (bookingId) => {
    const PREFIX = 'MBD';

    const now = new Date();
    const year = now.getFullYear();
    // padStart(2, '0') giúp đảm bảo luôn có 2 chữ số, ví dụ: "7" thành "07"
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const dateString = `${year}${month}${day}`;

    // random 6 characters alphanumeric
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let randomString = ''
    for (let i = 0; i < 6; i++) {
        const randomIndex = Math.floor(Math.random() * characters.length);
        randomString += characters[randomIndex];
    }
    return `${PREFIX}-${dateString}-${randomString}`;
}

//generate QR image from ticket code
const generateQRCode = async (bookingCode) => {
    try {
        const qrCodeDataURL = await QRCode.toDataURL(bookingCode, {
            errorCorrectionLevel: 'H',  // Cao hơn để scan tốt
            margin: 2,
            width: 300,                 // width of the QR code image
            color: {
                dark: '#000000',
                light: '#FFFFFF'
            }
        });
        return qrCodeDataURL;
    } catch (error) {
        console.error('Generate QR Code Error:', error);
        throw new ApiError(StatusCodes.INTERNAL_SERVER_ERROR,'Failed to generate QR code');
    }

}
export const ticketUtils = {
    generateTicketCode,
    generateQRCode
};