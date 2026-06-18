import os
import sys
import re

def main():
    # Read inputs from env
    event_name = os.environ.get("GITHUB_EVENT_NAME", "")
    ref = os.environ.get("GITHUB_REF", "")
    ref_name = os.environ.get("GITHUB_REF_NAME", "")
    input_version_name = os.environ.get("INPUT_VERSION_NAME", "")
    input_version_code = os.environ.get("INPUT_VERSION_CODE", "")
    
    properties_path = "gradle.properties"
    if not os.path.exists(properties_path):
        print(f"Error: {properties_path} not found.")
        sys.exit(1)
        
    with open(properties_path, "r") as f:
        content = f.read()
        
    # Parse current version code and name
    version_code_match = re.search(r"^app\.version\.code\s*=\s*(\d+)", content, re.MULTILINE)
    version_name_match = re.search(r"^app\.version\.name\s*=\s*(.+)", content, re.MULTILINE)
    
    if not version_code_match or not version_name_match:
        print("Error: Could not parse app.version.code or app.version.name from gradle.properties")
        sys.exit(1)
        
    current_version_code = int(version_code_match.group(1))
    current_version_name = version_name_match.group(1).strip()
    
    print(f"Current version in properties: Name={current_version_name}, Code={current_version_code}")
    
    new_version_name = current_version_name
    new_version_code = current_version_code
    output_version_name = current_version_name
    run_number = os.environ.get("GITHUB_RUN_NUMBER", "1")
    
    # Logic for bumping
    should_commit = False
    
    if event_name == "push" and ref.startswith("refs/tags/v"):
        # Tag pushed (e.g. v1.2.3)
        tag_version = ref_name[1:] if ref_name.startswith("v") else ref_name
        new_version_name = tag_version
        output_version_name = tag_version
        new_version_code = current_version_code + 1
        should_commit = True
        print(f"Triggered by tag. Bumping version name to {new_version_name} and version code to {new_version_code}")
        
    elif event_name == "workflow_dispatch":
        should_commit = True
        if input_version_name and input_version_name.strip():
            new_version_name = input_version_name.strip()
            output_version_name = new_version_name
            print(f"Using manual version name: {new_version_name}")
        else:
            new_version_name = current_version_name
            output_version_name = f"{current_version_name}-build{run_number}"
            print(f"No manual version name. Generating unique build version: {output_version_name}")
            
        if input_version_code and input_version_code.strip():
            try:
                new_version_code = int(input_version_code.strip())
                print(f"Using manual version code: {new_version_code}")
            except ValueError:
                print(f"Invalid version code input: {input_version_code}. Using current + 1.")
                new_version_code = current_version_code + 1
        else:
            new_version_code = current_version_code + 1
            print(f"No manual version code provided. Auto-incrementing to: {new_version_code}")
            
    elif event_name == "push" and ref == "refs/heads/main":
        # Push to main branch (not a tag)
        new_version_name = current_version_name  # Keep base clean in properties file
        output_version_name = f"{current_version_name}-build{run_number}"
        new_version_code = current_version_code + 1
        should_commit = True
        print(f"Push to main. Bumping version code to {new_version_code} and build version name to {output_version_name}")
        
    else:
        # Pull request or other events
        new_version_name = current_version_name
        output_version_name = f"{current_version_name}-build{run_number}"
        print("Trigger event does not require version bump commit in repository.")
        
    # Replace in file content
    content = re.sub(
        r"^app\.version\.code\s*=\s*\d+",
        f"app.version.code={new_version_code}",
        content,
        flags=re.MULTILINE
    )
    content = re.sub(
        r"^app\.version\.name\s*=\s*.+",
        f"app.version.name={new_version_name}",
        content,
        flags=re.MULTILINE
    )
    
    with open(properties_path, "w") as f:
        f.write(content)
        
    print(f"Updated properties file to: Name={new_version_name}, Code={new_version_code}")
    print(f"Output build version: {output_version_name}")
    
    # Export outputs to GITHUB_OUTPUT
    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a") as f:
            f.write(f"new_version_name={output_version_name}\n")
            f.write(f"new_version_code={new_version_code}\n")
            f.write(f"should_commit={'true' if should_commit else 'false'}\n")

if __name__ == "__main__":
    main()
