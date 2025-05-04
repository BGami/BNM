from clearml import Task
import numpy as np
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Flatten, Dense, Dropout, BatchNormalization
from tensorflow.keras.optimizers import Adam

# Create the task
task = Task.init(project_name="BNM Pipeline", task_name="Step 3 - Model Training (Eye and Yawn)")

# Fix numpy version issue by adding requirements directly to the task
task.add_requirements("numpy", ">=1.19.5,<2.0.0")

# Get the features from the previous s`tep
features_path = Task.get_task(task_name="Step 2 - Feature Extraction", 
                             project_name="BNM Pipeline").artifacts['features'].get_local_copy()

# Get the labels from the previous step
labels_path = Task.get_task(task_name="Step 1 - Smart Data Preprocessing (Deep Scan)", 
                                      project_name="BNM Pipeline").artifacts['processed_data'].get_local_copy()
                                      
print(f"Loading features from: {features_path}")
print(f"Loading labels from: {labels_path}")

# Load feature data
with np.load(features_path) as data:
    X_train_feat, X_test_feat = data["X_train_feat"], data["X_test_feat"]

# Load label data
with np.load(labels_path) as data:
    y_train, y_test = data["y_train"], data["y_test"]
    X_train_orig = data["X_train"] if "X_train" in data else None
    X_test_orig = data["X_test"] if "X_test" in data else None

print(f"Feature shapes: X_train_feat: {X_train_feat.shape}, X_test_feat: {X_test_feat.shape}")
print(f"Label shapes: y_train: {y_train.shape}, y_test: {y_test.shape}")

# Reshape features if needed
if len(X_train_feat.shape) > 2:
    X_train_feat = X_train_feat.reshape(X_train_feat.shape[0], -1)
    X_test_feat = X_test_feat.reshape(X_test_feat.shape[0], -1)
    print(f"Reshaped features: X_train_feat: {X_train_feat.shape}, X_test_feat: {X_test_feat.shape}")

# Split data into eye and yawn datasets
# We'll use the first half for eyes and second half for yawn
# This is a simplification - in a real scenario, you'd want to properly identify which is which
total_samples = len(X_train_feat)
half_point = total_samples // 2

# Training data
X_train_eyes = X_train_feat[:half_point]
y_train_eyes = y_train[:half_point]

X_train_yawn = X_train_feat[half_point:]
y_train_yawn = y_train[half_point:]

# Test data
test_samples = len(X_test_feat)
test_half = test_samples // 2

X_test_eyes = X_test_feat[:test_half]
y_test_eyes = y_test[:test_half]

X_test_yawn = X_test_feat[test_half:]
y_test_yawn = y_test[test_half:]

# Ensure we have test data for both models
if len(X_test_eyes) == 0:
    # If no test data for eyes, use some training data
    split_point = int(len(X_train_eyes) * 0.8)
    X_test_eyes = X_train_eyes[split_point:]
    y_test_eyes = y_train_eyes[split_point:]
    X_train_eyes = X_train_eyes[:split_point]
    y_train_eyes = y_train_eyes[:split_point]

if len(X_test_yawn) == 0:
    # If no test data for yawn, use some training data
    split_point = int(len(X_train_yawn) * 0.8)
    X_test_yawn = X_train_yawn[split_point:]
    y_test_yawn = y_train_yawn[split_point:]
    X_train_yawn = X_train_yawn[:split_point]
    y_train_yawn = y_train_yawn[:split_point]

print(f"Eye detection dataset: {len(X_train_eyes)} training samples, {len(X_test_eyes)} testing samples")
print(f"Yawn detection dataset: {len(X_train_yawn)} training samples, {len(X_test_yawn)} testing samples")

# Define model architecture for eye detection
eye_model = Sequential([
    Dense(512, activation='relu', input_shape=(X_train_eyes.shape[1],)),
    BatchNormalization(),
    Dropout(0.5),
    Dense(512, activation='relu'),
    BatchNormalization(),
    Dropout(0.5),
    Dense(1, activation='sigmoid')
])

# Compile eye model
eye_model.compile(optimizer=Adam(learning_rate=0.001), 
                 loss='binary_crossentropy', 
                 metrics=['accuracy'])

# Train eye model
print("Training eye detection model...")
eye_history = eye_model.fit(
    X_train_eyes, y_train_eyes,
    validation_data=(X_test_eyes, y_test_eyes),
    epochs=10,
    batch_size=32
)
# Save history dict
# Save history as JSON
import json
with open("eye_history.json", "w") as f:
    json.dump(eye_history.history, f)

task.upload_artifact("eye_history", artifact_object="eye_history.json")

# Define model architecture for yawn detection
yawn_model = Sequential([
    Dense(512, activation='relu', input_shape=(X_train_yawn.shape[1],)),
    BatchNormalization(),
    Dropout(0.5),
    Dense(512, activation='relu'),
    BatchNormalization(),
    Dropout(0.5),
    Dense(1, activation='sigmoid')
])

# Compile yawn model
yawn_model.compile(optimizer=Adam(learning_rate=0.001), 
                  loss='binary_crossentropy', 
                  metrics=['accuracy'])

# Train yawn model
print("Training yawn detection model...")
yawn_history = yawn_model.fit(
    X_train_yawn, y_train_yawn,
    validation_data=(X_test_yawn, y_test_yawn),
    epochs=10,
    batch_size=32
)

# Save history as JSON
import json
with open("yawn_history.json", "w") as f:
    json.dump(yawn_history.history, f)

task.upload_artifact("yawn_history", artifact_object="yawn_history.json")

# Save models with the exact same filenames as the original implementation
eye_model.save("eye_feature_best.h5")
yawn_model.save("yawn_feature_best.h5")

# Upload models as artifacts
task.upload_artifact("eye_model", artifact_object="eye_feature_best.h5")
task.upload_artifact("yawn_model", artifact_object="yawn_feature_best.h5")

# Print final evaluation
print("\nEye Detection Model Evaluation:")
eye_eval = eye_model.evaluate(X_test_eyes, y_test_eyes)
print(f"Test Loss: {eye_eval[0]:.4f}")
print(f"Test Accuracy: {eye_eval[1]:.4f}")

print("\nYawn Detection Model Evaluation:")
yawn_eval = yawn_model.evaluate(X_test_yawn, y_test_yawn)
print(f"Test Loss: {yawn_eval[0]:.4f}")
print(f"Test Accuracy: {yawn_eval[1]:.4f}")

# Execute remotely (comment this out when creating the task template)
# task.execute_remotely()