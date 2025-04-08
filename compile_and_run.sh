#!/bin/bash

# Make the script exit on error
set -e

echo "LearnIQ Application Build Script"
echo "--------------------------------"

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
check_environment() {
  echo -e "${BLUE}Checking environment...${NC}"
  
  # Check if Java is installed
  if ! command -v java &> /dev/null; then
    echo -e "${RED}Java is not installed or not in PATH.${NC}"
    echo "Please install Java 21 to run this application."
    exit 1
  fi
  
  # Get Java version 
  java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
  echo -e "${GREEN}Found Java version: $java_version${NC}"
  
  # Check if Maven is installed
  if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Maven is not installed or not in PATH.${NC}"
    echo "Please install Maven to build this application."
    exit 1
  fi
  
  # Check if GEMINI_API_KEY is set
  if [[ -z "${GEMINI_API_KEY}" ]]; then
    echo -e "${RED}GEMINI_API_KEY environment variable is not set.${NC}"
    echo "Please set your Gemini API key by running:"
    echo "  export GEMINI_API_KEY=your_api_key_here"
    exit 1
  else
    echo -e "${GREEN}GEMINI_API_KEY is set.${NC}"
  fi
  
  echo -e "${GREEN}Environment check passed.${NC}"
}

compile() {
  echo -e "${BLUE}Compiling the LearnIQ application...${NC}"
  
  # Clean and package with Maven
  mvn clean package assembly:single
  
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}Compilation successful.${NC}"
  else
    echo -e "${RED}Compilation failed.${NC}"
    exit 1
  fi
}

run() {
  echo -e "${BLUE}Running the LearnIQ application...${NC}"
  
  # Get the JAR file name from Maven target directory
  jar_file=$(find target -name "*-jar-with-dependencies.jar" | head -1)
  
  if [ -f "$jar_file" ]; then
    echo -e "${GREEN}Starting application from $jar_file${NC}"
    java -jar "$jar_file"
  else
    echo -e "${RED}Could not find the compiled JAR file.${NC}"
    echo "Please make sure the application has been compiled successfully."
    exit 1
  fi
}

print_usage() {
  echo -e "Usage: $0 [options]"
  echo -e ""
  echo -e "Options:"
  echo -e "  --compile     Compile the application"
  echo -e "  --run         Run the application (compiles first if JAR not found)"
  echo -e "  --help        Show this help message"
  echo -e ""
  echo -e "Example: $0 --run"
}

# Main script execution

# If no arguments provided, print usage
if [ $# -eq 0 ]; then
  print_usage
  exit 0
fi

# Parse command line arguments
while [ $# -gt 0 ]; do
  case "$1" in
    --compile)
      check_environment
      compile
      ;;
    --run)
      check_environment
      
      # Check if JAR exists, if not compile first
      if [ ! -f "$(find target -name "*-jar-with-dependencies.jar" 2>/dev/null | head -1)" ]; then
        compile
      fi
      
      run
      ;;
    --help)
      print_usage
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      print_usage
      exit 1
      ;;
  esac
  shift
done

echo -e "${GREEN}Script completed.${NC}"
exit 0