from clearml import Task
import numpy as np
import os
import cv2
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras.applications.mobilenet_v2 import preprocess_input
from sklearn.model_selection import train_test_split

# Create the task
task = Task.init(project_name="BNM Pipeline", task_name="Step 2 - Feature Extraction")

# Fix numpy version issue by adding requirements directly to the task
task.add_requirements("numpy", ">=1.19.5,<2.0.0")

# Get the preprocessed data from the previous step
preprocessed_data_path = Task.get_task(task_name="Step 1 - Smart Data Preprocessing (Deep Scan)", 
                                      project_name="BNM Pipeline").artifacts['processed_data'].get_local_copy()

print(f"Loading preprocessed data from: {preprocessed_data_path}")

# Load the preprocessed data
data = np.load(preprocessed_data_path)
X_train = data['X_train']
X_test = data['X_test']
y_train = data['y_train']
y_test = data['y_test']

print(f"Loaded data shapes: X_train: {X_train.shape}, X_test: {X_test.shape}")
print(f"Label shapes: y_train: {y_train.shape}, y_test: {y_test.shape}")

# Create feature extractor model
base_model = MobileNetV2(weights='imagenet', include_top=False, input_shape=(224, 224, 3))

# Extract features
def extract_features(X):
    features = []
    total = len(X)
    for i, img in enumerate(X):
        if i % 100 == 0:
            print(f"Processing image {i}/{total}")
        img = preprocess_input(img)
        features.append(base_model.predict(np.expand_dims(img, axis=0), verbose=0))
    return np.array(features)

print("Extracting features from training data...")
X_train_feat = extract_features(X_train)
print("Extracting features from test data...")
X_test_feat = extract_features(X_test)

print(f"Feature shapes: X_train_feat: {X_train_feat.shape}, X_test_feat: {X_test_feat.shape}")
'''
# Save features
np.savez_compressed('features.npz', X_train_feat=X_train_feat, X_test_feat=X_test_feat)

# Save labels for the next step
np.savez_compressed('processed_data.npz', y_train=y_train, y_test=y_test, X_train=X_train, X_test=X_test)
'''

np.savez_compressed('features.npz',
                    X_train_feat=X_train_feat,
                    X_test_feat=X_test_feat,
                    y_train=y_train,
                    y_test=y_test)
# Upload artifacts
task.upload_artifact('features', artifact_object='features.npz')
#task.upload_artifact('processed_data', artifact_object='processed_data.npz')

print("Feature extraction completed successfully!")

# Execute remotely (comment this out when creating the task template)
# task.execute_remotely()