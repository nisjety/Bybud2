This guide provides a concise overview of how to set up and run the ByBud microservices architecture, and some insight into the core system components, communication patterns, and best practices.
 
Getting Started
1.	To build all backend services, open a terminal in the project's root directory and run:
•	mvn clean install
2.	Once the build is complete, start the Kafka, Redis, and MongoDB containers:
•	docker compose down -v && docker compose up -d
•	It may take a moment to initialize all requirements.
3.	Next, open separate terminals to run each microservice. For the Auth Gateway (port 8080):
•	cd backend/services/auth-gateway
•	mvn spring-boot:run
4.	For the User Service (port 8083):
•	cd backend/services/user-service
•	mvn spring-boot:run
5.	For the Delivery Service (port 8082):
•	cd backend/services/delivery-service
•	mvn spring-boot:run
6.	And for the Eureka Server (port 8761):
•	cd backend/services/eureka-server
•	mvn spring-boot:run
7.	To run the frontend, navigate to the frontend folder:
•	cd frontend
•	npm install
•	npm run dev
It may take a few minutes before all services are fully operational. (up to 3 min)
![image](https://github.com/user-attachments/assets/91a5b805-8ffd-4821-b2d8-16fb91444e9a)
