from clearml import Task, Logger
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt
import numpy as np
import json
from tensorflow.keras.models import load_model

# Init ClearML Task
task = Task.init(project_name="BNM Pipeline", task_name="Model Evaluation")
logger = task.get_logger()

# Load data & model artifacts from previous steps
label_task = Task.get_task(project_name="BNM Pipeline", task_name="Step 1 - Smart Data Preprocessing (Deep Scan)")
feature_task = Task.get_task(project_name="BNM Pipeline", task_name="Step 2 - Feature Extraction")
training_task = Task.get_task(project_name="BNM Pipeline", task_name="Step 3 - Model Training (Eye and Yawn)")

features_path = feature_task.artifacts["features"].get_local_copy()
labels_path = label_task.artifacts["processed_data"].get_local_copy()
eye_model_path = training_task.artifacts["eye_model"].get_local_copy()
eye_history_path = training_task.artifacts["eye_history"].get_local_copy()
yawn_model_path = training_task.artifacts["yawn_model"].get_local_copy()

# Load test data
features = np.load(features_path)
labels = np.load(labels_path)
X_test = features["X_test_feat"]
if X_test.ndim == 5:
    X_test = X_test.reshape((X_test.shape[0], -1))  # Flatten if not already

y_test = labels["y_test"]

# Load models
eye_model = load_model(eye_model_path)
yawn_model = load_model(yawn_model_path)

# ------------------ Eye Model Evaluation ------------------
eye_pred_probs = eye_model.predict(X_test).flatten()
eye_preds = (eye_pred_probs > 0.5).astype(int)

logger.report_text("=== Eye Model Evaluation ===")
logger.report_text("Confusion Matrix:\n" + str(confusion_matrix(y_test, eye_preds)))
logger.report_text("Classification Report:\n" + classification_report(y_test, eye_preds))

# ------------------ Yawn Model Evaluation ------------------
yawn_pred_probs = yawn_model.predict(X_test).flatten()
yawn_preds = (yawn_pred_probs > 0.5).astype(int)

logger.report_text("=== Yawn Model Evaluation ===")
logger.report_text("Confusion Matrix:\n" + str(confusion_matrix(y_test, yawn_preds)))
logger.report_text("Classification Report:\n" + classification_report(y_test, yawn_preds))

# ------------------ Accuracy & Loss Plots ------------------
with open(eye_history_path, "r") as f:
    history = json.load(f)

# Accuracy plot
plt.figure()
plt.plot(history["accuracy"], label="Train Accuracy")
if "val_accuracy" in history:
    plt.plot(history["val_accuracy"], label="Val Accuracy")
plt.title("Eye Model Accuracy Curve")
plt.xlabel("Epochs")
plt.ylabel("Accuracy")
plt.legend()
plt.grid(True)
plt.savefig("accuracy_plot.png")
logger.report_image("Eye Model Accuracy", "Accuracy Plot", local_path="accuracy_plot.png")


# Loss plot
plt.figure()
plt.plot(history["loss"], label="Train Loss")
if "val_loss" in history:
    plt.plot(history["val_loss"], label="Val Loss")
plt.title("Eye Model Loss Curve")
plt.xlabel("Epochs")
plt.ylabel("Loss")
plt.legend()
plt.grid(True)
plt.savefig("loss_plot.png")
logger.report_image("Eye Model Loss", "Loss Plot", local_path="loss_plot.png")

print(" Evaluation completed and results logged to ClearML.")
