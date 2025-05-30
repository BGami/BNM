"""
Model evaluation script for Drowsiness Detection pipeline with HPO.
This script evaluates the best models found by the HPO process.
"""
import os
print("Running NEW version of model_evaluation_hpo.py")
os.environ["MPLBACKEND"] = "Agg"
os.environ.pop("MPLCONFIGDIR", None)
from clearml import Task, Logger
import numpy as np
import tensorflow as tf
import json
import matplotlib.pyplot as plt
import matplotlib
matplotlib.use('Agg')
import os

def main():
    # Initialize ClearML Task
    task = Task.init(project_name="BNM Pipeline HPO", task_name="Step 5 - Model Evaluation HPO")
    
    # Add requirements
    task.add_requirements("numpy", ">=1.19.5,<2.0.0")
    task.add_requirements("tensorflow")
    task.add_requirements("matplotlib")

    # Get the best models from the HPO task
    hpo_task = Task.get_task(task_name="Step 4 - Hyperparameter Optimization", 
                            project_name="BNM Pipeline HPO")
    
    # Get the best parameters
    best_params_path = hpo_task.artifacts["best_parameters"].get_local_copy()
    with open(best_params_path, 'r') as f:
        best_params = json.load(f)
    
    print(f"Best parameters from HPO: {best_params}")
    
    # Get the best models
    best_eye_model_path = hpo_task.artifacts["best_eye_model"].get_local_copy()
    best_yawn_model_path = hpo_task.artifacts["best_yawn_model"].get_local_copy()
    
    # Get the history files
    best_eye_history_path = hpo_task.artifacts["best_eye_history"].get_local_copy()
    best_yawn_history_path = hpo_task.artifacts["best_yawn_history"].get_local_copy()
    
    # Load the history files
    with open(best_eye_history_path, 'r') as f:
        eye_history = json.load(f)
    
    with open(best_yawn_history_path, 'r') as f:
        yawn_history = json.load(f)
    
    # Get test data from feature extraction task
    features_path = Task.get_task(task_name="Step 2 - Feature Extraction",
                                 project_name="BNM Pipeline HPO").artifacts["features"].get_local_copy()
    
    with np.load(features_path) as data:
        X_test_feat = data["X_test_feat"]
        y_test = data["y_test"]
    
    print(f"Test data shapes: X_test_feat: {X_test_feat.shape}, y_test: {y_test.shape}")
    
    # Reshape if needed
    if len(X_test_feat.shape) > 2:
        X_test_feat = X_test_feat.reshape(X_test_feat.shape[0], -1)
        print(f"Reshaped test features: {X_test_feat.shape}")
    
    # Split test data for eye and yawn models
    test_half = len(X_test_feat) // 2
    X_test_eyes, y_test_eyes = X_test_feat[:test_half], y_test[:test_half]
    X_test_yawn, y_test_yawn = X_test_feat[test_half:], y_test[test_half:]
    
    # Load the models
    eye_model = tf.keras.models.load_model(best_eye_model_path)
    yawn_model = tf.keras.models.load_model(best_yawn_model_path)
    
    # Evaluate the models
    eye_loss, eye_accuracy = eye_model.evaluate(X_test_eyes, y_test_eyes)
    yawn_loss, yawn_accuracy = yawn_model.evaluate(X_test_yawn, y_test_yawn)
    
    # Calculate average accuracy
    average_accuracy = (eye_accuracy + yawn_accuracy) / 2.0
    
    print(f"Eye Model Test Accuracy: {eye_accuracy:.4f}")
    print(f"Yawn Model Test Accuracy: {yawn_accuracy:.4f}")
    print(f"Overall Average Accuracy of Best HPO Models: {average_accuracy:.4f}")
    
    # Log metrics
    logger = task.get_logger()
    logger.report_scalar(title="test", series="eye_accuracy", value=eye_accuracy, iteration=0)
    logger.report_scalar(title="test", series="yawn_accuracy", value=yawn_accuracy, iteration=0)
    logger.report_scalar(title="test", series="average_accuracy", value=average_accuracy, iteration=0)
    
    # Plot learning curves
    os.makedirs("plots", exist_ok=True)
    
    # Eye model learning curves
    plt.figure(figsize=(12, 5))
    plt.subplot(1, 2, 1)
    plt.plot(eye_history.get("accuracy", []), label="Training Accuracy")
    plt.plot(eye_history.get("val_accuracy", []), label="Validation Accuracy")
    plt.title("Eye Model Accuracy")
    plt.xlabel("Epoch")
    plt.ylabel("Accuracy")
    plt.legend()
    
    plt.subplot(1, 2, 2)
    plt.plot(eye_history.get("loss", []), label="Training Loss")
    plt.plot(eye_history.get("val_loss", []), label="Validation Loss")
    plt.title("Eye Model Loss")
    plt.xlabel("Epoch")
    plt.ylabel("Loss")
    plt.legend()
    
    plt.tight_layout()
    plt.savefig("best_hpo_eye_model_curves.png")
    
    # Yawn model learning curves
    plt.figure(figsize=(12, 5))
    plt.subplot(1, 2, 1)
    plt.plot(yawn_history.get("accuracy", []), label="Training Accuracy")
    plt.plot(yawn_history.get("val_accuracy", []), label="Validation Accuracy")
    plt.title("Yawn Model Accuracy")
    plt.xlabel("Epoch")
    plt.ylabel("Accuracy")
    plt.legend()
    
    plt.subplot(1, 2, 2)
    plt.plot(yawn_history.get("loss", []), label="Training Loss")
    plt.plot(yawn_history.get("val_loss", []), label="Validation Loss")
    plt.title("Yawn Model Loss")
    plt.xlabel("Epoch")
    plt.ylabel("Loss")
    plt.legend()
    
    plt.tight_layout()
    plt.savefig("best_hpo_yawn_model_curves.png")
    
    # Upload plots
    logger.report_image(title="Best HPO Eye Model Learning Curves", series="hpo_model_plots", iteration=1, local_path="best_hpo_eye_model_curves.png")
    logger.report_image(title="Best HPO Yawn Model Learning Curves", series="hpo_model_plots", iteration=2, local_path="best_hpo_yawn_model_curves.png") # Iteration 2 for yawn plots
    
    # Save final models
    eye_model.save("final_eye_model.h5")
    yawn_model.save("final_yawn_model.h5")
    
    # Upload final models
    task.upload_artifact("final_eye_model", artifact_object="final_eye_model.h5")
    task.upload_artifact("final_yawn_model", artifact_object="final_yawn_model.h5")
    
    print("Model evaluation completed successfully!")

if __name__ == "__main__":
    main()
