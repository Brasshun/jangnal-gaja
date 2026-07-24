import os
from PIL import Image

def process_marker():
    # Process the Green (Permanent) market marker
    src_file = r"C:/Users/MJ/.gemini/antigravity/brain/45259a4a-be81-43ae-b679-3b4130b9f3b1/uploaded_image_1768970858905.png"
    dest_path = r"c:/Users/MJ/.gemini/antigravity/scratch/jangnal-gaja/app/src/main/res/drawable/ic_marker_permanent.png"
    
    if not os.path.exists(src_file):
        print(f"Error: Source file not found: {src_file}")
        # Fallback to previous upload if this one fails? No, the user just uploaded it.
        # Check previous uploads in history if needed.
        # User uploaded `uploaded_image_1768970566332.png` in step 116.
        # User uploaded `uploaded_image_1768970858905.png` in step 144.
        # They seem to be the same image or similar. Use the latest one.
        return

    try:
        img = Image.open(src_file).convert('RGBA')
        
        # Target size matching others (100px width)
        target_width = 100
        w_percent = (target_width / float(img.size[0]))
        h_size = int((float(img.size[1]) * float(w_percent)))
        img_resized = img.resize((target_width, h_size), Image.Resampling.LANCZOS)
        
        img_resized.save(dest_path)
        print(f"Saved permanent marker to {dest_path}")
        
    except Exception as e:
        print(f"Failed to process marker: {e}")

if __name__ == "__main__":
    process_marker()
