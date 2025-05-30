"""
HPO Optimizer script for Drowsiness Detection pipeline.
This script implements Hyperparameter Optimization (HPO) for the Drowsiness Detection pipeline,
using ClearML's HyperParameterOptimizer to find the best set of hyperparameters for model_training_hpo.py.
"""
from clearml import Task
from clearml.automation import HyperParameterOptimizer, UniformParameterRange, DiscreteParameterRange, UniformIntegerParameterRange
from clearml.automation.optuna import OptimizerOptuna
import time
import json

def main():
    # Initialize the HPO task
    hpo_task = Task.init(
        project_name="BNM Pipeline HPO",
        task_name="Step 4 - Hyperparameter Optimization", 
        task_type=Task.TaskTypes.optimizer
    )
    
    # Add requirements
    hpo_task.add_requirements("clearml")
    hpo_task.add_requirements("optuna")
    hpo_task.add_requirements("numpy", ">=1.19.5,<2.0.0")
    hpo_task.add_requirements("tensorflow")

    # Define parameters
    args = {
        'num_trials': 10,  # Number of HPO trials to run
        'time_limit_minutes': 60,  # Time limit for HPO
        'execution_queue': 'bnm04',  # Queue for execution
        'max_epochs': 20,  # Maximum epochs for any trial
    }
    args = hpo_task.connect(args)
    print(f"HPO parameters: {args}")

    # Get the base task for HPO
    base_task_project = "BNM Pipeline HPO"
    base_task_name = "Step 3 - Model Training HPO"

    try:
        base_task_object = Task.get_task(project_name=base_task_project, task_name=base_task_name)
        if not base_task_object:
            print(f"Error: Base task '{base_task_name}' in project '{base_task_project}' not found. Please ensure it is registered.")
            return
        base_task_id = base_task_object.id
        print(f"Found base task '{base_task_name}' with ID: {base_task_id}")
    except Exception as e:
        print(f"Error retrieving base task '{base_task_name}': {e}. Please ensure it is registered.")
        return

    # Define hyperparameters to optimize
    # IMPORTANT: Use flat parameter names (no 'Args/' prefix) to match the training script
    hyper_params = [
        UniformParameterRange("learning_rate", min_value=0.0001, max_value=0.01, step_size=0.0001),
        DiscreteParameterRange("batch_size", values=[16, 32, 64]),
        UniformIntegerParameterRange("num_units_1", min_value=128, max_value=512, step_size=64),
        UniformIntegerParameterRange("num_units_2", min_value=128, max_value=512, step_size=64),
        UniformParameterRange("dropout_rate", min_value=0.2, max_value=0.5, step_size=0.1),
        DiscreteParameterRange("epochs", values=[10, 15, 20])
    ]

    # Define objective metric
    objective_metric_title = "validation"
    objective_metric_series = "average_accuracy" 
    objective_metric_sign = "max"  

    # Initialize the optimizer
    print("Initializing HyperParameterOptimizer...")
    optimizer = HyperParameterOptimizer(
        base_task_id=base_task_id, 
        hyper_parameters=hyper_params,
        objective_metric_title=objective_metric_title,
        objective_metric_series=objective_metric_series,
        objective_metric_sign=objective_metric_sign,
        optimizer_class=OptimizerOptuna,
        max_iteration_per_job=args['max_epochs'], 
        max_number_of_concurrent_tasks=2,  # Run 2 trials concurrently
        total_max_jobs=args['num_trials'], 
        execution_queue=args['execution_queue'],
        time_limit_per_job=args['time_limit_minutes'] * 60,  # Convert to seconds
    )

    # Start the optimization process
    print(f"Starting HPO process with {args['num_trials']} trials...")
    optimizer.start()
    print(f"HPO started with task ID: {hpo_task.id}. Monitor progress in the ClearML UI.")

    # Wait for optimization to complete
    print(f"Waiting for optimization to complete (time limit: {args['time_limit_minutes']} minutes)...")
    start_time = time.time()
    while time.time() - start_time < args['time_limit_minutes'] * 60:
        if not optimizer.is_running():
            print("Optimization process completed.")
            break
        time.sleep(30)  # Check every 30 seconds
    
    # Get the top experiments
    top_experiments = optimizer.get_top_experiments(top_k=1)
    if top_experiments:
        best_experiment = top_experiments[0]
        best_task_id = best_experiment.id
        print(f"Best training task ID: {best_task_id}")

        # Get the best task
        best_task = Task.get_task(task_id=best_task_id)
        
        # Get the best parameters
        best_params = best_experiment.get_parameters()
        print(f"Best parameters: {best_params}")
        
        # Save best parameters as artifact
        best_params_file = "best_parameters.json"
        with open(best_params_file, "w") as f:
            json.dump(best_params, f, indent=4)
        hpo_task.upload_artifact("best_parameters", artifact_object=best_params_file)
        
        # Upload best model artifacts
        artifacts_to_upload = {
            "eye_model": "best_eye_model",
            "yawn_model": "best_yawn_model",
            "eye_history": "best_eye_history",
            "yawn_history": "best_yawn_history"
        }

        for source_artifact_name, target_artifact_name in artifacts_to_upload.items():
            artifact_info = best_task.artifacts.get(source_artifact_name)
            if artifact_info:
                local_path = artifact_info.get_local_copy()
                hpo_task.upload_artifact(name=target_artifact_name, artifact_object=local_path)
                print(f"Uploaded {target_artifact_name} from task {best_task_id} to HPO task {hpo_task.id}")
            else:
                print(f"Could not find '{source_artifact_name}' artifact in task {best_task_id}")
    else:
        print("No experiments were completed successfully by HPO, or no top experiments found.")

    print(f"HPO task {hpo_task.id} finished. Best models and parameters (if any) are uploaded as artifacts.")

if __name__ == "__main__":
    main()
