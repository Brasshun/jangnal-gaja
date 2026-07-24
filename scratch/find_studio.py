import os
import subprocess

common_paths = [
    r"C:\Program Files\Android\Android Studio\bin\studio64.exe",
    r"C:\Program Files\Android Studio\bin\studio64.exe",
    r"C:\Users\MJ\AppData\Local\Programs\Android Studio\bin\studio64.exe",
    r"C:\Program Files (x86)\Android\Android Studio\bin\studio64.exe"
]

project_path = r"C:\Users\MJ\.gemini\antigravity\scratch\jangnal-gaja"

found_path = None
for path in common_paths:
    if os.path.exists(path):
        found_path = path
        break

if found_path:
    print("FOUND Android Studio:", found_path)
    # Start Android Studio with the project path as argument
    # We use subprocess.Popen so it runs independently of this script
    try:
        subprocess.Popen([found_path, project_path])
        print("LAUNCH_SUCCESS")
    except Exception as e:
        print("LAUNCH_FAILED:", e)
else:
    print("Android Studio executable not found in common paths.")
