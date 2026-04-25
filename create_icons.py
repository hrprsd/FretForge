import os
import base64

base_dir = r"C:\Users\Haraprasad\.gemini\antigravity\Projects\FretForge\app\src\main\res"

# Base64 for a simple 1x1 transparent png
png_data = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg==')

folders = ['mipmap-mdpi', 'mipmap-hdpi', 'mipmap-xhdpi', 'mipmap-xxhdpi', 'mipmap-xxxhdpi']
files = ['ic_launcher.png', 'ic_launcher_round.png']

for folder in folders:
    folder_path = os.path.join(base_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    for file in files:
        with open(os.path.join(folder_path, file), 'wb') as f:
            f.write(png_data)

print("Icons created.")
