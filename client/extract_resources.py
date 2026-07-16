import os
import re

LAYOUT_DIR = r"d:\movie-booking-app\client\app\src\main\res\layout"
STRINGS_FILE = r"d:\movie-booking-app\client\app\src\main\res\values\strings.xml"
COLORS_FILE = r"d:\movie-booking-app\client\app\src\main\res\values\colors.xml"

# Regular expressions for hardcoded values
string_pattern = re.compile(r'(android:text|app:title)="([^@\?][^"]*)"')
color_pattern = re.compile(r'(android:textColor|android:background|app:backgroundTint|app:tint|app:strokeColor|app:cardBackgroundColor|android:buttonTint)="(#([0-9a-fA-F]{3,8}))"')

def get_string_name(value, existing_names):
    # create a safe name
    name = re.sub(r'[^a-zA-Z0-9]', '_', value.strip().lower())
    name = re.sub(r'_+', '_', name).strip('_')
    if not name:
        name = "str_auto"
    if len(name) > 30:
        name = name[:30].strip('_')
    
    base_name = f"auto_str_{name}"
    final_name = base_name
    count = 1
    while final_name in existing_names:
        final_name = f"{base_name}_{count}"
        count += 1
    return final_name

def get_color_name(hex_val, existing_colors):
    hex_upper = hex_val.upper()
    if hex_upper in existing_colors:
        return existing_colors[hex_upper]
    
    name = f"auto_color_{hex_upper.replace('#', '')}"
    existing_colors[hex_upper] = name
    return name

def main():
    existing_string_names = set()
    existing_colors_map = {} # hex -> name
    
    extracted_strings = {} # name -> value
    extracted_colors = {} # name -> hex

    count_files_changed = 0

    if not os.path.exists(LAYOUT_DIR):
        print(f"Error: {LAYOUT_DIR} does not exist.")
        return

    for filename in os.listdir(LAYOUT_DIR):
        if not filename.endswith(".xml"):
            continue
            
        filepath = os.path.join(LAYOUT_DIR, filename)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            
        original_content = content
        
        # 1. Replace strings
        def str_replacer(match):
            attr = match.group(1)
            val = match.group(2)
            name = get_string_name(val, existing_string_names)
            existing_string_names.add(name)
            extracted_strings[name] = val
            return f'{attr}="@string/{name}"'
            
        content = string_pattern.sub(str_replacer, content)
        
        # 2. Replace colors
        def col_replacer(match):
            attr = match.group(1)
            hex_val = match.group(2)
            name = get_color_name(hex_val, existing_colors_map)
            extracted_colors[name] = hex_val
            return f'{attr}="@color/{name}"'
            
        content = color_pattern.sub(col_replacer, content)
        
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            count_files_changed += 1
                
    # 3. Append new strings to strings.xml
    if extracted_strings:
        with open(STRINGS_FILE, 'r', encoding='utf-8') as f:
            s_content = f.read()
        
        insert_idx = s_content.rfind('</resources>')
        if insert_idx != -1:
            new_strings = "\n    <!-- Auto-extracted strings (Script) -->\n"
            for name, val in extracted_strings.items():
                val_escaped = re.sub(r'&(?!amp;|lt;|gt;|quot;|apos;)', '&amp;', val).replace("'", "\\'").replace('"', '\\"')
                new_strings += f'    <string name="{name}">{val_escaped}</string>\n'
            s_content = s_content[:insert_idx] + new_strings + s_content[insert_idx:]
            
        with open(STRINGS_FILE, 'w', encoding='utf-8') as f:
            f.write(s_content)
            
    # 4. Append new colors to colors.xml
    if extracted_colors:
        with open(COLORS_FILE, 'r', encoding='utf-8') as f:
            c_content = f.read()
        
        insert_idx = c_content.rfind('</resources>')
        if insert_idx != -1:
            new_colors = "\n    <!-- Auto-extracted colors (Script) -->\n"
            for name, hex_val in extracted_colors.items():
                new_colors += f'    <color name="{name}">{hex_val}</color>\n'
            c_content = c_content[:insert_idx] + new_colors + c_content[insert_idx:]
            
        with open(COLORS_FILE, 'w', encoding='utf-8') as f:
            f.write(c_content)
            
    print("=== Extraction Complete ===")
    print(f"Files modified: {count_files_changed}")
    print(f"New strings extracted: {len(extracted_strings)}")
    print(f"New colors extracted: {len(extracted_colors)}")

if __name__ == '__main__':
    main()
