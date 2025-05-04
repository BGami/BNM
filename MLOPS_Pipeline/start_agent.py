import subprocess
import sys
import time

def start_clearml_agent():
    print("Starting ClearML Agent for queue BNM03...")
    try:
        # Start the ClearML agent in a separate process
        agent_process = subprocess.Popen(
            ["clearml-agent", "daemon", "--queue", "bnm03", "--foreground"],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            universal_newlines=True
        )
        
        # Monitor the agent output
        while True:
            output = agent_process.stdout.readline()
            if output:
                print(output.strip())
            
            # Check if the process is still running
            if agent_process.poll() is not None:
                print("ClearML Agent has stopped.")
                break
                
            time.sleep(0.1)
            
    except KeyboardInterrupt:
        print("Stopping ClearML Agent...")
        agent_process.terminate()
        agent_process.wait()
        print("ClearML Agent stopped.")
    except Exception as e:
        print(f"Error starting ClearML Agent: {e}")
        sys.exit(1)

if __name__ == "__main__":
    start_clearml_agent()
