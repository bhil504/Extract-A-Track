# Extract-A-Track
Extract-A-Track BETA
Extract-A-Track is an audio processing and management application designed for musicians, producers, and audio enthusiasts. This application allows users to upload audio tracks, analyze and extract specific audio stems using Spleeter, and perform in-depth audio analysis with Librosa. Files are securely stored in an Amazon S3 bucket for easy access and management.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
- [Installation](#installation)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Technologies Used](#technologies-used)
- [Contributing](#contributing)
- [License](#license)

## Features
Track Upload and Management: Users can upload audio files (WAV format) and manage their tracks.
Stem Separation: Using Spleeter, users can extract stems from tracks (2 or 5 stems).
Audio Analysis: Integrates with Librosa for in-depth analysis, including features like tempo and pitch extraction.
Amazon S3 Integration: Secure storage of audio files in Amazon S3 with a structured folder system.
User Authentication: Secure login and registration process to access the application.
Progress Tracking: Real-time progress indicators for track conversions and stem extraction.

## Getting Started
Prerequisites
Java 11 or higher
Docker (optional for containerized deployment)
Git
An Amazon S3 bucket with permissions for file storage and management
Python 3.10+ (for Spleeter and Librosa functionality)

## Installation
Clone the repository:

git clone https://github.com/bhil504/Extract-A-Track.git
cd Extract-A-Track

Set up Environment Variables: Configure environment variables for S3 credentials and paths required by Spleeter and Librosa.

Install Dependencies:

For the backend (Java Spring Boot dependencies), check pom.xml.
For Python dependencies (Librosa and Spleeter), use:
bash
Copy code
pip install -r requirements.txt
Database Configuration:

This app uses MySQL for storing user and track metadata. Update database credentials in application.properties and set up the database.
Run the Application:


mvn spring-boot:run
For a Dockerized deployment, build and start the containers using:

docker-compose up --build
## Usage
Upload a Track: After logging in, navigate to the upload page to upload a WAV file.
Extract Stems: Select the number of stems (2 or 5) and process the track with Spleeter. Downloadable stems will appear once processed.
Audio Analysis: Use the "Analyze with Librosa" feature to extract audio characteristics, including tempo and pitch.
Track Management: View, download, and delete your uploaded tracks and stems.

## Project Structure
/src/main/java: Java source code.
/src/main/resources: Application configuration files.
/src/main/webapp/WEB-INF/views: JSP views for the front end.
/librosa: Contains Python scripts and requirements for Librosa audio analysis.
/spleeter: Contains scripts for Spleeter processing.

## Technologies Used
Java Spring Boot: Backend framework.
MySQL: Database for storing user data and metadata.
AWS S3: Cloud storage for audio files.
Spleeter: Audio stem separation.
Librosa: Audio feature extraction.
Docker: Containerized deployment.
Git LFS: Manages large files in the repository.

## Contributing
Contributions are welcome! Feel free to submit a pull request or report an issue.

## License
This project is licensed under the MIT License - see the LICENSE file for details.

