/**
 * Simple method to Convert a String to Slug
 * For more reference details, visit: https://byby.dev/js-slugify-string
 */
export const slugify = (val) => {
    if (!val) return ''
    return String(val)
        .normalize('NFKD') // split accented characters into their base characters and diacritical marks
        .replace(/[\u0300-\u036f]/g, '') // remove all the accents, which happen to be all in the \u03xx UNICODE block.
        .trim() // trim leading or trailing whitespace
        .toLowerCase() // convert to lowercase
        .replace(/[^a-z0-9 -]/g, '') // remove non-alphanumeric characters
        .replace(/\s+/g, '-') // replace spaces with hyphens
        .replace(/-+/g, '-') // remove consecutive hyphens
}

export const formatDate = (date, includeTime = false) => {
    if (!date) return '';
    
    const d = new Date(date);
    // avoid NaN/NaN/NaN error in both cases
    if (isNaN(d.getTime())) {
        return '';
    }
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();

    // if including both hour and minute
    if (includeTime) {
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes} - ${day}/${month}/${year}`;
    }
    
    // default return date
    return `${day}/${month}/${year}`;
};