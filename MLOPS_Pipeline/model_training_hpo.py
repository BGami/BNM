"""
Model training script for Drowsiness Detection, modified for HPO.
This script is designed to be used as a base task for hyperparameter optimization.
It uses a flat dictionary for hyperparameters in task.connect() to ensure HPO overrides work correctly.
"""
from clearml import Task, Logger
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, BatchNormalization
from tensorflow.keras.optimizers import Adam
import json

def main():
    # Initialize ClearML Task
    task = Task.init(project_name="BNM Pipeline HPO", task_name="Step 3 - Model Training HPO")

    # Define hyperparameters as a flat dictionary (no 'Args/' prefix)
    # This is critical for HPO to work correctly
    hyperparameters = {
        "learning_rate": 0.001,
        "batch_size": 32,
        "num_units_1": 512,
        "num_units_2": 512,
        "dropout_rate": 0.5,
        "epochs": 1  # Default epochs
    }
    print(f"Initial default hyperparameters dictionary: {hyperparameters}")

    # Connect the dictionary. HPO will override these values.
    effective_params = task.connect(hyperparameters)
    print(f"Effective parameters after task.connect(): {effective_params}")
    
    # Ensure types are correct after fetching
    try:
        actual_params = {
            "learning_rate": float(effective_params["learning_rate"]),
            "batch_size": int(effective_params["batch_size"]),
            "num_units_1": int(effective_params["num_units_1"]),
            "num_units_2": int(effective_params["num_units_2"]),
            "dropout_rate": float(effective_params["dropout_rate"]),
            "epochs": int(effective_params["epochs"])
        }
    except KeyError as e:
        print(f"ERROR: A hyperparameter key was missing: {e}. Check HPO configuration.")
        raise
    except ValueError as e:
        print(f"ERROR: Could not convert a hyperparameter to its correct type: {e}")
        raise

    print(f"Typed effective parameters for script use: {actual_params}")

    # Add requirements directly to the task
    task.add_requirements("numpy", ">=1.19.5,<2.0.0")
    task.add_requirements("tensorflow")

    # --- Load Data ---
    features_path = Task.get_task(task_name="Step 2 - Feature Extraction",
                                 project_name="BNM Pipeline HPO").artifacts["features"].get_local_copy()

    print(f"Loading features from: {features_path}")

    with np.load(features_path) as data:
        X_train_feat = data["X_train_feat"]
        X_test_feat = data["X_test_feat"]
        y_train = data["y_train"]
        y_test = data["y_test"]

    print(f"Feature shapes: X_train_feat: {X_train_feat.shape}, X_test_feat: {X_test_feat.shape}")
    print(f"Label shapes: y_train: {y_train.shape}, y_test: {y_test.shape}")

    if len(X_train_feat.shape) > 2:
        X_train_feat = X_train_feat.reshape(X_train_feat.shape[0], -1)
        X_test_feat = X_test_feat.reshape(X_test_feat.shape[0], -1)
        print(f"Reshaped features: X_train_feat: {X_train_feat.shape}, X_test_feat: {X_test_feat.shape}")

    total_samples = len(X_train_feat)
    half_point = total_samples // 2
    X_train_eyes, y_train_eyes = X_train_feat[:half_point], y_train[:half_point]
    X_train_yawn, y_train_yawn = X_train_feat[half_point:], y_train[half_point:]

    test_samples = len(X_test_feat)
    test_half = test_samples // 2
    X_test_eyes, y_test_eyes = X_test_feat[:test_half], y_test[:test_half]
    X_test_yawn, y_test_yawn = X_test_feat[test_half:], y_test[test_half:]

    if len(X_test_eyes) == 0 and len(X_train_eyes) > 0:
        split_point = int(len(X_train_eyes) * 0.8)
        X_test_eyes, y_test_eyes = X_train_eyes[split_point:], y_train_eyes[split_point:]
        X_train_eyes, y_train_eyes = X_train_eyes[:split_point], y_train_eyes[:split_point]

    if len(X_test_yawn) == 0 and len(X_train_yawn) > 0:
        split_point = int(len(X_train_yawn) * 0.8)
        X_test_yawn, y_test_yawn = X_train_yawn[split_point:], y_train_yawn[split_point:]
        X_train_yawn, y_train_yawn = X_train_yawn[:split_point], y_train_yawn[:split_point]
        
    print(f"Eye detection dataset: {len(X_train_eyes)} training samples, {len(X_test_eyes)} testing samples")
    print(f"Yawn detection dataset: {len(X_train_yawn)} training samples, {len(X_test_yawn)} testing samples")

    eye_val_accuracy = 0.0
    yawn_val_accuracy = 0.0

    # --- Eye Model --- 
    if len(X_train_eyes) > 0 and len(X_test_eyes) > 0:
        print(f"\n--- TRAINING EYE MODEL WITH {actual_params['epochs']} EPOCHS (Batch: {actual_params['batch_size']}, LR: {actual_params['learning_rate']}) ---")
        eye_model = Sequential([
            Dense(actual_params['num_units_1'], activation='relu', input_shape=(X_train_eyes.shape[1],)),
            BatchNormalization(),
            Dropout(actual_params['dropout_rate']),
            Dense(actual_params['num_units_2'], activation='relu'),
            BatchNormalization(),
            Dropout(actual_params['dropout_rate']),
            Dense(1, activation='sigmoid')
        ])
        eye_model.compile(optimizer=Adam(learning_rate=actual_params['learning_rate']),
                          loss='binary_crossentropy', metrics=['accuracy'])
        print("Training eye detection model...")
        eye_history = eye_model.fit(
            X_train_eyes, y_train_eyes,
            validation_data=(X_test_eyes, y_test_eyes),
            epochs=actual_params['epochs'],
            batch_size=actual_params['batch_size'],
            verbose=2
        )
        eye_model.save("eye_feature_best.h5")
        task.upload_artifact("eye_model", artifact_object="eye_feature_best.h5")
        with open("eye_history.json", "w") as f:
            json.dump(eye_history.history, f)
        task.upload_artifact("eye_history", artifact_object="eye_history.json")
        if 'val_accuracy' in eye_history.history and eye_history.history['val_accuracy']:
            eye_val_accuracy = eye_history.history['val_accuracy'][-1]
        task.get_logger().report_scalar(title="validation", series="eye_accuracy", value=eye_val_accuracy, iteration=actual_params['epochs'])
    else:
        print("Skipping eye model training due to insufficient data.")

    # --- Yawn Model --- 
    if len(X_train_yawn) > 0 and len(X_test_yawn) > 0:
        print(f"\n--- TRAINING YAWN MODEL WITH {actual_params['epochs']} EPOCHS (Batch: {actual_params['batch_size']}, LR: {actual_params['learning_rate']}) ---")
        yawn_model = Sequential([
            Dense(actual_params['num_units_1'], activation='relu', input_shape=(X_train_yawn.shape[1],)),
            BatchNormalization(),
            Dropout(actual_params['dropout_rate']),
            Dense(actual_params['num_units_2'], activation='relu'),
            BatchNormalization(),
            Dropout(actual_params['dropout_rate']),
            Dense(1, activation='sigmoid')
        ])
        yawn_model.compile(optimizer=Adam(learning_rate=actual_params['learning_rate']),
                           loss='binary_crossentropy', metrics=['accuracy'])
        print("Training yawn detection model...")
        yawn_history = yawn_model.fit(
            X_train_yawn, y_train_yawn,
            validation_data=(X_test_yawn, y_test_yawn),
            epochs=actual_params['epochs'],
            batch_size=actual_params['batch_size'],
            verbose=2
        )
        yawn_model.save("yawn_feature_best.h5")
        task.upload_artifact("yawn_model", artifact_object="yawn_feature_best.h5")
        with open("yawn_history.json", "w") as f:
            json.dump(yawn_history.history, f)
        task.upload_artifact("yawn_history", artifact_object="yawn_history.json")
        if 'val_accuracy' in yawn_history.history and yawn_history.history['val_accuracy']:
            yawn_val_accuracy = yawn_history.history['val_accuracy'][-1]
        task.get_logger().report_scalar(title="validation", series="yawn_accuracy", value=yawn_val_accuracy, iteration=actual_params['epochs'])
    else:
        print("Skipping yawn model training due to insufficient data.")

    # --- Report Objective Metric for HPO ---
    average_val_accuracy = 0.0
    if eye_val_accuracy > 0 and yawn_val_accuracy > 0:
        average_val_accuracy = (eye_val_accuracy + yawn_val_accuracy) / 2.0
    elif eye_val_accuracy > 0:
        average_val_accuracy = eye_val_accuracy
    elif yawn_val_accuracy > 0:
        average_val_accuracy = yawn_val_accuracy
        
    task.get_logger().report_scalar(title="validation", series="average_accuracy", value=average_val_accuracy, iteration=actual_params['epochs'])

    print(f"Final Eye Validation Accuracy: {eye_val_accuracy:.4f}")
    print(f"Final Yawn Validation Accuracy: {yawn_val_accuracy:.4f}")
    print(f"Final Average Validation Accuracy for HPO: {average_val_accuracy:.4f}")

    print("Model training script completed.")

if __name__ == "__main__":
    main()
