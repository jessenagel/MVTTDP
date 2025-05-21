# Project Overview

This is the companion code for the paper 


## Project Structure

- `App/src/main/java/nl/jessenagel/mvttdp/app/Main.java`: Entry point for the application.
- `parameters.json`: Configuration file for simulation parameters.
- `data/Model input/`: Directory for input files such as distance matrices.
- `README.md`: Documentation for the project.

## Requirements

- **Languages**: Java
- **Build Tool**: Maven
- **Dependencies**: Ensure all required libraries are included in the Maven `pom.xml`.

## Usage

1. **Setup**: Clone the repository and ensure all dependencies are installed.
2. **Configuration**: Modify `parameters.json` to set up the simulation parameters. 
3. **Execution**: Run the project using Maven or your preferred IDE. Usage: java main <seed(integer)> <parameterfile> <index> <gap> <scratchDir>
4. **Results**: View the output, including optimized schedules and transportation costs.

## Configuration

The `parameters.json` file allows you to configure the following:

- **Algorithm**: Choose the optimization method (e.g., `FullEnumeration`, `GRASP`).
- **Transportation Settings**: Define travel times, capacities, and penalties.
- **Simulation Settings**: Set start and end times, arrival processes, and other parameters.

## Key Classes

- **Scenario**: Models a transportation scenario with all relevant parameters.
- **GRASP**: Implements the GRASP algorithm for solving scheduling problems.
- **Triplet**: Represents a candidate solution element in the GRASP algorithm.

## Input Files

- **Distance Matrix**: JSON file containing travel times between locations.
- **Orderbook**: CSV file with order data for generating daily schedules.

## Output

The system produces:

- Optimized transportation schedules.
- Cost and CO2 emission summaries.
- Feasibility checks for proposed solutions.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.