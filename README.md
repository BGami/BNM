## BNM
# Driver Drowsiness Detection Project

## Overview
This project utilizes artificial intelligence, particularly computer vision, to develop an automated system for detecting driver drowsiness in real time. This innovation aligns with our broader objective of enhancing road safety and reducing accident risks through advanced AI-driven monitoring solutions. By accurately identifying signs of fatigue, the system helps prevent drowsiness-related incidents, ultimately improving driver well-being, reducing accident rates, and supporting industry efforts to promote safer transportation practices.

## Team Members
- Nassar Alsharif
- Maitha Alhammadi
- Bhavin Gami

## Project Structure
The repository is organized into several key components:

### Mobile Application
- **DrowzeeApp**: Android application (Kotlin-based) for real-time drowsiness detection
  - Contains the mobile implementation of the drowsiness detection system
  - Includes model integration for on-device inference

### ML Pipeline and Model Development
- **Baseline Code**: Initial implementation and baseline models
- **Data Processing**: Scripts for data preprocessing and feature extraction
- **MLOPS_Pipeline**: End-to-end machine learning operations pipeline
  - Includes ClearML tasks and pipeline integration
- **Model Training**: Scripts and notebooks for training drowsiness detection models
- **eye_state_model.h5**: Pre-trained model for eye state detection

### Data and Utilities
- **data**: Dataset files and resources
- **utils**: Helper functions and utility scripts

## Features
- Real-time drowsiness detection using computer vision
- Mobile application for on-device monitoring
- Eye state detection for drowsiness indicators
- MLOps pipeline for model training and deployment
- Comprehensive data processing workflow

## Installation

### Prerequisites
- Python 3.x
- TensorFlow/Keras
- Android Studio (for mobile app development)
- ClearML (for MLOps pipeline)

### Setup Instructions
1. Clone the repository:
   ```
   git clone https://github.com/BGami/BNM.git
   cd BNM
   ```

2. Install Python dependencies:
   ```
   pip install -r requirements.txt
   ```

3. Download pre-trained models:
   - Follow instructions in the `Model Downloading Instructions` file in the DrowzeeApp directory

### Mobile App Setup
1. Open the DrowzeeApp directory in Android Studio
2. Sync Gradle files
3. Build and run the application on an Android device

## Usage
1. For model training and evaluation, refer to notebooks in the Baseline Code directory
2. To run the MLOps pipeline, use the notebook in the MLOPS_Pipeline directory
3. For mobile application usage, install the app on an Android device and follow the in-app instructions

## Note
For webapp version, you can download all codes from here (Google Drive link). We are doing this way as we could not upload it to GitHub.

## License
This project is available for academic and research purposes.
