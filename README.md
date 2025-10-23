# California State University Dominguez Hills Course Scheduler
This project is the backend implementation of a full stack application that assists students with fullfilling requirements for 
a bachelor degree from Cal State Dominguez Hills. 

# What technolgies were used

The backend was implemented in Kotlin using the Ktor framework along with the exposed library. To persist data a postgres databsae was used. 

# How the application was deployed. 
For deployment, AWS ec2 instances were used to host the backend and front end of the application. 
Both parts of the application used Amazon linux 2023 as the image for the ec2 instance. To deploy the backend the application was packeged as a fat jar using the gradle wrapper and the jar was copied over using the scp command in git bash. 
After enviorement variables were set in the ec2 instance the application was successfully deployed. For the frontend an Amazon linux 2023 image was also used. After building the Angular application NGINX was used to serve output. 

# Degree Mapping
The main feature that the backend implements is the ability to store the structure of any degree the university offers into a format that the front end can use. All degrees that the university offers is essentially a graph. Each object
in the degree becomes a node and any relationship between the objects become edges. This structure is what the front end requests whenever a student creates a plan for a specific degree.  

