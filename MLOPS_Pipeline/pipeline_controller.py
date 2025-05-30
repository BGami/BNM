"""
Pipeline controller script for Drowsiness Detection with HPO.
This script orchestrates the entire pipeline, from preprocessing to HPO and evaluation.
"""
from clearml import Task
from clearml.automation import PipelineController

def main():
    # Initialize the pipeline controller
    pipe = PipelineController(
        project="BNM Pipeline HPO",
        name="Drowsiness Detection Pipeline with HPO",
        version="1.0"
    )
    
    # Add Step 1: Smart Data Preprocessing (already completed)
    pipe.add_step(
        name="preprocessing",
        base_task_project="BNM Pipeline HPO",
        base_task_name="Step 1 - Smart Data Preprocessing (Deep Scan)",
        parameter_override={},
        parents=[],
        execution_queue="bnm04"
    )
    
    # Add Step 2: Feature Extraction
    pipe.add_step(
        name="feature_extraction",
        base_task_project="BNM Pipeline HPO",
        base_task_name="Step 2 - Feature Extraction",
        parameter_override={},
        parents=["preprocessing"],
        execution_queue="bnm04"
    )
    
    # Add Step 3: Model Training HPO (base task for HPO)
    pipe.add_step(
        name="model_training_base",
        base_task_project="BNM Pipeline HPO",
        base_task_name="Step 3 - Model Training HPO",
        parameter_override={},
        parents=["feature_extraction"],
        execution_queue="bnm04"
    )
    
    # Add Step 4: Hyperparameter Optimization
    pipe.add_step(
        name="hpo",
        base_task_project="BNM Pipeline HPO",
        base_task_name="Step 4 - Hyperparameter Optimization",
        parameter_override={
            "Args/num_trials": 10,
            "Args/time_limit_minutes": 60,
            "Args/max_epochs": 20
        },
        parents=["model_training_base"],
        execution_queue="bnm04"
    )
    
    # Add Step 5: Model Evaluation
    pipe.add_step(
        name="model_evaluation",
        base_task_project="BNM Pipeline HPO",
        base_task_name="Step 5 - Model Evaluation HPO",
        parameter_override={},
        parents=["hpo"],
        execution_queue="bnm04"
    )


    pipe.start_locally(run_pipeline_steps_locally=False)

    print("Pipeline started! Monitor progress in the ClearML UI.")

if __name__ == "__main__":
    main()
