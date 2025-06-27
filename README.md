
# FREELANCING PLATFORM
This project is a Java-based freelancing platform that allows hirers to post jobs and freelancers to apply. The application uses PostgreSQL for data storage and Java Swing for the GUI, creating an interactive and user-friendly experience. 

## Table of Contents
- [Installation](#installation)
- [Usage](#usage)
- [Features](#features)
- [Modules and Algorithms](#modules-and-algorithms)
- [Contribution Guidelines](#contribution-guidelines)
- [Authors and Acknowledgments](#authors-and-acknowledgments)
- [Contact Information](#contact-information)

## Installation
### Requirements
- **Java JDK**: 17 or above
- **PostgreSQL**: Ensure PostgreSQL is installed and configured.
- **Java Swing**: Used for the user interface.
- **JDBC Driver for PostgreSQL**: Make sure the PostgreSQL JDBC driver is included in the classpath. 

### Step-by-Step Installation
1. Clone the repository: 
2. Navigate to the project directory: 
3. Compile the project.
4. Run the executable: 

## Usage
### Running the Application
- Execute the code using: javac -cp ".;postgresql-42.7.4.jar" and java -cp ".;postgresql-42.7.4.jar" SignupPage.
 

## Features
- **Posting a Job:** Hirers can post jobs with details such as job title, description, skills required, and timeline.
- **Freelancer Applications:** Freelancers can browse available jobs, submit proposals, and set bid amounts.
- **Job Management:** Hirers can view proposals, accept or reject freelancers, and manage the job status.
- **Proposal Tracking:** Accepted proposals are marked, and rejected freelancers are restricted from reapplying.
- **Filtering Proposals:** Propsals that are posted for a job are displayed in a sorted order based on skills ,projects done,bid amount and rating.
- **Payment Method:** Escrow  Payment method is used here where the money from the client is recieved completly and on each 25 percent completion a review is asked from client and money is sent according to review. 

##  Modules and Algorithms
### 1. User Module
- **Login:**
  - Handles user registration .
  - Stores user credentials in the users database.
  - Manages user profiles and user id.
- **Sign Up:** 
    - Handles user authentication.
    - Retrives user data from user table.
### 2.List Jobs  Module
- Retrives avaliable jobs from jobs database and show them to the user.

### 3.Filter Jobs Module
- Allows user to filter jobs based on skills required for their job.

### 4.Job Creation Module:
- Allows users to post job with requirements like skills , deadline
and how do they want it to be done.

### 5.Manage Proposals Module:
- Allows the clients who posted jobs to view their jobs and proposals that have come for that job. 
- It enables user to view proposals and view the user profile who has listed the proposal and accept or decline.

### 6.Filter Proposals:
- Proposla listed to the clients are displayed in a sorted order based on skills ,projects done,bid amount and rating so that client can only easily choose from top proposals.

### 7.Review Module: 
- On completion of 25 percent of job the freelancer can notify the client for review and it notified for the client.
- Client can verify their work and submit the review .

### 8.Payment Module
- Escrow  Payment method is used where the money from the client is recieved completly and on each 25 percent completion a review is asked from client and money is sent according to review submitted by the client. 


## Contribution Guidelines
- Fork the repository
- Create a new branch
- Submit a pull request

## Authors
- Developed by Shreyas K, Sarvesh Narayanan, Shiva Ganesh V.

## Contact Information
For questions or support, contact shreyas2310140@ssn.edu.in

