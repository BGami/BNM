from clearml import Dataset
import gdown
import zipfile
import os

# Download zip from Google Drive (already uploaded by user)
dataset_url = "https://drive.google.com/uc?id=1zI632W3zwVBX9Q-TX361_T00O31H4GIC"
zip_path = "DrowsinessDataset.zip"
extract_dir = "DrowsinessDataset"

# Download
if not os.path.exists(zip_path):
    gdown.download(dataset_url, zip_path, quiet=False)

# Extract
if not os.path.exists(extract_dir):
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(extract_dir)

# Upload to ClearML Dataset
dataset = Dataset.create(
    dataset_name="Drowsiness Dataset",
    dataset_project="BNM Pipeline"
)
dataset.add_files(extract_dir)
dataset.upload()
dataset.finalize()

print(" Dataset uploaded to ClearML")
