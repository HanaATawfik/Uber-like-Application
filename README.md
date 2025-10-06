# Uber-like Application

A Java-based ride-sharing application featuring a bidding system where customers request rides by setting their preferred fare, and drivers can place competitive bids.

## Overview

This application uses a client-server architecture to create a ride-sharing platform where:
- Customers can request rides and set their desired fare
- Available drivers can view ride requests and offer their own fare
- Customers can select from driver bids
- Both parties can track ride status through completion

## Features

### Customer Features
- Account registration and login
- Create ride requests with pickup/dropoff locations and suggested fare
- View incoming bids from drivers
- Accept preferred driver bid
- Track current ride status

### Driver Features
- Account registration and login
- View available ride requests
- Make fare offers for rides
- Track assigned rides
- Update ride status (started, completed)

## System Architecture

The application uses:
- **Socket-based communication**: Server handles multiple clients concurrently
- **Multi-threaded design**: Each client connection runs in a separate thread
- **In-memory data storage**: User accounts and rides managed through ConcurrentHashMaps

## Installation

### Prerequisites
- Java JDK 8 or higher
- Maven

### Building the Project
1. Clone the repository:
```
git clone https://github.com/HanaATawfik/Uber-like-Application.git
```

2. Navigate to the project directory:
```
cd Uber-like-Application
```

3. Build with Maven:
```
mvn clean install
```

## Running the Application

1. Start the server:
```
java GreetingServer <port_number>
```

2. Start client application(s):
```
java GreetingClient <server_host> <server_port>
```

## How the Bidding System Works

1. **Ride Request**: Customer creates a request including pickup/dropoff locations and suggested fare
2. **Driver Bidding**: Available drivers view pending requests and can offer their own fare
3. **Bid Selection**: Customer views all bids and selects their preferred driver
4. **Match Confirmation**: When a bid is accepted, the ride status changes to "matched"
5. **Ride Progress**: Driver updates ride status to "started" when picking up customer
6. **Completion**: Driver marks ride as "completed" when reaching the destination

## Main Components

### Server-Side Classes
- **GreetingServer**: Main server class handling client connections
- **Customer**: Stores customer account information
- **Driver**: Manages driver data including availability status
- **Ride**: Tracks ride details, status, and associated bids
- **Bid**: Stores information about driver offers

### Client-Side
- **GreetingClient**: Handles user interface and communication with the server

## Data Flow

1. User registers/logs in through client application
2. Requests and responses are exchanged through socket connections
3. Server maintains user session data and processes requests
4. All changes to ride status are broadcast to relevant parties

## Project Status

This project is a functional demonstration of a ride-sharing application with bidding capabilities. Future enhancements may include:
- Persistent data storage with a database
- Map integration for route planning
- Rating system for drivers and passengers
- Payment processing integration
- Mobile application interface

## License
