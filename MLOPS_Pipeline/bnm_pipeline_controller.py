from clearml import PipelineController

# Create the pipeline controller
pipe = PipelineController(
    name="BNM Sprint 2 ML Pipeline 1/05",
    project="BNM Pipeline",
    version="1.0",
    add_pipeline_tags=False
)

pipe.add_step(
    name="DataPreprocessing",
    base_task_project="BNM Pipeline",
    base_task_name="Step 1 - Smart Data Preprocessing (Deep Scan)",
    execution_queue="bnm03"
)

pipe.add_step(
    name="FeatureExtraction",
    base_task_project="BNM Pipeline",
    base_task_name="Step 2 - Feature Extraction",
    parents=["DataPreprocessing"],
    execution_queue="bnm03"
)

pipe.add_step(
    name="ModelTraining",
    base_task_project="BNM Pipeline",
    base_task_name="Step 3 - Model Training (Eye and Yawn)",
    parents=["FeatureExtraction"],
    execution_queue="bnm03"
)

# Start the pipeline
pipe.start_locally(run_pipeline_steps_locally=False)