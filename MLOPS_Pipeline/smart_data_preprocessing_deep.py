from clearml import Task, Dataset
import os
import cv2
import numpy as np
from sklearn.model_selection import train_test_split

# Step 0: Initialize ClearML task
task = Task.init(project_name="BNM Pipeline", task_name="Step 1 - Smart Data Preprocessing (Deep Scan)")

# Fix numpy version conflict
task.add_requirements("numpy", ">=1.19.5,<2.0.0")

# Step 1: Get dataset from ClearML
dataset = Dataset.get(dataset_name="Drowsiness Dataset", dataset_project="BNM Pipeline")
dataset_path = dataset.get_local_copy()

# Folder mappings
LABELS = {"Closed": 1, "yawn": 1, "Open": 0, "no_yawn": 0}
expected_folders = set(LABELS.keys())

# Step 2: Find the root directory that contains all label folders
def find_best_dataset_root(root_path):
    for root, dirs, _ in os.walk(root_path):
        if expected_folders.issubset(set(dirs)):
            return root
    return None

resolved_dataset_path = find_best_dataset_root(dataset_path)
if not resolved_dataset_path:
    raise ValueError("Could not locate dataset folders: Closed, Yawn, Open, no_yawn")

print(f" Using dataset path: {resolved_dataset_path}")

# Step 3: Print structure
print(" Folder structure:")
for root, dirs, _ in os.walk(resolved_dataset_path):
    level = root.replace(resolved_dataset_path, "").count(os.sep)
    indent = "    " * level
    print(f"{indent}- {os.path.basename(root)}/")
    for d in dirs:
        print(f"{indent}    - {d}/")

# Step 4: Preprocess and label images
IMAGE_SIZE = (224, 224)
data = []

for category, label in LABELS.items():
    folder_path = os.path.join(resolved_dataset_path, category)
    if not os.path.exists(folder_path):
        print(f" Skipping missing folder: {folder_path}")
        continue
    for file in os.listdir(folder_path):
        img_path = os.path.join(folder_path, file)
        img = cv2.imread(img_path)
        if img is None:
            continue
        img = cv2.resize(img, IMAGE_SIZE)
        data.append([img, label])

if not data:
    raise ValueError(" No data found. Please check dataset folder structure.")

# Prepare dataset arrays
X, y = zip(*data)
X = np.array(X) / 255.0
y = np.array(y)

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)

# Save and upload
np.savez("processed_data.npz", X_train=X_train, X_test=X_test, y_train=y_train, y_test=y_test)
task.upload_artifact("processed_data", artifact_object="processed_data.npz")

print(" Preprocessing completed and uploaded to ClearML.")
