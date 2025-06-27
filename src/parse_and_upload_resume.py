import sys
import pdfplumber
import psycopg2
import os
import re

# Function to extract text from the PDF file
def extract_resume_data(file_path):
    try:
        resume_text = ""
        with pdfplumber.open(file_path) as pdf:
            for page in pdf.pages:
                page_text = page.extract_text()
                if page_text:  # Ensure we only add non-empty text
                    resume_text += page_text
        if resume_text.strip():  # Ensure the extracted text is not empty
            return resume_text
        else:
            raise ValueError("No text extracted from the PDF.")
    except Exception as e:
        print(f"Error extracting data from PDF: {e}")
        return None

# Function to parse the extracted resume data
def parse_resume_data(resume_text):
    skills = "Not found"
    experience = "Not found"
    education = "Not found"

    # Match "Skills" section and grab everything until a keyword indicating the next section
    skills_match = re.search(r"(skills?|skill\s*set?):?\s*((?:[^\n]+\n?)+)", resume_text, re.IGNORECASE)
    if skills_match:
        skills = skills_match.group(2).strip()

    # Match "Experience" section and grab everything until the next section starts
    experience_match = re.search(r"(experience|work\s*history):?\s*((?:[^\n]+\n?)+)", resume_text, re.IGNORECASE)
    if experience_match:
        experience = experience_match.group(2).strip()

    # Match "Education" section
    education_match = re.search(r"(education|qualifications?):?\s*((?:[^\n]+\n?)+)", resume_text, re.IGNORECASE)
    if education_match:
        education = education_match.group(2).strip()

    # Print parsed data to display before uploading to database
    print("Parsed Resume Data:")
    print(f"Skills: {skills}")
    print(f"Experience: {experience}")
    print(f"Education: {education}")
    
    return {
        'skills': skills,
        'experience': experience,
        'education': education
    }

# Function to upload or update the parsed resume data to the database
def upload_resume_to_db(user_id, resume_data):
    try:
        # Connect to the PostgreSQL database using the correct connection details
        connection = psycopg2.connect(
            dbname="mydb",  # Database name from your DatabaseConnection class
            user="postgres",  # PostgreSQL username
            password="mydatabase",  # PostgreSQL password
            host="localhost"  # Database host
        )
        cursor = connection.cursor()
        
        # SQL query to insert or update data in the resume table
        query = """
            INSERT INTO resume (user_id, skills, experience, education)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (user_id) 
            DO UPDATE SET 
                skills = EXCLUDED.skills,
                experience = EXCLUDED.experience,
                education = EXCLUDED.education;
        """
        
        # Execute the query with the parsed resume data
        cursor.execute(query, (user_id, resume_data['skills'], 
                               resume_data['experience'], resume_data['education']))
        connection.commit()
        
        # Log the insertion or update
        print(f"Resume content uploaded or updated for user_id: {user_id}")
    except Exception as e:
        print(f"Error uploading resume to database: {e}")
    finally:
        if connection:
            connection.close()

# Main execution block
if __name__ == "__main__":
    try:
        # Get the file path and user ID from command line arguments
        file_path = sys.argv[1]
        user_id = int(sys.argv[2])
        
        # Check if the provided file exists
        if not os.path.exists(file_path):
            print(f"Error: The file at {file_path} does not exist.")
            sys.exit(1)
        
        # Extract the resume data from the PDF
        resume_text = extract_resume_data(file_path)
        if resume_text:
            # Parse the extracted resume data into specific fields
            resume_data = parse_resume_data(resume_text)
            
            # Upload the parsed resume data to the database
            upload_resume_to_db(user_id, resume_data)
        else:
            print("Error: No text extracted from the resume.")
    except IndexError:
        print("Error: Please provide both the file path and user ID.")
    except ValueError as ve:
        print(f"Error: {ve}")
